package com.won983212.servermod.client.sound;

import net.minecraft.client.audio.TickableSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class RepeatPlaySound extends TickableSound {
    private int tick = 0;

    public RepeatPlaySound(SoundEvent sound, SoundCategory category, int x, int y, int z, float volume) {
        super(sound, category);
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = volume;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean shouldPlaySound() {
        return true;
    }

    public boolean canBeSilent() {
        return false;
    }

    public void stop() {
        finishPlaying();
    }

    public void tick() {
        if (--tick > 0)
            return;

        if (!SoundHandlerHelper.isClientPlayerInRange(this))
            stop();

        tick = 15;
    }
}
