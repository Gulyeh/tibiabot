package apis.tibiaData.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum Vocation {
    RP("Royal Paladin", true, ":bow_and_arrow:"),
    MS("Master Sorcerer", true, ":fire:"),
    ED("Elder Druid", true, ":snowflake:"),
    EK("Elite Knight", true, ":shield:"),
    K("Knight", false, ":shield:"),
    P("Paladin", false, ":bow_and_arrow:"),
    D("Druid", false, ":snowflake:"),
    S("Sorcerer", false, ":fire:"),
    NONE("None", false, ":no_entry_sign:");

    private final String name;
    private final boolean promotion;
    private final String icon;

    public static Vocation getEnum(String value) {
        Optional<Vocation> voc = Arrays.stream(Vocation.values()).filter(x -> x.getName().equals(value)).findFirst();
        return voc.orElse(null);
    }
}
