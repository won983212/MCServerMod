package com.won983212.servermod.schematic.network;

import com.won983212.servermod.Settings;
import com.won983212.servermod.utility.Lang;
import net.minecraft.entity.player.PlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SchematicNetwork {
    protected void createFolderIfMissing(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static boolean isSchematicSizeTooBig(PlayerEntity player, long size) {
        if (size > Settings.MAX_TOTAL_SCHEMATIC_SIZE * 1000) {
            if (player != null) {
                player.sendMessage(Lang.translate("schematics.uploadTooLarge").append(" (" + size / 1000 + " KB)."), player.getUUID());
                player.sendMessage(Lang.translate("schematics.maxAllowedSize").append(" " + Settings.MAX_TOTAL_SCHEMATIC_SIZE + " KB"), player.getUUID());
            }
            return true;
        }
        return false;
    }
}
