package com.won983212.servermod.schematic.network;

import com.won983212.servermod.client.gui.SchematicSelectionScreen;
import com.won983212.servermod.network.IMessage;
import com.won983212.servermod.schematic.SchematicFile;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SOpenSchematicMenu implements IMessage {
    private final List<SchematicFile> schematicFiles;

    public SOpenSchematicMenu(List<SchematicFile> schematicFiles) {
        this.schematicFiles = schematicFiles;
        if (schematicFiles == null) {
            throw new NullPointerException("schematicFileNames");
        }
    }

    public SOpenSchematicMenu(PacketBuffer buffer) {
        int len = buffer.readShort();
        schematicFiles = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            schematicFiles.add(new SchematicFile(buffer));
        }
    }

    public void write(PacketBuffer buffer) {
        buffer.writeShort(schematicFiles.size());
        for (SchematicFile file : schematicFiles) {
            file.writeTo(buffer);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> Minecraft.getInstance().setScreen(new SchematicSelectionScreen(schematicFiles)));
        context.get().setPacketHandled(true);
    }
}
