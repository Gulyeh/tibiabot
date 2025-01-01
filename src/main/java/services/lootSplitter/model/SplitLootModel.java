package services.lootSplitter.model;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.List;

@Getter
@Setter
public class SplitLootModel {
    private String spotName;
    private String individualBalance;
    private String balance;
    private String supplies;
    private String loot;
    private String lootPerHour;
    private String huntTime;
    private String huntFrom;
    private String huntTo;
    private String lootType;
    private boolean isNegative;
    private List<SplittingMember> members;

    public boolean validateModel() {
        for(Field field : this.getClass().getDeclaredFields()) {
            try {
                if (field.get(this) == null) return false;
            } catch (IllegalAccessException ignore) {
                return false;
            }
        }

        if(members.size() < 2) return false;

        for(SplittingMember member : members) {
            if(!member.validateMember()) return false;
        }

        return true;
    }
}
