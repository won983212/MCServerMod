package com.won983212.servermod.network;

import com.won983212.servermod.schematic.packet.SchematicPlacePacket;
import com.won983212.servermod.schematic.packet.SchematicSyncPacket;
import com.won983212.servermod.schematic.packet.SchematicUploadPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public enum Packets {
    RELOAD_SKIN(ReloadSkinClientMessage.class, ReloadSkinClientMessage::new, NetworkDirection.PLAY_TO_CLIENT),
    SCHEMATIC_UPLOAD(SchematicUploadPacket.class, SchematicUploadPacket::new, NetworkDirection.PLAY_TO_SERVER),
    SCHEMATIC_PLACE(SchematicPlacePacket.class, SchematicPlacePacket::new, NetworkDirection.PLAY_TO_SERVER),
    SCHEMATIC_SYNC(SchematicSyncPacket.class, SchematicSyncPacket::new, NetworkDirection.PLAY_TO_SERVER);

    private final PacketLoader<?> loader;

    <T extends IMessage> Packets(Class<T> type, Function<PacketBuffer, T> factory, NetworkDirection direction) {
        this.loader = new PacketLoader<>(type, factory, direction);
    }

    public static void registerAllPackets(SimpleChannel channel){
        for (Packets packet : values()) {
            packet.loader.reigster(channel);
        }
    }

    private static class PacketLoader<T extends IMessage> {
        private static int id = 0;
        private final BiConsumer<T, PacketBuffer> encoder;
        private final Function<PacketBuffer, T> decoder;
        private final BiConsumer<T, Supplier<NetworkEvent.Context>> handler;
        private final Class<T> type;
        private final NetworkDirection direction;

        private PacketLoader(Class<T> type, Function<PacketBuffer, T> factory, NetworkDirection direction) {
            encoder = T::write;
            decoder = factory;
            handler = T::handle;
            this.type = type;
            this.direction = direction;
        }

        public void reigster(SimpleChannel channel){
            channel.messageBuilder(type, id++, direction)
                    .encoder(encoder)
                    .decoder(decoder)
                    .consumer(handler)
                    .add();
        }
    }
}
