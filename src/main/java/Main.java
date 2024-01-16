import builders.Commands.CommandsBuilder;
import discord.Connector;
import events.*;

import static discord.Connector.client;


public class Main {

    public static void main(String[] args) {
        initializeBot();
        client.onDisconnect().block();
    }

    private static void initializeBot() {
        Connector.connect();
        Connector.addListener(new TibiaCoinsEvent());
        Connector.addListener(new ServerStatusEvent());
        Connector.addListener(new TrackWorldEvent());
        Connector.addListener(new KillStatisticsEvent());
        Connector.addListener(new HousesEvent());
//        Connector.addListener(new EventsCalendarEvent());

        CommandsBuilder.builder()
                .addBoss()
                .setEventsChannel()
                .setHousesChannel()
                .setKillingStatsChannel()
                .setServerStatusChannel()
                .setTibiaCoinsPricesChannel()
                .setWorld()
                .clearUnusedCommands()
                .build();
    }
}