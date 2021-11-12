package com.won983212.servermod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.won983212.servermod.client.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ServerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeEventHandler {
    // TODO legacy.json File not found?!?!
    private static final ResourceLocation legacyMapFile = ResourceUtil.getResource("legacy.json");
    private static Map<String, String> LEGACY_ID_MAP = null;

    public static void prepareLegacyIdMap() {
        try {
            IResource resource = Minecraft.getInstance().getResourceManager().getResource(legacyMapFile);
            InputStream is = resource.getInputStream();

            String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Type idMapType = new TypeToken<Map<String, String>>() {
            }.getType();
            LEGACY_ID_MAP = gson.fromJson(json, idMapType);

            is.close();
            Logger.info("Legacy ID Map loaded.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent e) {
        if (Key.KEY_CLEAR_CACHE.isPressed()) {
            clearSkinCache();
        }
    }

    @SubscribeEvent
    public static void onTooltipShow(ItemTooltipEvent e) {
        if (e.getFlags().isAdvanced() && LEGACY_ID_MAP != null) {
            String key = Registry.ITEM.getKey(e.getItemStack().getItem()).toString();
            String legacyId = LEGACY_ID_MAP.get(key);
            if (legacyId != null) {
                e.getToolTip().add((new StringTextComponent("# " + legacyId).mergeStyle(TextFormatting.DARK_GRAY)));
            }
        }
    }

    public static void clearSkinCache() {
        Minecraft minecraft = Minecraft.getInstance();
        File cacheFolder = minecraft.getSkinManager().skinCacheDir;
        if (cacheFolder.isDirectory()) {
            try {
                FileUtils.deleteDirectory(cacheFolder);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        ClientPlayNetHandler connection = minecraft.getConnection();
        if (connection == null) {
            minecraft.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("servermod.message.cachecleared"));
            return;
        }

        for (NetworkPlayerInfo info : connection.getPlayerInfoMap()) {
            ResourceLocation location = info.getLocationSkin();
            minecraft.textureManager.deleteTexture(location);

            location = info.getLocationCape();
            if (location != null)
                minecraft.textureManager.deleteTexture(location);

            location = info.getLocationElytra();
            if (location != null)
                minecraft.textureManager.deleteTexture(location);

            info.playerTexturesLoaded = false;
            info.playerTextures.clear();
        }

        minecraft.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("servermod.message.cachecleared"));
    }
}
