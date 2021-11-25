package com.won983212.servermod.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.won983212.servermod.network.NetworkDispatcher;
import com.won983212.servermod.network.SReloadSkin;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.TranslationTextComponent;

public class ReloadSkinCommand {
    public static void register(CommandDispatcher<CommandSource> dispater) {
        LiteralArgumentBuilder<CommandSource> skinCommand
                = net.minecraft.command.Commands.literal("skin")
                .requires((source) -> source.hasPermission(2))
                .then(net.minecraft.command.Commands.literal("reload")
                        .executes(ctx -> reloadSkin(ctx.getSource())));
        dispater.register(skinCommand);
    }

    private static int reloadSkin(CommandSource source) {
        NetworkDispatcher.sendToAll(new SReloadSkin());
        source.sendSuccess(new TranslationTextComponent("servermod.command.reloaded"), true);
        return 1;
    }
}
