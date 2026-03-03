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
        PayloadTypeRegistry.playS2C().register(
                TotemActivatedPayload.ID,
                TotemActivatedPayload.CODEC
        );
        registerTotemEvent();
        LOGGER.info("Everything Totem mod initialized!");
    }

    private void registerTotemEvent() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!ModConfig.isTotemMode()) return true;
            boolean activated = activateTotem(entity);
            return !activated;
        });
    }

    private static boolean activateTotem(LivingEntity entity) {
        ItemStack mainHand = entity.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand  = entity.getStackInHand(Hand.OFF_HAND);

        ItemStack totemStack;
        Hand totemHand;

        if (!mainHand.isEmpty()) {
            totemStack = mainHand;
            totemHand  = Hand.MAIN_HAND;
        } else if (!offHand.isEmpty()) {
            totemStack = offHand;
            totemHand  = Hand.OFF_HAND;
        } else {
            return false;
        }

        ItemStack usedStack = totemStack.copy();
        totemStack.decrement(1);

        entity.setHealth(1.0F);
        entity.clearStatusEffects();
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,  900, 1));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,     100, 1));

        // ✅ FIX: getWorld() → getEntityWorld() (Yarn mapping 1.21.9)
        entity.getEntityWorld().playSound(
                null,
                entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ITEM_TOTEM_USE,
                entity.getSoundCategory(),
                1.0F, 1.0F
        );

        // ✅ ServerLivingEntityEvents.ALLOW_DEATH hanya jalan di server,
        //    jadi cast ke ServerWorld sudah pasti aman di sini
        if (entity.getEntityWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    entity.getX(), entity.getY() + 1.0, entity.getZ(),
                    100, 0.5, 0.5, 0.5, 0.5
            );
        }

        entity.swingHand(totemHand);

        if (entity instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.incrementStat(Stats.USED.getOrCreateStat(usedStack.getItem()));
            Criteria.USED_TOTEM.trigger(serverPlayer, usedStack);
            ServerPlayNetworking.send(serverPlayer, new TotemActivatedPayload(usedStack));
        }

        return true;
    }
}