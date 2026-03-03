package com.example.everythingtotem;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ModKeybindings {
    private static KeyBinding toggleKeybind;

    public static void register() {
        // ✅ FIX: Di 1.21.9, KeyBinding WAJIB menerima KeyBinding.Category object,
        //         bukan raw String. Buat category dengan KeyBinding.Category.create().
        KeyBinding.Category TOGGLE_CATEGORY = KeyBinding.Category.create(
                Identifier.of("everything-totem", "main")
        );

        toggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.everything-totem.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                TOGGLE_CATEGORY   // ← pakai Category object, bukan String
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // ✅ Gunakan while() agar tidak ada input yang terlewat
            while (toggleKeybind.wasPressed()) {
                ModConfig.toggleMode();

                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal(ModConfig.getCurrentMode().getDisplayMessage()),
                            true // tampil di actionbar atas hotbar
                    );
                }
            }
        });
    }
}