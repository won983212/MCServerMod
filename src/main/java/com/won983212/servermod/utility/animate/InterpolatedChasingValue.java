package com.won983212.servermod.utility.animate;

public class InterpolatedChasingValue extends InterpolatedValue {

    private float speed = 0.5f;
    private float target = 0;
    private final float eps = 1 / 4096f;

    public void tick() {
        float diff = getCurrentDiff();
        if (Math.abs(diff) < eps) {
            return;
        }
        set(value + (diff) * speed);
    }

    protected float getCurrentDiff() {
        return getTarget() - value;
    }

    public InterpolatedChasingValue withSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    public InterpolatedChasingValue target(float target) {
        this.target = target;
        return this;
    }

    public InterpolatedChasingValue start(float value) {
        lastValue = this.value = value;
        target(value);
        return this;
    }

    public float getTarget() {
        return target;
    }

}
