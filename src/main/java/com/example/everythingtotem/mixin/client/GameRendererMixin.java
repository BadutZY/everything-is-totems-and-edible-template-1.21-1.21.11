package com.example.everythingtotem.mixin.client;

import com.example.everythingtotem.EverythingTotemClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin untuk GameRenderer (CLIENT ONLY).
 *
 * Tujuan: Intercept method showFloatingItem() yang dipanggil vanilla
 * ketika Totem of Undying digunakan. Kita ganti argumen ItemStack-nya
 * dengan item yang benar-benar dipegang pemain saat totem pop.
 *
 * Dengan pendekatan ini, SELURUH animasi vanilla (rotasi, flash, partikel, sound)
 * dipertahankan — kita hanya mengubah item yang ditampilkan.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * ModifyVariable: ubah argumen ItemStack sebelum showFloatingItem() dijalankan.
     *
     * Vanilla selalu memanggil showFloatingItem(totemStack) saat totem digunakan.
     * Kita ganti stack itu dengan item yang sebenarnya dipegang pemain
     * (yang sudah disimpan oleh EverythingTotemClient saat menerima packet dari server).
     */
    @ModifyVariable(
            method = "showFloatingItem(Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private ItemStack replaceTotemPopItem(ItemStack originalStack) {
        // Ambil item override dari EverythingTotemClient
        // Jika null (tidak ada override aktif), kembalikan stack vanilla
        ItemStack override = EverythingTotemClient.getTotemOverrideStack();
        if (override != null && !override.isEmpty()) {
            return override;
        }
        return originalStack;
    }
}