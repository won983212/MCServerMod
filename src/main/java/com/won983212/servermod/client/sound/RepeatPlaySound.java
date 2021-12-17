package com.won983212.servermod.client.sound;

import net.minecraft.client.audio.TickableSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class RepeatPlaySound extends TickableSound {
    private int tick = 0;

    public RepeatPlaySound(SoundEvent sound, SoundCategory category, int x, int y, int z, float volume) {
        super(sound, category);
        this.looping = true;
        this.delay = 0;
        this.volume = volume;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void stopSound() {
        stop();
    }

    public void tick() {
        if (--tick > 0) {
            return;
        }

        if (SoundHandlerHelper.isOutClientPlayerInRange(this)) {
            stop();
        }

        tick = 15;
    }
}
