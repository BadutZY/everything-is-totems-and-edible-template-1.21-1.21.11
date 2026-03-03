package com.example.everythingtotem;

public class ModConfig {
    private static ModMode currentMode = ModMode.DISABLED;

    public static ModMode getCurrentMode() {
        return currentMode;
    }

    public static void setCurrentMode(ModMode mode) {
        currentMode = mode;
    }

    public static void toggleMode() {
        currentMode = currentMode.next();
    }

    public static boolean isEdibleMode() {
        return currentMode == ModMode.EDIBLE;
    }

    public static boolean isTotemMode() {
        return currentMode == ModMode.TOTEM;
    }
}