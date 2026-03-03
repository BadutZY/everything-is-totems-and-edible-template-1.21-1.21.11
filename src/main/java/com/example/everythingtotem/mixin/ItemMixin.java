package com.example.everythingtotem.mixin;

import com.example.everythingtotem.ModConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(
            method = "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!ModConfig.isEdibleMode()) return;

        ItemStack stack = user.getStackInHand(hand);
        if (stack.get(DataComponentTypes.FOOD) != null) return;
        if (stack.isEmpty()) return;

        user.setCurrentHand(hand);
        cir.setReturnValue(ActionResult.CONSUME);
    }

    @Inject(
            method = "getMaxUseTime(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void modifyMaxUseTime(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (ModConfig.isEdibleMode()) {
            cir.setReturnValue(32);
        }
    }

    @Inject(
            method = "getUseAction(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/consume/UseAction;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void modifyUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (ModConfig.isEdibleMode()) {
            cir.setReturnValue(UseAction.EAT);
        }
    }

    @Inject(
            method = "usageTick(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;I)V",
            at = @At("HEAD")
    )
    private void onUsageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (!ModConfig.isEdibleMode()) return;
        if (stack.get(DataComponentTypes.FOOD) != null) return;

        int ticksElapsed = 32 - remainingUseTicks;
        if (ticksElapsed >= 0 && ticksElapsed % 4 == 0) {
            float pitch = 0.5F + world.random.nextFloat() * 0.4F;
            world.playSound(
                    null,
                    user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_GENERIC_EAT,
                    SoundCategory.PLAYERS,
                    0.5F, pitch
            );
        }
    }

    @Inject(
            method = "finishUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onFinishUsing(ItemStack stack, World world, LivingEntity user,
                               CallbackInfoReturnable<ItemStack> cir) {
        if (!ModConfig.isEdibleMode()) return;
        if (stack.get(DataComponentTypes.FOOD) != null) return;

        world.playSound(
                null,
                user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_GENERIC_EAT,
                SoundCategory.PLAYERS,
                1.0F,
                0.9F + world.random.nextFloat() * 0.2F
        );

        // ✅ FIX: instanceof ServerWorld menggantikan !world.isClient
        if (world instanceof ServerWorld && user instanceof PlayerEntity player) {
            player.getHungerManager().add(4, 0.3f);
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(2.0f);
            }
            if (!player.isInCreativeMode()) {
                stack.decrement(1);
            }
        }

        cir.setReturnValue(stack);
    }
}