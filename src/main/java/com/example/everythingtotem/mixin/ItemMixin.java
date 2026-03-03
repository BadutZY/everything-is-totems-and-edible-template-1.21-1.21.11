package com.example.everythingtotem.mixin;

import com.example.everythingtotem.ModConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
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

/**
 * Mixin untuk Item class.
 * - Semua item bisa dimakan di Edible Mode
 * - Sound effect makan keluar terus-menerus selama makan (bukan hanya di akhir)
 * - Item berkurang setelah selesai dimakan
 */
@Mixin(Item.class)
public class ItemMixin {

    /**
     * Inject ke use() agar semua item bisa di-hold saat Edible Mode aktif.
     */
    @Inject(
            method = "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
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

        // Mulai animasi use/eat
        user.setCurrentHand(hand);
        cir.setReturnValue(ActionResult.CONSUME);
    }

    /**
     * Durasi makan: 32 ticks = 1.6 detik (standar Minecraft).
     */
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

    /**
     * Set animasi ke EAT.
     */
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

    /**
     * FIX SOUND: Inject ke usageTick() yang dipanggil SETIAP TICK selama item digunakan.
     *
     * Vanilla food memainkan sound makan setiap 4 tick selama proses makan berlangsung.
     * Kita replikasi perilaku yang sama untuk semua item non-food.
     *
     * Parameter remainingUseTicks: berapa tick tersisa hingga selesai makan.
     * Total durasi = 32 ticks. Tick pertama = remainingUseTicks=32, terakhir = 1.
     *
     * Cara hitung: play sound setiap saat (32 - remainingUseTicks) % 4 == 0
     * artinya: tick ke-0, 4, 8, 12, 16, 20, 24, 28
     */
    @Inject(
            method = "usageTick(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;I)V",
            at = @At("HEAD")
    )
    private void onUsageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (!ModConfig.isEdibleMode()) {
            return;
        }

        // Jangan double-play untuk item yang sudah punya food component (vanilla sudah handle)
        if (stack.get(DataComponentTypes.FOOD) != null) {
            return;
        }

        // Hitung tick yang sudah berlalu sejak mulai makan
        // getMaxUseTime = 32, jadi ticksElapsed = 32 - remainingUseTicks
        int ticksElapsed = 32 - remainingUseTicks;

        // Play sound setiap 4 tick (sama seperti vanilla food eating sound)
        // Kondisi: ticksElapsed % 4 == 0 dan ticksElapsed >= 0
        if (ticksElapsed >= 0 && ticksElapsed % 4 == 0) {
            // Pitch sedikit random untuk variasi suara yang natural
            float pitch = 0.5F + world.random.nextFloat() * 0.4F;

            world.playSound(
                    // null = play untuk semua player di dekat posisi (server-side broadcast)
                    null,
                    user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_GENERIC_EAT,
                    SoundCategory.PLAYERS,
                    0.5F, // volume sedikit lebih pelan dari finishUsing agar tidak terlalu berisik
                    pitch
            );
        }
    }

    /**
     * Setelah animasi makan selesai:
     * - Play sound akhir makan (crunch/swallow)
     * - Tambah hunger + heal
     * - Kurangi item 1
     *
     * HANYA untuk item non-food.
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

        // Biarkan vanilla handle item yang sudah punya FoodComponent
        if (stack.get(DataComponentTypes.FOOD) != null) {
            return;
        }

        // Sound akhir makan (sedikit lebih keras sebagai tanda selesai)
        world.playSound(
                null,
                user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_GENERIC_EAT,
                SoundCategory.PLAYERS,
                1.0F,
                0.9F + world.random.nextFloat() * 0.2F
        );

        if (!world.isClient && user instanceof PlayerEntity player) {
            // Tambah hunger dan heal
            player.getHungerManager().add(4, 0.3f);
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(2.0f);
            }

            // Kurangi item 1 (tidak berlaku di Creative Mode)
            if (!player.isInCreativeMode()) {
                stack.decrement(1);
            }
        }

        cir.setReturnValue(stack);
    }
}