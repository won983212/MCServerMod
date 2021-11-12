package com.won983212.servermod.client.sound;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

public class ModSounds {
    public static final SoundEvent INDUSTRIAL_ALARM = create("servermod:tile.industrial_alarm");

    private static SoundEvent create(String key) {
        return new SoundEvent(new ResourceLocation(key));
    }
}
