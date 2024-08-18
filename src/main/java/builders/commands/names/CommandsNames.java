package builders.commands.names;

import java.util.Arrays;
import java.util.List;

public final class CommandsNames {
    public final static String worldCommand = "trackworld";
    public final static String serverStatusCommand = "setserverstatus";
    public final static String tibiaCoinsCommand = "setcoinsprices";
    public final static String killingStatsCommand = "setkillingstats";
    public final static String houseCommand = "sethouses";
    public final static String eventsCommand = "setevents";
    public final static String miniWorldChangesCommand = "setminiworldchanges";
    public final static String boostedsCommand = "setboosteds";

    public static List<String> getCommands() {
        return Arrays.asList(worldCommand, serverStatusCommand, tibiaCoinsCommand, killingStatsCommand, houseCommand, eventsCommand, miniWorldChangesCommand,
                boostedsCommand);
    }
}
