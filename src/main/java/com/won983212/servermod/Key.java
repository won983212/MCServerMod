package com.won983212.servermod;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class Key {
    public static final KeyBinding KEY_CLEAR_CACHE;

    public static void registerKeys() {
        ClientRegistry.registerKeyBinding(KEY_CLEAR_CACHE);
    }

    static {
        KEY_CLEAR_CACHE = new KeyBinding("key.clearcache.desc", KeyConflictContext.IN_GAME, KeyModifier.CONTROL,
                InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.servermod.category");
    }
}