package com.won983212.servermod.utility.animate;

import net.minecraft.util.math.MathHelper;

public class InterpolatedValue {

    public float value = 0;
    public float lastValue = 0;

    public void set(float value) {
        lastValue = this.value;
        this.value = value;
    }

    public float get(float partialTicks) {
        return MathHelper.lerp(partialTicks, lastValue, value);
    }
}
