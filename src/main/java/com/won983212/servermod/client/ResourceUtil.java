package com.won983212.servermod.client;

import com.won983212.servermod.ServerMod;
import net.minecraft.util.ResourceLocation;

public class ResourceUtil {
    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(ServerMod.MODID + ":" + path);
    }
}
