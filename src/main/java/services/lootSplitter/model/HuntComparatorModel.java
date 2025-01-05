package services.lootSplitter.model;

import lombok.Getter;
import lombok.Setter;
import utils.Emojis;

import java.util.List;

@Getter
@Setter
public class HuntComparatorModel extends ChangableArrow {
    private String lootPerHour;
    private String balancePerHour;
    private String suppliesPerHour;
    private String individualBalancePerHour;

    private String avgLootPerHour;
    private String avgBalancePerHour;
    private String avgSuppliesPerHour;
    private String avgIndividualBalancePerHour;

    private String lootPerHourDifference;
    private String balancePerHourDifference;
    private String suppliesPerHourDifference;
    private String individualBalancePerHourDifference;

    private double lootPerHourDifferencePercentage;
    private double balancePerHourDifferencePercentage;
    private double suppliesPerHourDifferencePercentage;
    private double individualBalancePerHourDifferencePercentage;

    private List<ComparatorMember> comparedMembers;



    public String getLootPerHourDifferencePercentage() {
        return formatPercentage(lootPerHourDifferencePercentage) + " " + getComparingEmoji(lootPerHourDifferencePercentage);
    }

    public String getBalancePerHourDifferencePercentage() {
        return formatPercentage(balancePerHourDifferencePercentage) + " " + getComparingEmoji(balancePerHourDifferencePercentage);
    }

    public String getSuppliesPerHourDifferencePercentage() {
        return formatPercentage(suppliesPerHourDifferencePercentage) + " " + getComparingEmojiReversed(suppliesPerHourDifferencePercentage);
    }

    public String getIndividualBalancePerHourDifferencePercentage() {
        return formatPercentage(individualBalancePerHourDifferencePercentage) + " " + getComparingEmoji(individualBalancePerHourDifferencePercentage);
    }

    private String formatPercentage(double value) {
        return String.format("%+.2f", value) + "%";
    }
}
