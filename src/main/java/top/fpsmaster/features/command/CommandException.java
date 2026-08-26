package top.fpsmaster.features.command;

/**
 * A command refused to run and already carries a player-readable, localized reason.
 *
 * <p>{@link CommandManager} prints the message as-is. Anything a command cannot do must throw this
 * rather than returning quietly: a command that silently does nothing is indistinguishable from one
 * that worked.
 */
public class CommandException extends Exception {
    public CommandException(String message) {
        super(message);
    }
}
