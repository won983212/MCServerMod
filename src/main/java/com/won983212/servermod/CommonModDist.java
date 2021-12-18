package com.won983212.servermod;

import com.won983212.servermod.schematic.SchematicProcessor;
import com.won983212.servermod.schematic.network.ServerSchematicLoader;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class CommonModDist {
    public static final ServerSchematicLoader SCHEMATIC_RECEIVER = new ServerSchematicLoader();

    public void onCommonSetup(FMLCommonSetupEvent event) {
        SchematicProcessor.register();
        SCHEMATIC_RECEIVER.tick();
    }
}
