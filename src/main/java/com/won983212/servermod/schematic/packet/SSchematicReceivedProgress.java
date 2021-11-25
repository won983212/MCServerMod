package com.won983212.servermod.schematic.packet;

import com.won983212.servermod.client.ClientDist;
import com.won983212.servermod.network.IMessage;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SSchematicReceivedProgress implements IMessage {

    public static final int SUCCESS = 1;
    public static final int FAIL = 2;

    private final int code;
    private final String schematic;
    private final float receivedProgress;

    private SSchematicReceivedProgress(int code, String schematic, float receivedProgress) {
        this.code = code;
        this.schematic = schematic;
        this.receivedProgress = receivedProgress;
    }

    public static SSchematicReceivedProgress success(String schematic, float receivedProgress) {
        return new SSchematicReceivedProgress(SUCCESS, schematic, receivedProgress);
    }

    public static SSchematicReceivedProgress fail(String schematic) {
        return new SSchematicReceivedProgress(FAIL, schematic, 0);
    }

    public SSchematicReceivedProgress(PacketBuffer buf) {
        code = buf.readByte();
        schematic = buf.readUtf(256);
        receivedProgress = buf.readFloat();
    }

    @Override
    public void write(PacketBuffer buf) {
        buf.writeByte(code);
        buf.writeUtf(schematic, 256);
        buf.writeFloat(receivedProgress);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (code == SUCCESS) {
                ClientDist.SCHEMATIC_SENDER.handleServerProgress(schematic, receivedProgress);
            }
            if (code == FAIL) {
                ClientDist.SCHEMATIC_SENDER.handleFailState(schematic);
            }
        });
        context.get().setPacketHandled(true);
    }
}
