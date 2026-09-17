package com.dragonminez.common.hair;

public final class HairStrandSizing {
	public static final float MIN_SEGMENT_LENGTH = 1.0f;
	public static final float MAX_SEGMENT_LENGTH = 4.0f;

	private HairStrandSizing() {}

	public static void resizeLength(HairStrand strand, float length, int maxSegments, float maxLength) {
		int segments = Math.max(1, strand.getSegments());
		float clamped = HairMath.clamp(length, MIN_SEGMENT_LENGTH, maxLength);
		while (clamped / segments > MAX_SEGMENT_LENGTH && segments < maxSegments) segments++;
		while (clamped / segments < MIN_SEGMENT_LENGTH && segments > 1) segments--;
		clamped = HairMath.clamp(clamped, segments * MIN_SEGMENT_LENGTH, Math.min(maxLength, segments * MAX_SEGMENT_LENGTH));
		strand.setSegments(segments);
		strand.setLength(clamped);
	}

	public static void resizeSegments(HairStrand strand, int segments, int maxSegments, float maxLength) {
		int count = Math.max(1, Math.min(maxSegments, segments));
		strand.setSegments(count);
		strand.setLength(HairMath.clamp(strand.getLength(), count * MIN_SEGMENT_LENGTH, Math.min(maxLength, count * MAX_SEGMENT_LENGTH)));
	}

	public static boolean addSegment(HairStrand strand, int maxSegments, float maxLength) {
		int segments = strand.getSegments();
		if (segments >= maxSegments) return false;
		float average = segments > 0 ? strand.getLength() / segments : HairStrand.DEFAULT_LENGTH / HairStrand.DEFAULT_SEGMENTS;
		float length = Math.min(maxLength, strand.getLength() + average);
		if (length / (segments + 1) < MIN_SEGMENT_LENGTH) return false;
		strand.setSegments(segments + 1);
		strand.setLength(length);
		return true;
	}

	public static boolean removeSegment(HairStrand strand) {
		int segments = strand.getSegments();
		if (segments <= 1) return false;
		float average = strand.getLength() / segments;
		strand.setSegments(segments - 1);
		strand.setLength(Math.max(MIN_SEGMENT_LENGTH, strand.getLength() - average));
		strand.removeOverride(segments - 1);
		return true;
	}
}
