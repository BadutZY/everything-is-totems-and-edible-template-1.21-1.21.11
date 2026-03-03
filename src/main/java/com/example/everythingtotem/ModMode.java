package com.example.everythingtotem;

public enum ModMode {
    DISABLED("Disabled", "§7Everything MOD: §cDisabled"),
    EDIBLE("Edible Mode", "§7Everything MOD: §aEdible Mode"),
    TOTEM("Totem Mode", "§7Everything MOD: §6Totem Mode");

    private final String name;
    private final String displayMessage;

    ModMode(String name, String displayMessage) {
        this.name = name;
        this.displayMessage = displayMessage;
    }

    public String getName() {
        return name;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public ModMode next() {
        return switch (this) {
            case DISABLED -> EDIBLE;
            case EDIBLE -> TOTEM;
            case TOTEM -> DISABLED;
        };
    }
}