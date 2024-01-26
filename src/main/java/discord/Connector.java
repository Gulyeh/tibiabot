package discord;

import utils.Configurator;
import discord.enums.Statuses;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.shard.ShardingStrategy;
import discord4j.discordjson.Id;
import events.interfaces.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Connector {

    public static GatewayDiscordClient client;
    private final static String status = "Hello giga mates";
    private final static Logger logINFO = LoggerFactory.getLogger(Connector.class);
    private final static String key = Configurator.config.get("BOT_KEY");

    public static void connect() {
        try {
            if(client != null) return;

            client = DiscordClient.create(key)
                    .gateway()
                    .setInitialPresence(x -> ClientPresence.online(ClientActivity.playing(status)))
                    .setAwaitConnections(true)
                    .setSharding(ShardingStrategy.recommended())
                    .login()
                    .doOnError(e -> logINFO.error("Failed to authenticate with Discord", e))
                    .doOnSuccess(result -> logINFO.info("Connected to Discord"))
                    .block();

            assert client != null;
        } catch (Exception e) {
            logINFO.info(e.getMessage());
        }
    }

    public static void addListener(EventListener event) {
        logINFO.info("Listening to: " + event.getEventName());
        event.executeEvent();
    }

    public static void setStatus(Statuses status) {
        switch (status) {
            case ONLINE -> client.updatePresence(ClientPresence.online());
            case OFFLINE -> client.updatePresence(ClientPresence.invisible());
            case DO_NOT_DISTURB -> client.updatePresence(ClientPresence.doNotDisturb());
            case IDLE -> client.updatePresence(ClientPresence.idle());
        }
    }

    public static Id getId() {
        return Id.of(client.getSelfId().asString());
    }
}
