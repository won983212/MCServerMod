package com.won983212.servermod.schematic.packet;

import com.won983212.servermod.client.gui.SchematicSelectionScreen;
import com.won983212.servermod.network.IMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SOpenSchematicMenu implements IMessage {
    private final List<String> schematicFileNames;

    public SOpenSchematicMenu(List<String> schematicFileNames) {
        this.schematicFileNames = schematicFileNames;
        if (schematicFileNames == null) {
            throw new NullPointerException("schematicFileNames");
        }
    }

    public SOpenSchematicMenu(PacketBuffer buffer) {
        int len = buffer.readShort();
        schematicFileNames = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            schematicFileNames.add(buffer.readUtf(64));
        }
    }

    public void write(PacketBuffer buffer) {
        buffer.writeShort(schematicFileNames.size());
        for (String fileName : schematicFileNames) {
            buffer.writeUtf(fileName, 64);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> Minecraft.getInstance().setScreen(new SchematicSelectionScreen(schematicFileNames)));
        context.get().setPacketHandled(true);
    }
}
