package com.won983212.servermod.utility;

public class InterpolatedChasingAngle extends InterpolatedChasingValue {

	public float get(float partialTicks) {
		return AngleHelper.angleLerp(partialTicks, lastValue, value);
	}
	
	@Override
	protected float getCurrentDiff() {
		return AngleHelper.getShortestAngleDiff(value, getTarget());
	}

}
