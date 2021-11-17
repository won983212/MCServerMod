package com.won983212.servermod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.won983212.servermod.client.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
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
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ServerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeEventHandler {
    private static final ResourceLocation LEGACY_MAP_FILE = ResourceUtil.getResource("legacy.json");
    private static final HashMap<String, String> LEGACY_ID_MAP = new HashMap<>();
    private static final Minecraft MC_INST = Minecraft.getInstance();

    public static void prepareLegacyIdMap() {
        try {
            InputStream is = Minecraft.getInstance().getResourceManager().getResource(LEGACY_MAP_FILE).getInputStream();
            String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            Type idMapType = new TypeToken<Map<String, String>>() { }.getType();
            setLegacyMap(new Gson().fromJson(json, idMapType));
            is.close();
            Logger.info("Legacy ID Map loaded.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setLegacyMap(Map<String, String> map){
        LEGACY_ID_MAP.clear();
        LEGACY_ID_MAP.putAll(map);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent e) {
        if (Key.KEY_CLEAR_CACHE.isDown()) {
            clearSkinCache();
        }
    }

    @SubscribeEvent
    public static void onTooltipShow(ItemTooltipEvent e) {
        if (e.getFlags().isAdvanced() && !LEGACY_ID_MAP.isEmpty()) {
            String key = Registry.ITEM.getKey(e.getItemStack().getItem()).toString();
            String legacyId = LEGACY_ID_MAP.get(key);
            if (legacyId != null) {
                e.getToolTip().add((new StringTextComponent("# " + legacyId).withStyle(TextFormatting.DARK_GRAY)));
            }
        }
    }

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

    private static void removeCacheFolder(){
        File cacheFolder = MC_INST.getSkinManager().skinsDirectory;
        if (cacheFolder.isDirectory()) {
            try {
                FileUtils.deleteDirectory(cacheFolder);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void clearPlayerSkin(NetworkPlayerInfo info){
        ResourceLocation location = info.getSkinLocation();
        MC_INST.textureManager.release(location);

        location = info.getCapeLocation();
        if (location != null)
            MC_INST.textureManager.release(location);

        location = info.getElytraLocation();
        if (location != null)
            MC_INST.textureManager.release(location);

        info.pendingTextures = false;
        info.textureLocations.clear();
    }
}
