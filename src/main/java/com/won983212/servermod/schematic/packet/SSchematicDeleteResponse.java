package com.won983212.servermod.schematic.packet;

import com.won983212.servermod.client.gui.SchematicSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SSchematicDeleteResponse extends CSchematicFileDelete {

    public SSchematicDeleteResponse(String schematicFileName) {
        super(schematicFileName);
    }

    public SSchematicDeleteResponse(PacketBuffer buffer) {
        super(buffer);
    }

    @OnlyIn(Dist.CLIENT)
    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof SchematicSelectionScreen) {
                ((SchematicSelectionScreen) screen).onResponseFileDeletion(schematicFileName);
            }
        });
        ctx.setPacketHandled(true);
    }
}
