/**
 * Command handling for {@code /opteco} (aliases {@code /points}, {@code /pts},
 * {@code /p}).
 *
 * <p>{@link me.playernguyen.opteco.command.CommandDispatcher} resolves the
 * subcommand, enforces permissions, parses arguments, and hands work to the
 * economy and payment services. Database work is asynchronous; replies and
 * events return to the main thread.</p>
 */
package me.playernguyen.opteco.command;
