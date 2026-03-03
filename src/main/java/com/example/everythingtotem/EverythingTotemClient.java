package com.example.everythingtotem;

import com.example.everythingtotem.network.TotemActivatedPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class EverythingTotemClient implements ClientModInitializer {

    /**
     * Item yang akan ditampilkan di animasi totem pop.
     * Di-set saat menerima packet dari server, di-clear setelah digunakan.
     *
     * GameRendererMixin akan membaca nilai ini setiap kali showFloatingItem() dipanggil.
     */
    private static ItemStack totemOverrideStack = null;

    /**
     * Getter untuk GameRendererMixin — dipanggil dari mixin saat showFloatingItem dijalankan.
     */
    public static ItemStack getTotemOverrideStack() {
        ItemStack stack = totemOverrideStack;
        // Clear setelah diambil agar tidak menimpa animasi totem selanjutnya yang tidak terkait mod
        totemOverrideStack = null;
        return stack;
    }

    @Override
    public void onInitializeClient() {
        ModKeybindings.register();

        // Terima packet dari server saat totem berhasil diaktifkan
        ClientPlayNetworking.registerGlobalReceiver(
                TotemActivatedPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        ItemStack received = payload.totemStack();

                        // Simpan item yang digunakan sebagai override untuk animasi
                        totemOverrideStack = received;

                        // Panggil showFloatingItem() — vanilla akan menjalankan animasi totem penuh:
                        // rotasi item, flash putih, dll.
                        // GameRendererMixin akan intercept dan ganti itemnya dengan totemOverrideStack.
                        MinecraftClient client = context.client();
                        if (client.gameRenderer != null) {
                            // Kita kirim dummy Totem of Undying stack agar vanilla
                            // mau memulai animasi. GameRendererMixin akan segera ganti
                            // item ini dengan totemOverrideStack yang asli.
                            client.gameRenderer.showFloatingItem(
                                    new ItemStack(Items.TOTEM_OF_UNDYING)
                            );
                        }
                    });
                }
        );

        EverythingTotem.LOGGER.info("Everything Totem client initialized!");
    }
}