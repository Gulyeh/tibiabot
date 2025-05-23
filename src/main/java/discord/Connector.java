package discord;

import discord.enums.Statuses;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.shard.ShardingStrategy;
import discord4j.discordjson.Id;
import discord4j.gateway.intent.IntentSet;
import events.interfaces.Listener;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import utils.Configurator;

import java.util.concurrent.TimeUnit;

@Slf4j
public final class Connector {

    public static GatewayDiscordClient client;
    private final static String status = "Hello giga mates";
    private final static String key = Configurator.config.get(Configurator.ConfigPaths.BOT_KEY.getName());

    @SneakyThrows
    public static void connect() {
        try {
            if(client != null) return;

            client = DiscordClient.create(key)
                    .gateway()
                    .setInitialPresence(x -> ClientPresence.online(ClientActivity.playing(status)))
                    .setAwaitConnections(true)
                    .setSharding(ShardingStrategy.recommended())
                    .setEnabledIntents(IntentSet.all())
                    .login()
                    .doOnError(e -> log.error("Failed to authenticate with Discord", e))
                    .doOnSuccess(result -> log.info("Connected to Discord"))
                    .block();

            assert client != null;
        } catch (Exception e) {
            log.info("Error while connecting to discord. Retry in 1 minute. - {}", e.getMessage());
            TimeUnit.MINUTES.sleep(1);
            connect();
        }
    }

    public static void addListener(Listener event) {
        log.info("Listening to: {}", event.getEventName());
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
