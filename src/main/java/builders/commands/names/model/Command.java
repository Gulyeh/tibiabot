package builders.commands.names.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Command {
    private final String commandName;
    private final String description;
    private final List<CommandOption> options;

    public Command(String commandName, String description, CommandOption... options) {
        this.commandName = commandName;
        this.description = description;
        this.options = new ArrayList<>(List.of(options));
    }
}
