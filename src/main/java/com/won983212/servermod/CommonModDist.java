package com.won983212.servermod;

import com.won983212.servermod.schematic.network.ServerSchematicLoader;
import com.won983212.servermod.task.TaskScheduler;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class CommonModDist {
    public static final ServerSchematicLoader SCHEMATIC_RECEIVER = new ServerSchematicLoader();
    public static final TaskScheduler CLIENT_SCHEDULER = new TaskScheduler();
    public static final TaskScheduler SERVER_SCHEDULER = new TaskScheduler();

    public void onCommonSetup(FMLCommonSetupEvent event) {
        SCHEMATIC_RECEIVER.tick();
    }
}
