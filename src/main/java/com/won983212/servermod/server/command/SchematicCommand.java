package com.won983212.servermod.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.won983212.servermod.network.NetworkDispatcher;
import com.won983212.servermod.schematic.SchematicFile;
import com.won983212.servermod.schematic.network.SOpenSchematicMenu;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.PacketDistributor;

public class SchematicCommand {
    public static void register(CommandDispatcher<CommandSource> dispater) {
        LiteralArgumentBuilder<CommandSource> schematicCommand
                = net.minecraft.command.Commands.literal("scm")
                .requires((source) -> source.hasPermission(2))
                .executes(ctx -> openSchematicDialog(ctx.getSource()));
        dispater.register(schematicCommand);
    }

    private static int openSchematicDialog(CommandSource source) {
        Entity e = source.getEntity();
        if (!(e instanceof ServerPlayerEntity)) {
            source.sendFailure(new TranslationTextComponent("servermod.command.error.cantexecuteentity"));
            return 0;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) e;
        SOpenSchematicMenu packet = new SOpenSchematicMenu(SchematicFile.getFileList(player.getGameProfile().getName()));
        NetworkDispatcher.send(PacketDistributor.PLAYER.with(() -> player), packet);

        return 1;
    }
}
