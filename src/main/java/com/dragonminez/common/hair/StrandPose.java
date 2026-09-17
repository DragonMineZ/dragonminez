package com.dragonminez.common.hair;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class StrandPose {
	public CustomHair.HairFace face;
	public int index;
	public int id;
	public int segments;
	public HairJointStyle jointStyle = HairJointStyle.BLOCKS;
	public final HairPhysicsProfile physics = HairPhysicsProfile.defaultFor(HairStyleSlot.BASE).copy();

	public final Vector3f root = new Vector3f();
	public final Quaternionf baseRotation = new Quaternionf();

	public Quaternionf[] relativeRotations = new Quaternionf[0];
	public Vector3f[] localOffsets = new Vector3f[0];
	public Quaternionf[] restRotations = new Quaternionf[0];
	public Vector3f[] restStarts = new Vector3f[0];
	public final Vector3f restTip = new Vector3f();

	public float[] lengths = new float[0];
	public float[] widths = new float[0];
	public float[] depths = new float[0];
	public float[] params = new float[0];

	public int strandColorFrom = HairColors.INHERIT;
	public int strandColorTo = HairColors.INHERIT;
	public int tipColorFrom = HairColors.INHERIT;
	public int tipColorTo = HairColors.INHERIT;
	public int[] segmentColorsFrom = new int[0];
	public int[] segmentColorsTo = new int[0];
	public float colorFactor;

	public void ensureCapacity(int count) {
		if (lengths.length >= count) return;
		int capacity = Math.max(count, 4);
		relativeRotations = grow(relativeRotations, capacity);
		restRotations = grow(restRotations, capacity);
		localOffsets = grow(localOffsets, capacity);
		restStarts = grow(restStarts, capacity);
		lengths = new float[capacity];
		widths = new float[capacity];
		depths = new float[capacity];
		params = new float[capacity];
		segmentColorsFrom = new int[capacity];
		segmentColorsTo = new int[capacity];
	}

	public Vector3f restEnd(int segment, Vector3f out) {
		return restRotations[segment].transform(out.set(0.0f, lengths[segment], 0.0f)).add(restStarts[segment]);
	}

	public int resolveColor(int segment, int globalFrom, int globalTo, boolean forceGlobalFrom, boolean forceGlobalTo) {
		int from = forceGlobalFrom ? globalFrom : resolveSide(segmentColorsFrom[segment], strandColorFrom, tipColorFrom, globalFrom, params[segment]);
		int to = forceGlobalTo ? globalTo : resolveSide(segmentColorsTo[segment], strandColorTo, tipColorTo, globalTo, params[segment]);
		return HairColors.lerp(from, to, colorFactor);
	}

	private static int resolveSide(int segmentColor, int strandColor, int tipColor, int global, float param) {
		if (segmentColor != HairColors.INHERIT) return segmentColor;
		int base = strandColor != HairColors.INHERIT ? strandColor : global;
		int tip = tipColor != HairColors.INHERIT ? tipColor : base;
		return HairColors.lerp(base, tip, param);
	}

	private static Quaternionf[] grow(Quaternionf[] array, int capacity) {
		Quaternionf[] grown = new Quaternionf[capacity];
		for (int i = 0; i < capacity; i++) grown[i] = i < array.length ? array[i] : new Quaternionf();
		return grown;
	}

	private static Vector3f[] grow(Vector3f[] array, int capacity) {
		Vector3f[] grown = new Vector3f[capacity];
		for (int i = 0; i < capacity; i++) grown[i] = i < array.length ? array[i] : new Vector3f();
		return grown;
	}
}
