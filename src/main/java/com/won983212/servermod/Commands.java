package com.won983212.servermod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.won983212.servermod.network.NetworkDispatcher;
import com.won983212.servermod.network.ReloadSkinClientMessage;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ServerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class Commands {
    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSource> dispater = e.getDispatcher();
        LiteralArgumentBuilder<CommandSource> skinCommand
                = net.minecraft.command.Commands.literal("skin")
                .requires((source) -> source.hasPermission(2))
                .then(net.minecraft.command.Commands.literal("reload")
                        .executes(ctx -> reloadSkin(ctx.getSource())));
        dispater.register(skinCommand);
    }

    private static int reloadSkin(CommandSource source) {
        NetworkDispatcher.sendToAll(new ReloadSkinClientMessage());
        source.sendSuccess(new TranslationTextComponent("servermod.command.reloaded"), true);
        return 1;
    }
}
