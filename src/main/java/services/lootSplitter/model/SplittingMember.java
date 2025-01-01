package services.lootSplitter.model;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SplittingMember {
    private String name;
    private String loot;
    private String supplies;
    private String balance;
    private String damage;
    private String healing;
    private double damagePercentage;
    private double healingPercentage;
    private double lootPercentage;
    private double suppliesPercentage;
    private List<TransferData> transfers = new ArrayList<>();

    public String getDamagePercentageString() {
        return formatPercentage(damagePercentage);
    }

    public String getHealingPercentageString() {
        return formatPercentage(healingPercentage);
    }

    public String getLootPercentageString() {
        return formatPercentage(lootPercentage);
    }

    public String getSuppliesPercentageString() {
        return formatPercentage(suppliesPercentage);
    }

    public boolean validateMember() {
        for(Field field : this.getClass().getDeclaredFields()) {
            try {
                if (field.get(this) == null) return false;
            } catch (IllegalAccessException ignore) {
                return false;
            }
        }
        return true;
    }

    private String formatPercentage(double value) {
        return String.format("%.2f", value) + "%";
    }
}
