package com.won983212.servermod.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class SkinCacheCleaner {
    private static final Minecraft MC_INST = Minecraft.getInstance();

    public static void clearSkinCache() {
        removeCacheFolder();
        ClientPlayNetHandler connection = MC_INST.getConnection();
        if (connection == null) {
            MC_INST.gui.getChat().addMessage(new TranslationTextComponent("servermod.message.cachecleared"));
            return;
        }
        for (NetworkPlayerInfo info : connection.getOnlinePlayers()) {
            clearPlayerSkin(info);
        }
        MC_INST.gui.getChat().addMessage(new TranslationTextComponent("servermod.message.cachecleared"));
    }

    private static void removeCacheFolder() {
        File cacheFolder = MC_INST.getSkinManager().skinsDirectory;
        if (cacheFolder.isDirectory()) {
            try {
                FileUtils.deleteDirectory(cacheFolder);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void clearPlayerSkin(NetworkPlayerInfo info) {
        ResourceLocation location = info.getSkinLocation();
        MC_INST.textureManager.release(location);

        location = info.getCapeLocation();
        if (location != null) {
            MC_INST.textureManager.release(location);
        }

        location = info.getElytraLocation();
        if (location != null) {
            MC_INST.textureManager.release(location);
        }

        info.pendingTextures = false;
        info.textureLocations.clear();
    }
}
