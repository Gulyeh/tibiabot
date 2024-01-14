package events.interfaces;

import discord4j.common.util.Snowflake;

public interface Worldable {
    void setWorld(Snowflake guildId, String world);
}
