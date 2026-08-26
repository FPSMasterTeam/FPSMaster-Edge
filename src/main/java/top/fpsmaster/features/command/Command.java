package top.fpsmaster.features.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A chat command. {@code name} is the primary token, {@code aliases} are extra tokens that resolve
 * to the same command, and {@code usage} is the argument shape shown by {@code .help}.
 *
 * <p>The description is not stored here: it is looked up as {@code command.<name>.desc} so it
 * follows the active language instead of freezing whatever was passed at construction.
 */
public abstract class Command {
    public final String name;
    public final String usage;
    public final List<String> aliases;

    public Command(String name) {
        this(name, name, Collections.<String>emptyList());
    }

    public Command(String name, String usage, String... aliases) {
        this(name, usage, Arrays.asList(aliases));
    }

    public Command(String name, String usage, List<String> aliases) {
        this.name = name;
        this.usage = usage;
        this.aliases = Collections.unmodifiableList(aliases);
    }

    /** True if {@code token} is this command's name or one of its aliases, ignoring case. */
    public boolean matches(String token) {
        if (name.equalsIgnoreCase(token)) {
            return true;
        }
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    public abstract void execute(String[] args) throws Exception;
}
