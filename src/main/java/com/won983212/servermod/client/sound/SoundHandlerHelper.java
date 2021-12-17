package com.won983212.servermod.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.Sound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

public class SoundHandlerHelper {
    public static ISound playRepeat(SoundEvent sound, SoundCategory category, BlockPos pos, float volume) {
        return playRepeat(sound, category, pos.getX(), pos.getY(), pos.getZ(), volume);
    }

    public static ISound playRepeat(SoundEvent sound, SoundCategory category, int x, int y, int z, float volume) {
        RepeatPlaySound activeSound = new RepeatPlaySound(sound, category, x, y, z, volume);
        if (isOutClientPlayerInRange(activeSound)) {
            return null;
        }
        Minecraft.getInstance().getSoundManager().play(activeSound);
        return activeSound;
    }

    public static void stop(ISound sound) {
        if (sound instanceof RepeatPlaySound) {
            ((RepeatPlaySound) sound).stopSound();
        }
    }

    public static boolean isOutClientPlayerInRange(ISound sound) {
        if (sound.isRelative() || sound.getAttenuation() == ISound.AttenuationType.NONE) {
            //If the sound is global or has no attenuation, then return that the player is in range
            return false;
        }
        PlayerEntity player = Minecraft.getInstance().player;
        if (player == null) {
            //Shouldn't happen but just in case
            return true;
        }
        Sound s = sound.getSound();
        if (s == null) {
            //If the sound hasn't been initialized yet for some reason try initializing it
            sound.resolve(Minecraft.getInstance().getSoundManager());
            s = sound.getSound();
        }
        //Attenuation distance, defaults to 16 blocks
        int attenuationDistance = s.getAttenuationDistance();
        //Scale the distance based on the sound's volume
        float scaledDistance = Math.max(sound.getVolume(), 1) * attenuationDistance;
        //Check if the player is within range of hearing the sound
        return !(player.position().distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) < scaledDistance * scaledDistance);
    }
}
