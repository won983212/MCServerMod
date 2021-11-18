package com.won983212.servermod.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public interface IMessage {
    void write(PacketBuffer buf);

    void handle(Supplier<NetworkEvent.Context> ctxSupplier);
}
