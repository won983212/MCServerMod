package com.won983212.servermod.schematic.network;

import com.won983212.servermod.CommonModDist;
import com.won983212.servermod.network.IMessage;
import com.won983212.servermod.network.NetworkDispatcher;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

public class CSchematicUpload implements IMessage {

    public static final int BEGIN = 0;
    public static final int WRITE = 1;
    public static final int FINISH = 2;

    private final int code;
    private long size;
    private final String schematic;
    private byte[] data;

    private CSchematicUpload(int code, String schematic, long size, byte[] data) {
        this.code = code;
        this.schematic = schematic;
        this.size = size;
        this.data = data;
    }

    public static CSchematicUpload begin(String schematic, long size) {
        return new CSchematicUpload(BEGIN, schematic, size, null);
    }

    public static CSchematicUpload write(String schematic, byte[] data) {
        return new CSchematicUpload(WRITE, schematic, 0, data);
    }

    public static CSchematicUpload finish(String schematic) {
        return new CSchematicUpload(FINISH, schematic, 0, null);
    }

    public CSchematicUpload(PacketBuffer buffer) {
        code = buffer.readByte();
        schematic = buffer.readUtf(256);

        if (code == BEGIN) {
            size = buffer.readLong();
        }
        if (code == WRITE) {
            data = buffer.readByteArray();
        }
    }

    public void write(PacketBuffer buffer) {
        buffer.writeByte(code);
        buffer.writeUtf(schematic, 256);

        if (code == BEGIN) {
            buffer.writeLong(size);
        }
        if (code == WRITE) {
            buffer.writeByteArray(data);
        }
    }

    public void handle(Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayerEntity player = context.get().getSender();
            if (player == null) {
                return;
            }

            boolean success = false;
            if (code == BEGIN) {
                success = CommonModDist.SCHEMATIC_RECEIVER.handleNewUpload(player, schematic, size);
            }
            if (code == WRITE) {
                success = CommonModDist.SCHEMATIC_RECEIVER.handleWriteRequest(player, schematic, data);
            }
            if (code == FINISH) {
                success = CommonModDist.SCHEMATIC_RECEIVER.handleFinishedUpload(player, schematic);
            }
            if (!success) {
                NetworkDispatcher.send(PacketDistributor.PLAYER.with(() -> player),
                        SSchematicReceivedProgress.fail(schematic));
            }
        });
        context.get().setPacketHandled(true);
    }

}
