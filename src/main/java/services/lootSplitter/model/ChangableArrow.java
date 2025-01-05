package services.lootSplitter.model;

import utils.Emojis;

public abstract class ChangableArrow {
    protected String getComparingEmoji(double value) {
        if(value < 0) return Emojis.getRedDownArrow();
        if(value == 0) return "";
        return Emojis.getGreenUpArrow();
    }

    protected String getComparingEmojiReversed(double value) {
        if(value < 0) return Emojis.getGreenDownArrow();
        if(value == 0) return "";
        return ":small_red_triangle:";
    }
}
