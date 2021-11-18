package com.won983212.servermod.schematic.packet;

import com.won983212.servermod.network.IMessage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class SchematicUploadPacket implements IMessage {

    public static final int BEGIN = 0;
    public static final int WRITE = 1;
    public static final int FINISH = 2;

    private int code;
    private long size;
    private String schematic;
    private byte[] data;

    public SchematicUploadPacket(int code, String schematic) {
        this.code = code;
        this.schematic = schematic;
    }

    public static SchematicUploadPacket begin(String schematic, long size) {
        SchematicUploadPacket pkt = new SchematicUploadPacket(BEGIN, schematic);
        pkt.size = size;
        return pkt;
    }

    public static SchematicUploadPacket write(String schematic, byte[] data) {
        SchematicUploadPacket pkt = new SchematicUploadPacket(WRITE, schematic);
        pkt.data = data;
        return pkt;
    }

    public static SchematicUploadPacket finish(String schematic) {
        return new SchematicUploadPacket(FINISH, schematic);
    }

    public SchematicUploadPacket(PacketBuffer buffer) {
        code = buffer.readInt();
        schematic = buffer.readUtf(256);

        if (code == BEGIN)
            size = buffer.readLong();
        if (code == WRITE)
            data = buffer.readByteArray();
    }

    public void write(PacketBuffer buffer) {
        buffer.writeInt(code);
        buffer.writeUtf(schematic);

        if (code == BEGIN)
            buffer.writeLong(size);
        if (code == WRITE)
            buffer.writeByteArray(data);
    }

    public void handle(Supplier<Context> context) {
        context.get()
                .enqueueWork(() -> {
                    ServerPlayerEntity player = context.get()
                            .getSender();
                    if (player == null)
                        return;
                    // TODO Handle upload packet
				/*if (code == BEGIN) {
					BlockPos pos = ((SchematicTableContainer) player.containerMenu).getTileEntity()
							.getBlockPos();
					Create.SCHEMATIC_RECEIVER.handleNewUpload(player, schematic, size, pos);
				}
				if (code == WRITE)
					Create.SCHEMATIC_RECEIVER.handleWriteRequest(player, schematic, data);
				if (code == FINISH)
					Create.SCHEMATIC_RECEIVER.handleFinishedUpload(player, schematic);*/
                });
        context.get()
                .setPacketHandled(true);
    }

}
