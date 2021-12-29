package com.won983212.servermod.server;

import com.won983212.servermod.CommonModDist;
import com.won983212.servermod.Logger;
import com.won983212.servermod.ServerMod;
import com.won983212.servermod.task.TaskScheduler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ServerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }
        CommonModDist.SERVER_SCHEDULER.tick();
    }
}
