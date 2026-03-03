package com.example.everythingtotem;

import com.example.everythingtotem.network.TotemActivatedPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EverythingTotem implements ModInitializer {
    public static final String MOD_ID = "everything-totem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Daftarkan custom payload type agar Fabric tahu cara encode/decode packet ini
        PayloadTypeRegistry.playS2C().register(
                TotemActivatedPayload.ID,
                TotemActivatedPayload.CODEC
        );

        registerTotemEvent();
        LOGGER.info("Everything Totem mod initialized!");
    }

    /**
     * Intercept kematian entity menggunakan Fabric API event.
     * Return false = batalkan kematian.
     * Return true = izinkan mati normal.
     */
    private void registerTotemEvent() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!ModConfig.isTotemMode()) {
                return true;
            }

            boolean activated = activateTotem(entity);
            return !activated;
        });
    }

    /**
     * Aktifkan efek totem pada entity.
     * Jika entity adalah ServerPlayerEntity, kirim packet ke client untuk tampilkan overlay.
     *
     * @return true jika berhasil (ada item di tangan), false jika tidak ada item
     */
    private static boolean activateTotem(LivingEntity entity) {
        ItemStack mainHand = entity.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = entity.getStackInHand(Hand.OFF_HAND);

        ItemStack totemStack;
        Hand totemHand;

        if (!mainHand.isEmpty()) {
            totemStack = mainHand;
            totemHand = Hand.MAIN_HAND;
        } else if (!offHand.isEmpty()) {
            totemStack = offHand;
            totemHand = Hand.OFF_HAND;
        } else {
            return false;
        }

        // Simpan copy untuk dikirim ke client dan untuk stats
        ItemStack usedStack = totemStack.copy();

        // Consume item
        totemStack.decrement(1);

        // Set health ke 1 HP
        entity.setHealth(1.0F);

        // Bersihkan semua efek
        entity.clearStatusEffects();

        // Tambah efek totem
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));

        // Sound totem
        entity.getWorld().playSound(
                null,
                entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ITEM_TOTEM_USE,
                entity.getSoundCategory(),
                1.0F, 1.0F
        );

        // Partikel totem
        if (entity.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    entity.getX(), entity.getY() + 1.0, entity.getZ(),
                    100, 0.5, 0.5, 0.5, 0.5
            );
        }

        // Animasi swing tangan
        entity.swingHand(totemHand);

        // Stats + advancement untuk player
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.incrementStat(Stats.USED.getOrCreateStat(usedStack.getItem()));
            Criteria.USED_TOTEM.trigger(serverPlayer, usedStack);

            // Kirim packet ke client untuk tampilkan totem overlay UI
            // Payload berisi ItemStack yang digunakan agar client bisa render icon item yang benar
            ServerPlayNetworking.send(serverPlayer, new TotemActivatedPayload(usedStack));
        }

        return true;
    }
}