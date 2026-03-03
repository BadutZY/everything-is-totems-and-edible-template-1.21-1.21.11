package com.example.everythingtotem.mixin;

import com.example.everythingtotem.ModConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin untuk ItemStack - backup apply efek makan di level ItemStack.
 *
 * LivingEntityMixin sudah DIHAPUS karena pendekatan mixin untuk intercept kematian
 * sangat error-prone (method name berbeda tiap versi, timing issue, dll).
 *
 * Sebagai gantinya, Totem Mode menggunakan Fabric API event ServerLivingEntityEvents.ALLOW_DEATH
 * yang didaftarkan di EverythingTotem.java - lebih reliable dan tidak perlu mixin.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    /**
     * Inject ke finishUsing di level ItemStack sebagai pelengkap ItemMixin.
     * Hanya apply efek untuk item yang tidak punya FoodComponent asli.
     */
    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void afterItemUsed(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (!ModConfig.isEdibleMode()) {
            return;
        }

        if (!(user instanceof PlayerEntity player) || world.isClient) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;

        // Skip item yang sudah punya FoodComponent asli agar tidak double-apply
        if (self.get(DataComponentTypes.FOOD) != null) {
            return;
        }

        // Apply efek makan
        player.getHungerManager().add(4, 0.3f);
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(4.0f);
        }
    }
}