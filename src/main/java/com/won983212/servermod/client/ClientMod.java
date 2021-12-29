package com.won983212.servermod.client;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.won983212.servermod.CommonMod;
import com.won983212.servermod.Logger;
import com.won983212.servermod.ModKeys;
import com.won983212.servermod.ServerMod;
import com.won983212.servermod.client.gui.ConfigScreen;
import com.won983212.servermod.client.render.tile.RenderIndustrialAlarm;
import com.won983212.servermod.skin.SkinCacheCleaner;
import com.won983212.servermod.tile.ModTiles;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ServerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientMod extends CommonMod {
    private static final ResourceLocation LEGACY_MAP_FILE = ResourceUtil.getResource("legacy.json");
    private static final HashMap<String, String> LEGACY_ID_MAP = new HashMap<>();

    public static void prepareLegacyIdMap() {
        try {
            InputStream is = Minecraft.getInstance().getResourceManager().getResource(LEGACY_MAP_FILE).getInputStream();
            String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            Type idMapType = new TypeToken<Map<String, String>>() {
            }.getType();
            setLegacyMap(new Gson().fromJson(json, idMapType));
            is.close();
            Logger.info("Legacy ID Map loaded.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setLegacyMap(Map<String, String> map) {
        LEGACY_ID_MAP.clear();
        LEGACY_ID_MAP.putAll(map);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        super.onCommonSetup(event);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (mc, screen) -> new ConfigScreen()
        );
        prepareLegacyIdMap();
        ClientRegistry.bindTileEntityRenderer(ModTiles.tileEntityIndustrialAlarm, RenderIndustrialAlarm::new);
        ModKeys.registerKeys();
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

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent e) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        if (ModKeys.KEY_CLEAR_CACHE.isDown()) {
            SkinCacheCleaner.clearSkinCache();
        }
    }
}
