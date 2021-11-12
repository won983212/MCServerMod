package com.won983212.servermod.network;

import com.won983212.servermod.ForgeEventHandler;
import com.won983212.servermod.Logger;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ReloadSkinClientMessage {

    public ReloadSkinClientMessage() {
    }

    public static ReloadSkinClientMessage decode(PacketBuffer buf) {
        return new ReloadSkinClientMessage();
    }

    public void encode(PacketBuffer buf) {
    }

    public static void onMessageReceived(final ReloadSkinClientMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide side = ctx.getDirection().getReceptionSide();

        ctx.setPacketHandled(true);
        if (side != LogicalSide.CLIENT) {
            Logger.warn("Wrong side packet message: " + side);
            return;
        }

        ctx.enqueueWork(ForgeEventHandler::clearSkinCache);
    }
}
