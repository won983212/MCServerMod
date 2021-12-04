package com.won983212.servermod;

import com.won983212.servermod.schematic.SchematicPrinter;
import com.won983212.servermod.schematic.SchematicProcessor;
import com.won983212.servermod.schematic.ServerSchematicLoader;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.SynchronousQueue;

public class CommonModDist {
    public static final ServerSchematicLoader SCHEMATIC_RECEIVER = new ServerSchematicLoader();
    public static final Queue<SchematicPrinter> PRINTERS = new LinkedList<>();

    public void onCommonSetup(FMLCommonSetupEvent event) {
        SchematicProcessor.register();
        SCHEMATIC_RECEIVER.tick();
    }
}
