package com.won983212.servermod;

import com.won983212.servermod.schematic.SchematicProcessor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ModDist {
    public void onCommonSetup(FMLCommonSetupEvent event) {
        SchematicProcessor.register();
    }
}
