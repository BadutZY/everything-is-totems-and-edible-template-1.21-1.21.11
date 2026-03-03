package com.example.everythingtotem.network;

import com.example.everythingtotem.EverythingTotem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Custom S2C Packet: Dikirim dari server ke client saat totem berhasil diaktifkan.
 * Membawa data ItemStack yang digunakan sebagai totem agar client bisa render overlay-nya.
 *
 * Menggunakan Fabric API custom payload networking (1.21.4 compatible).
 */
public record TotemActivatedPayload(ItemStack totemStack) implements CustomPayload {

    public static final Id<TotemActivatedPayload> ID =
            new Id<>(Identifier.of(EverythingTotem.MOD_ID, "totem_activated"));

    /**
     * PacketCodec untuk serialize/deserialize ItemStack lewat jaringan.
     * ItemStack.OPTIONAL_PACKET_CODEC handles null/empty stack dengan aman.
     */
    public static final PacketCodec<RegistryByteBuf, TotemActivatedPayload> CODEC =
            ItemStack.OPTIONAL_PACKET_CODEC.xmap(
                    TotemActivatedPayload::new,
                    TotemActivatedPayload::totemStack
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}