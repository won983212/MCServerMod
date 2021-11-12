package com.won983212.servermod.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public interface IMessage<T> {
    T decode(PacketBuffer buf);

    void encode(PacketBuffer buf);

    void onMessageReceived(final T message, Supplier<NetworkEvent.Context> ctxSupplier);
}
