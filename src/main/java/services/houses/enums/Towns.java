package services.houses.enums;

import lombok.Getter;

@Getter
public enum Towns {
    AB_DENDRIEL("Ab'Dendriel"),
    ANKRAHMUN("Ankrahmun"),
    CARLIN("Carlin"),
    DARASHIA("Darashia"),
    EDRON("Edron"),
    FARMINE("Farmine"),
    GRAY_BEACH("Gray Beach"),
    ISSAVI("Issavi"),
    KAZORDOON("Kazordoon"),
    LIBERTY_BAY("Liberty Bay"),
    MOONFALL("Moonfall"),
    PORT_HOPE("Port Hope"),
    RATHLETON("Rathleton"),
    SILVERTIDES("Silvertides"),
    SVARGROND("Svargrond"),
    THAIS("Thais"),
    VENORE("Venore"),
    YALAHAR("Yalahar");

    Towns(String name) {
        townName = name;
    }

    private final String townName;
}
