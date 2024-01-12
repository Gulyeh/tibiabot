import builders.Commands.CommandsBuilder;
import discord.Connector;
import events.*;


public class Main {

    public static void main(String[] args) {
        initializeBot();

        while (true) {

        }
    }

    private static void initializeBot() {
        Connector.connect();
        Connector.addListener(new TibiaCoinsEvent());
//        Connector.addListener(new WorldsEvent());
//        Connector.addListener(new KillStatisticsEvent());
//        Connector.addListener(new HousesEvent());
//        Connector.addListener(new EventsCalendarEvent());

        CommandsBuilder.builder()
                .addBoss()
                .setEventsChannel()
                .setHousesChannel()
                .setKillingStatsChannel()
                .setServerStatusChannel()
                .setTibiaCoinsPricesChannel()
                .setWorld()
                .build();
    }
}