package com.won983212.servermod.network;

import com.won983212.servermod.ServerMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkDispatcher {
    private static final String MESSAGE_PROTOCOL_VERSION = "1.0";
    private static final ResourceLocation channelRL = new ResourceLocation(ServerMod.MODID, "networkchannel");
    private static SimpleChannel channel;

    public static void initDispatcher() {
        channel = NetworkRegistry.newSimpleChannel(channelRL, () -> MESSAGE_PROTOCOL_VERSION,
                NetworkDispatcher::isMatchVersion, NetworkDispatcher::isMatchVersion);

        int id = 0;
        channel.registerMessage(++id, ReloadSkinClientMessage.class, ReloadSkinClientMessage::encode,
                ReloadSkinClientMessage::decode, ReloadSkinClientMessage::onMessageReceived);
    }

    public static void sendToAll(Object message) {
        send(PacketDistributor.ALL.noArg(), message);
    }

    public static void send(PacketDistributor.PacketTarget target, Object message) {
        channel.send(target, message);
    }

    private static boolean isMatchVersion(String protocolVersion) {
        return MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }
}
