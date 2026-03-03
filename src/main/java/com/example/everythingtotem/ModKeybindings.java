package com.example.everythingtotem;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {
    private static KeyBinding toggleKeybind;

    public static void register() {
        toggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.everything-totem.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.everything-totem"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKeybind.wasPressed()) {
                ModConfig.toggleMode();

                if (client.player != null) {
                    // Tampilkan pesan di atas hotbar (actionbar)
                    client.player.sendMessage(
                            Text.literal(ModConfig.getCurrentMode().getDisplayMessage()),
                            true // true = tampilkan di actionbar (atas hotbar)
                    );
                }
            }
        });
    }
}