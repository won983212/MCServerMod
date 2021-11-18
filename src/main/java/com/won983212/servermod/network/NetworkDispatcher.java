package com.won983212.servermod.network;

import com.won983212.servermod.ServerMod;
import com.won983212.servermod.schematic.packet.SchematicUploadPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetworkDispatcher {
    private static final String MESSAGE_PROTOCOL_VERSION = "1.0";
    private static final ResourceLocation CHANNEL_NAME = new ResourceLocation(ServerMod.MODID, "networkchannel");
    private static SimpleChannel channel;

    public static void initDispatcher() {
        channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_NAME)
                .serverAcceptedVersions(MESSAGE_PROTOCOL_VERSION::equals)
                .clientAcceptedVersions(MESSAGE_PROTOCOL_VERSION::equals)
                .networkProtocolVersion(() -> MESSAGE_PROTOCOL_VERSION)
                .simpleChannel();
        Packets.registerAllPackets(channel);
    }

    public static void sendToAll(IMessage message) {
        send(PacketDistributor.ALL.noArg(), message);
    }

    public static void sendToServer(IMessage message) {
        channel.sendToServer(message);
    }

    public static void send(PacketDistributor.PacketTarget target, IMessage message) {
        channel.send(target, message);
    }
}
