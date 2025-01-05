package services.lootSplitter.model;

import lombok.Getter;
import lombok.Setter;
import utils.Emojis;

@Getter
@Setter
public class ComparatorMember extends ChangableArrow {
    private String name;

    private String damagePerHour;
    private String healingPerHour;
    private String suppliesPerHour;
    private String lootPerHour;

    private String avgDamagePerHour;
    private String avgHealingPerHour;
    private String avgSuppliesPerHour;
    private String avgLootPerHour;

    private String damageDifference;
    private String healingDifference;
    private String lootDifference;
    private String suppliesDifference;

    private double damageDifferencePercentage;
    private double healingDifferencePercentage;
    private double lootDifferencePercentage;
    private double suppliesDifferencePercentage;

    public String getDamageDifferencePercentage() {
        return formatPercentage(damageDifferencePercentage) + " " + getComparingEmoji(damageDifferencePercentage);
    }

    public String getHealingDifferencePercentage() {
        return formatPercentage(healingDifferencePercentage) + " " + getComparingEmoji(healingDifferencePercentage);
    }

    public String getLootDifferencePercentage() {
        return formatPercentage(lootDifferencePercentage) + " " + getComparingEmoji(lootDifferencePercentage);
    }

    public String getSuppliesDifferencePercentage() {
        return formatPercentage(suppliesDifferencePercentage) + " " + getComparingEmojiReversed(suppliesDifferencePercentage);
    }

    private String formatPercentage(double value) {
        return String.format("%+.2f", value) + "%";
    }
}
