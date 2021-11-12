package com.won983212.servermod.tile;

import com.won983212.servermod.client.sound.ModSounds;
import com.won983212.servermod.client.sound.SoundHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.SoundCategory;

public class TileEntityIndustrialAlarm extends ActiveStateTile {
    private ISound activeSound = null;
    private int playSoundCooldown = 0;

    public TileEntityIndustrialAlarm() {
        super(ModTiles.tileEntityIndustrialAlarm);
    }

    @Override
    protected void onPowerChange() {
        setActive(isPowered());
    }

    @Override
    public void tick() {
        if (isClient()) {
            updateSound();
        }
    }

    @Override
    public void remove() {
        super.remove();
        if (isClient()) {
            updateSound();
        }
    }

    private void updateSound() {
        if (isActive() && !isRemoved()) {
            if (--playSoundCooldown > 0) {
                return;
            }
            if (!Minecraft.getInstance().getSoundHandler().isPlaying(activeSound)) {
                activeSound = SoundHandlerHelper.playRepeat(ModSounds.INDUSTRIAL_ALARM, SoundCategory.RECORDS, getPos(), 6);
            }
            playSoundCooldown = 20;
        } else if (activeSound != null) {
            SoundHandlerHelper.stop(activeSound);
            activeSound = null;
            playSoundCooldown = 0;
        }
    }
}