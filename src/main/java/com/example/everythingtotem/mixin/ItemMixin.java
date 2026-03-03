package com.example.everythingtotem.mixin;

import com.example.everythingtotem.ModConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin untuk Item class.
 * Kompatibel 100% dengan Minecraft 1.21 Yarn 1.21+build.1
 *
 * CATATAN PENTING tentang method signature di Yarn 1.21+build.1:
 *
 * - use()           : (World, PlayerEntity, Hand) -> TypedActionResult<ItemStack>
 * - getMaxUseTime() : (ItemStack, LivingEntity) -> int   << DUA parameter!
 * - getUseAction()  : (ItemStack) -> UseAction            << di net.minecraft.util
 * - usageTick()     : (World, LivingEntity, ItemStack, int) -> void
 * - finishUsing()   : (ItemStack, World, LivingEntity) -> ItemStack
 *
 * Cek food item: stack.get(DataComponentTypes.FOOD) != null
 * DataComponentTypes ada di: net.minecraft.component
 * UseAction ada di: net.minecraft.util.UseAction
 */
@Mixin(Item.class)
public class ItemMixin {

    /**
     * Inject ke use() agar semua item bisa di-hold saat Edible Mode aktif.
     */
    @Inject(
            method = "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onUse(World world, PlayerEntity user, Hand hand,
                       CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!ModConfig.isEdibleMode()) {
            return;
        }

        ItemStack stack = user.getStackInHand(hand);

        // Biarkan vanilla handle item yang sudah punya FoodComponent
        if (stack.get(DataComponentTypes.FOOD) != null) {
            return;
        }

        if (stack.isEmpty()) {
            return;
        }

        user.setCurrentHand(hand);
        cir.setReturnValue(TypedActionResult.consume(stack));
    }

    /**
     * Override durasi makan: 32 ticks = 1.6 detik.
     *
     * PENTING: Di Yarn 1.21+build.1 signature adalah:
     * getMaxUseTime(ItemStack stack, LivingEntity user) -> int
     * Ada DUA parameter!
     */
    @Inject(
            method = "getMaxUseTime(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void modifyMaxUseTime(ItemStack stack, LivingEntity user,
                                  CallbackInfoReturnable<Integer> cir) {
        if (!ModConfig.isEdibleMode()) {
            return;
        }
        if (stack.get(DataComponentTypes.FOOD) != null) {
            return;
        }
        cir.setReturnValue(32);
    }

    /**
     * Override animasi use menjadi EAT.
     * UseAction ada di net.minecraft.util.UseAction
     */
    @Inject(
            method = "getUseAction(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/util/UseAction;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void modifyUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (!ModConfig.isEdibleMode()) {
            return;
        }
        if (stack.get(DataComponentTypes.FOOD) != null) {
            return;
        }
        cir.setReturnValue(UseAction.EAT);
    }

    /**
     * Play sound makan setiap 4 tick selama item digunakan.
     * Mereplikasi behavior vanilla food eating sound.
     */
    @Inject(
            method = "usageTick(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;I)V",
            at = @At("HEAD")
    )
    private void onUsageTick(World world, LivingEntity user, ItemStack stack,
                             int remainingUseTicks, CallbackInfo ci) {
        if (!ModConfig.isEdibleMode()) {
            return;
        }

        if (stack.get(DataComponentTypes.FOOD) != null) {
            return;
        }

        int ticksElapsed = 32 - remainingUseTicks;

        if (ticksElapsed >= 0 && ticksElapsed % 4 == 0) {
            float pitch = 0.5F + world.random.nextFloat() * 0.4F;
            world.playSound(
                    null,
                    user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_GENERIC_EAT,
                    SoundCategory.PLAYERS,
                    0.5F,
                    pitch
            );
        }
    }

    /**
     * Setelah animasi makan selesai:
     * - Play sound akhir makan
     * - Tambah hunger + heal
     * - Kurangi item 1 (kecuali Creative Mode)
     */
    @Inject(
            method = "finishUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onFinishUsing(ItemStack stack, World world, LivingEntity user,
                               CallbackInfoReturnable<ItemStack> cir) {
        if (!ModConfig.isEdibleMode()) {
            return;
        }

        if (stack.get(DataComponentTypes.FOOD) != null) {
            return;
        }

        world.playSound(
                null,
                user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_GENERIC_EAT,
                SoundCategory.PLAYERS,
                1.0F,
                0.9F + world.random.nextFloat() * 0.2F
        );

        if (!world.isClient && user instanceof PlayerEntity player) {
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