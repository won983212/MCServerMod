package com.won983212.servermod.network;

import com.won983212.servermod.Logger;
import com.won983212.servermod.skin.SkinCacheCleaner;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SReloadSkin implements IMessage {

    public SReloadSkin() {
    }

    public SReloadSkin(PacketBuffer buf) {
    }

    @Override
    public void write(PacketBuffer buf) {
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide side = ctx.getDirection().getReceptionSide();

        ctx.setPacketHandled(true);
        if (side != LogicalSide.CLIENT) {
            Logger.warn("Wrong side packet message: " + side);
            return;
        }

        ctx.enqueueWork(SkinCacheCleaner::clearSkinCache);
    }
}
