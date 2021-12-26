package com.won983212.servermod;

import com.mojang.brigadier.CommandDispatcher;
import com.won983212.servermod.server.Commands;
import com.won983212.servermod.task.TaskScheduler;
import net.minecraft.command.CommandSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod.EventBusSubscriber
public class CommonEventHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        CommonModDist.SCHEMATIC_RECEIVER.tick();
        TaskScheduler.tick();
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
        CommonModDist.SCHEMATIC_RECEIVER.shutdown();
    }
}
