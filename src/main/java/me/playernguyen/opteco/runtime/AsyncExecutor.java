package me.playernguyen.opteco.runtime;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Small fixed thread pool for blocking database work plus the bridge back to the
 * Bukkit main thread.
 *
 * <p>Threads are daemons named {@code OptEco-DB-*}, so they never keep the JVM
 * alive. Database and event-bus code submits work here; callbacks that touch
 * players, titles, messages or Bukkit events are handed to
 * {@link #runOnMain(Runnable)}. The pool is created once during enable and shut
 * down in {@link #close()}; after that, {@link #supply(Callable)} returns an
 * already failed future and {@link #executeAsync(Runnable)} silently drops the
 * task. The class is thread-safe.
 */
public final class AsyncExecutor implements AutoCloseable {

    /** Plugin used for scheduler access, logging and enablement checks. */
    private final Plugin plugin;

    /** Fixed pool that runs the blocking work. */
    private final ExecutorService executor;

    /** Set once during shutdown so late submissions are ignored. */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Creates the pool.
     *
     * @param plugin the owning plugin, used for the main-thread scheduler and logging
     * @param threads the desired pool size; values below one are raised to one
     */
    public AsyncExecutor(Plugin plugin, int threads) {
        this.plugin = plugin;
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "OptEco-DB-" + runnable.hashCode());
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newFixedThreadPool(Math.max(1, threads), factory);
    }

    /**
     * Submits work to the pool and returns a future for its result.
     *
     * <p>Checked exceptions from {@code work} are wrapped in a
     * {@link CompletionException}, the usual {@link CompletableFuture}
     * convention.
     *
     * @param <T> the result type
     * @param work the blocking work to run off the main thread
     * @return a future completed on a pool thread, or already failed when the
     *         executor is shut down
     */
    public <T> CompletableFuture<T> supply(Callable<T> work) {
        if (closed.get()) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("OptEco is shutting down"));
            return future;
        }
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return work.call();
                    } catch (Exception exception) {
                        throw new CompletionException(exception);
                    }
                },
                executor);
    }

    /**
     * Runs work in the background and dispatches exactly one callback on the main
     * thread: success with the value, or failure with the unwrapped cause.
     *
     * @param <T> the result type
     * @param work the blocking work to run off the main thread
     * @param onSuccess called on the main thread with the result
     * @param onFailure called on the main thread with the cause of the failure
     */
    public <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        supply(work)
                .whenComplete((value, error) -> runOnMain(() -> {
                    if (error != null) {
                        onFailure.accept(unwrap(error));
                    } else {
                        onSuccess.accept(value);
                    }
                }));
    }

    /**
     * Runs a task on the Bukkit main thread, or immediately when already on it.
     *
     * <p>Tasks submitted while the plugin is disabled or disabling are dropped,
     * because the scheduler no longer accepts them.
     *
     * @param runnable the task that needs main-thread access
     */
    public void runOnMain(Runnable runnable) {
        if (closed.get() || !plugin.isEnabled()) {
            return;
        }
        try {
            if (Bukkit.isPrimaryThread()) {
                runnable.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        } catch (IllegalStateException ignored) {
            // The plugin is disabling and the scheduler is no longer accepting work.
        }
    }

    /**
     * Submits a task with no result. An unexpected runtime failure is logged as a
     * warning instead of being lost on the pool thread.
     *
     * @param runnable the task to run off the main thread
     */
    public void executeAsync(Runnable runnable) {
        if (closed.get()) {
            return;
        }
        executor.execute(() -> {
            try {
                runnable.run();
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Async OptEco task failed", exception);
            }
        });
    }

    /**
     * Unwraps the {@link CompletionException} added by
     * {@link CompletableFuture} so callers see the real cause.
     *
     * @param error the error reported by the future
     * @return the cause when one is present, otherwise the supplied error
     */
    private Throwable unwrap(Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }

    /**
     * Stops accepting work and shuts the pool down, waiting up to five seconds
     * for running tasks before forcing termination. Idempotent.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
