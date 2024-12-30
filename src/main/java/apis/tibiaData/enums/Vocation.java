package apis.tibiaData.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum Vocation {
    RP("Royal Paladin", ":bow_and_arrow:"),
    MS("Master Sorcerer", ":fire:"),
    ED("Elder Druid", ":snowflake:"),
    EK("Elite Knight", ":shield:"),
    K("Knight", ":shield:"),
    P("Paladin", ":bow_and_arrow:"),
    D("Druid", ":snowflake:"),
    S("Sorcerer", ":fire:"),
    NONE("None", ":no_entry_sign:");

    private final String name;
    private final String icon;

    public static Vocation getEnum(String value) {
        Optional<Vocation> voc = Arrays.stream(Vocation.values()).filter(x -> x.getName().equals(value)).findFirst();
        return voc.orElse(null);
    }
}
