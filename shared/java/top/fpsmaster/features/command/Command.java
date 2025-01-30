package top.fpsmaster.features.command;

public abstract class Command {
    String name;
    public Command(String name) {
        this.name = name;
    }

    public abstract void execute(String[] args);
}
