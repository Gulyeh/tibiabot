package builders.commands;

public final class CommandsBuilder {
    private CommandsBuilder() {}

    public static Commands builder() {
        return new Commands();
    }
}
