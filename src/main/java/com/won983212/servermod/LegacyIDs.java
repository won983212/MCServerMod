package com.won983212.servermod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.won983212.servermod.client.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LegacyIDs {
    private static final ResourceLocation LEGACY_MAP_FILE = ResourceUtil.getResource("legacy.json");
    private static final HashMap<String, String> LEGACY_ID_MAP = new HashMap<>();

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

    public static String getLegacyId(Item item){
        if (!LEGACY_ID_MAP.isEmpty()) {
            String key = Registry.ITEM.getKey(item).toString();
            return LEGACY_ID_MAP.get(key);
        }
        return null;
    }
}
