package com.dragonminez.client.systems.impactframes;

public class ImpactFrame {
	private float threshold;
	private float thresholdLerp;
	private int duration;
	private boolean invert;

	public ImpactFrame(float threshold, float thresholdLerp, int duration, boolean invert) {
		this.threshold = threshold;
		this.thresholdLerp = thresholdLerp;
		this.duration = duration;
		this.invert = invert;
	}

	public ImpactFrame(ImpactFrame other) {
		this(other.threshold, other.thresholdLerp, other.duration, other.invert);
	}

	public ImpactFrame() {
		this(0.6f, 0.05f, 15, false);
	}

	public float getThreshold() {
		return threshold;
	}

	public float getThresholdLerp() {
		return thresholdLerp;
	}

	public int getDuration() {
		return duration;
	}

	public boolean isInverted() {
		return invert;
	}

	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}

	public void setThresholdLerp(float thresholdLerp) {
		this.thresholdLerp = thresholdLerp;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public void setInverted(boolean invert) {
		this.invert = invert;
	}
}