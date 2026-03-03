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
 * Mixin untuk ItemStack — backup apply efek makan di level ItemStack.
 * Kompatibel dengan Minecraft 1.21 Yarn 1.21+build.1.
 *
 * Cek food item menggunakan DataComponentTypes.FOOD dari net.minecraft.component
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    /**
     * Inject ke finishUsing di level ItemStack.
     * Hanya apply efek untuk item yang tidak punya FoodComponent asli.
     */
    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void afterItemUsed(World world, LivingEntity user,
                               CallbackInfoReturnable<ItemStack> cir) {
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

        // Apply efek makan tambahan
        player.getHungerManager().add(4, 0.3f);
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(4.0f);
        }
    }
}