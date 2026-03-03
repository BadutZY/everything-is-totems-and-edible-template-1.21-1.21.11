package com.example.everythingtotem.mixin;

import com.example.everythingtotem.ModConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void afterItemUsed(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (!ModConfig.isEdibleMode()) return;

        // ✅ FIX: instanceof ServerWorld menggantikan !world.isClient
        if (!(world instanceof ServerWorld)) return;
        if (!(user instanceof PlayerEntity player)) return;

        ItemStack self = (ItemStack) (Object) this;
        if (self.get(DataComponentTypes.FOOD) != null) return;

        player.getHungerManager().add(4, 0.3f);
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(4.0f);
        }
    }
}