package utils;

public final class Emojis {
    public static String getCoinEmoji() {
        return getEmote(Configurator.Emoji.COIN_EMOJI);
    }

    public static String getBlankEmoji() {
        return getEmote(Configurator.Emoji.BLANK_EMOJI);
    }

    public static String getGreenUpArrow() {
        return getEmote(Configurator.Emoji.GREEN_UP_EMOJI);
    }

    public static String getGreenDownArrow() {
        return getEmote(Configurator.Emoji.GREEN_DOWN_EMOJI);
    }

    public static String getRedDownArrow() {
        return getEmote(Configurator.Emoji.RED_DOWN_EMOJI);
    }

    public static String getGreenBEEmoji() {
        return getEmote(Configurator.Emoji.GREEN_BE);
    }

    public static String getYellowBEEmoji() {
        return getEmote(Configurator.Emoji.YELLOW_BE);
    }

    private static String getEmote(Configurator.Emoji emote) {
        return Configurator.config.get(emote.getId());
    }
}
