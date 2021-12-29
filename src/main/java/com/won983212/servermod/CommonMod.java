package com.won983212.servermod;

import com.mojang.brigadier.CommandDispatcher;
import com.won983212.servermod.schematic.network.ServerSchematicLoader;
import com.won983212.servermod.server.Commands;
import com.won983212.servermod.task.TaskScheduler;
import net.minecraft.command.CommandSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod.EventBusSubscriber
public class CommonMod {
    public static final ServerSchematicLoader SCHEMATIC_RECEIVER = new ServerSchematicLoader();
    public static final TaskScheduler CLIENT_SCHEDULER = new TaskScheduler();
    public static final TaskScheduler SERVER_SCHEDULER = new TaskScheduler();

    public void onCommonSetup(FMLCommonSetupEvent event) {
        SCHEMATIC_RECEIVER.tick();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        CommonMod.SCHEMATIC_RECEIVER.tick();
        CommonMod.SERVER_SCHEDULER.tick();
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSource> dispater = e.getDispatcher();
        for (Commands command : Commands.values()) {
            command.reigster(dispater);
        }
    }

    @SubscribeEvent
    public static void serverStopped(FMLServerStoppingEvent event) {
        CommonMod.SCHEMATIC_RECEIVER.shutdown();
    }
}
