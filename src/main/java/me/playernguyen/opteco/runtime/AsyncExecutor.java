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

public final class AsyncExecutor implements AutoCloseable {

    private final Plugin plugin;
    private final ExecutorService executor;
    private final AtomicBoolean closed = new AtomicBoolean();

    public AsyncExecutor(Plugin plugin, int threads) {
        this.plugin = plugin;
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "OptEco-DB-" + runnable.hashCode());
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newFixedThreadPool(Math.max(1, threads), factory);
    }

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

    private Throwable unwrap(Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }

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
