package com.won983212.servermod.schematic.network;

import com.won983212.servermod.CommonMod;
import com.won983212.servermod.Logger;
import com.won983212.servermod.network.IMessage;
import com.won983212.servermod.network.NetworkDispatcher;
import com.won983212.servermod.schematic.SchematicFile;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class CSchematicUpload implements IMessage {

    public static final int BEGIN = 0;
    public static final int WRITE = 1;
    public static final int FINISH = 2;

    private final int code;
    private final String schematic;
    @Nullable
    private final SchematicFile schematicFile;
    private long size;
    private byte[] data;

    private CSchematicUpload(int code, String schematic, @Nullable SchematicFile schematicFile, long size, byte[] data) {
        this.code = code;
        this.schematic = schematic;
        this.schematicFile = schematicFile;
        this.size = size;
        this.data = data;
    }

    public static CSchematicUpload begin(SchematicFile schematic, long size) {
        return new CSchematicUpload(BEGIN, schematic.getName(), schematic, size, null);
    }

    public static CSchematicUpload write(String schematic, byte[] data) {
        return new CSchematicUpload(WRITE, schematic, null, 0, data);
    }

    public static CSchematicUpload finish(String schematic) {
        return new CSchematicUpload(FINISH, schematic, null, 0, null);
    }

    public CSchematicUpload(PacketBuffer buffer) {
        code = buffer.readByte();
        schematic = buffer.readUtf(256);

        boolean hasFileInfo = buffer.readBoolean();
        if (hasFileInfo) {
            schematicFile = new SchematicFile(buffer);
        } else {
            schematicFile = null;
        }

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

        buffer.writeBoolean(schematicFile != null);
        if (schematicFile != null) {
            schematicFile.writeTo(buffer);
        }

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
                if (schematicFile == null) {
                    Logger.error("Schematic file must not be null at BEGIN!");
                    success = false;
                } else {
                    success = CommonMod.SCHEMATIC_RECEIVER.handleNewUpload(player, schematicFile, size);
                }
            }
            if (code == WRITE) {
                success = CommonMod.SCHEMATIC_RECEIVER.handleWriteRequest(player, schematic, data);
            }
            if (code == FINISH) {
                success = CommonMod.SCHEMATIC_RECEIVER.handleFinishedUpload(player, schematic);
            }
            if (!success) {
                NetworkDispatcher.send(PacketDistributor.PLAYER.with(() -> player),
                        SSchematicReceivedProgress.fail(schematic));
            }
        });
        context.get().setPacketHandled(true);
    }

}
