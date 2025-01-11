package cache.guilds;

import abstracts.Singleton;
import cache.interfaces.Cachable;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import lombok.extern.slf4j.Slf4j;
import mongo.GuildDocumentActions;
import mongo.MongoConnector;
import mongo.models.GuildModel;

import java.util.List;

import static discord.Connector.client;
import static mongo.GuildDocumentActions.deleteDocument;
import static mongo.GuildDocumentActions.getDocument;

@Slf4j
public final class GuildCaching extends Singleton implements Cachable {
    private final GuildCacheInitializer initializer;

    public GuildCaching() {
        initializer = new GuildCacheInitializer();
    }

    public static GuildCaching getInstance() {
        return getInstance(GuildCaching.class);
    }

    @Override
    public void refreshCache() {
        new Thread(() -> {
            MongoConnector.connect();
            while (true) {
                try {
                    cacheGuildData(initializer);
                } catch (Exception e) {
                    log.error("Error while caching guild data: ", e);
                }

                log.info("Waiting for the next guild cache refresh...");
                sleepUntilNextRefresh();
            }
        }).start();
    }

    private void cacheGuildData(GuildCacheInitializer initializer) {
        List<Guild> guilds = fetchGuilds();
        if (guilds == null) return;

        log.info("Bot is in {} server(s)", guilds.size());
        List<GuildModel> models = fetchGuildModels();
        GuildCacheData.resetCache();

        for (GuildModel model : models) {
            initializer.removeUnusedChannels(model);

            if (isGuildPresentInBot(guilds, model))
                addToGuildsCache(initializer, model);
            else removeGuildDocument(model);
        }
    }

    private List<Guild> fetchGuilds() {
        try {
            return client.getGuilds().collectList().block();
        } catch (Exception e) {
            log.error("Failed to fetch guilds", e);
            return List.of();
        }
    }

    private List<GuildModel> fetchGuildModels() {
        try {
            return GuildDocumentActions.getDocuments(GuildModel.class);
        } catch (Exception e) {
            log.error("Failed to fetch guild models", e);
            return List.of();
        }
    }

    private boolean isGuildPresentInBot(List<Guild> guilds, GuildModel model) {
        return guilds.stream()
                .anyMatch(guild -> guild.getId().asString().equals(model.getGuildId()));
    }

    private void addToGuildsCache(GuildCacheInitializer initializer, GuildModel model) {
        initializer.addToWorldCache(model);
        initializer.addToChannelsCache(model);
        initializer.addToMinimumDeathsLevelCache(model);
    }

    private void removeGuildDocument(GuildModel model) {
        deleteDocument(getDocument(Snowflake.of(model.getGuildId())));
    }

    private void sleepUntilNextRefresh() {
        try {
            int CACHE_REFRESH_INTERVAL_MS = 30 * 60 * 1000;
            Thread.sleep(CACHE_REFRESH_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
