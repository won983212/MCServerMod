package com.won983212.servermod.utility.animate;

public class InterpolatedChasingValue extends InterpolatedValue {
    private static final float EPS = 1 / 4096f;
    private static final float SPEED = 0.5f;
    private float target = 0;

    public void tick() {
        float diff = getCurrentDiff();
        if (Math.abs(diff) < EPS) {
            return;
        }
        set(value + (diff) * SPEED);
    }

    protected float getCurrentDiff() {
        return getTarget() - value;
    }

    public void target(float target) {
        this.target = target;
    }

    public void start(float value) {
        lastValue = this.value = value;
        target(value);
    }

    public float getTarget() {
        return target;
    }

}
