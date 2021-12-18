package com.won983212.servermod.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.won983212.servermod.network.NetworkDispatcher;
import com.won983212.servermod.schematic.network.SOpenSchematicMenu;
import com.won983212.servermod.schematic.parser.SchematicFileParser;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import net.minecraftforge.fml.network.PacketDistributor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SchematicCommand {
    public static void register(CommandDispatcher<CommandSource> dispater) {
        LiteralArgumentBuilder<CommandSource> schematicCommand
                = net.minecraft.command.Commands.literal("schem")
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
        List<String> uploadedFiles = getFileList(player.getGameProfile().getName());
        SOpenSchematicMenu packet = new SOpenSchematicMenu(uploadedFiles);
        NetworkDispatcher.send(PacketDistributor.PLAYER.with(() -> player), packet);

        return 1;
    }

    public static List<String> getFileList(String owner) {
        File[] files = getDirectoryPath(owner).toFile().listFiles();
        if (files == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(files)
                .filter((file) -> file.isFile() && SchematicFileParser.isSupportedExtension(file.getName()))
                .map(File::getName)
                .collect(Collectors.toList());
    }

    public static Path getDirectoryPath(String owner) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            return Paths.get("schematics", "uploaded", owner).toAbsolutePath();
        } else {
            return Paths.get("schematics").toAbsolutePath();
        }
    }

    public static Path getFilePath(String owner, String schematicFile) throws IOException {
        Path dir = getDirectoryPath(owner);
        Path path = dir.resolve(Paths.get(schematicFile)).normalize();
        if (!path.startsWith(dir)) {
            throw new IOException("Can't resolve path: " + path);
        }
        return path;
    }
}
