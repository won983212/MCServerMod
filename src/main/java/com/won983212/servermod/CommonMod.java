package com.won983212.servermod;

import com.mojang.brigadier.CommandDispatcher;
import com.won983212.servermod.server.Commands;
import net.minecraft.command.CommandSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber
public class CommonMod {
    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSource> dispater = e.getDispatcher();
        for (Commands command : Commands.values()) {
            command.reigster(dispater);
        }
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
    }
}
