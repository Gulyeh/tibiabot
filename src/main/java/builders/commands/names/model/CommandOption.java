package builders.commands.names.model;

import discord4j.core.object.command.ApplicationCommandOption;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandOption {
    private final String optionName;
    private final String optionDescription;
    private final ApplicationCommandOption.Type type;
    private final List<CommandChoice> choices;

    public CommandOption(String optionName, String optionDescription, ApplicationCommandOption.Type type, CommandChoice... choices) {
        this.optionName = optionName;
        this.optionDescription = optionDescription;
        this.type = type;
        this.choices = new ArrayList<>(List.of(choices));
    }
}
