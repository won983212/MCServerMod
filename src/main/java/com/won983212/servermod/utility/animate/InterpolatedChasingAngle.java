package com.won983212.servermod.utility.animate;

public class InterpolatedChasingAngle extends InterpolatedChasingValue {

    public float get(float partialTicks) {
        return lastValue + getShortestAngleDiff(lastValue, value) * partialTicks;
    }

    @Override
    protected float getCurrentDiff() {
        return getShortestAngleDiff(value, getTarget());
    }

    private static float getShortestAngleDiff(double current, double target) {
        current = current % 360;
        target = target % 360;
        return (float) (((((target - current) % 360) + 540) % 360) - 180);
    }

}
