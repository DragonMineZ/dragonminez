package com.dragonminez.common.hair;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;

public final class HairSanitizer {
	private HairSanitizer() {}

	public static int sanitize(CustomHair hair, HairStyleSlot slot, HairLimits limits) {
		if (hair == null) return 0;
		int adjustments = 0;
		int remainingSegments = limits.getMaxTotalSegments();
		StrandPose scratch = new StrandPose();

		for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
			HairStrand[] strands = hair.getStrands(face);
			for (int index = 0; index < strands.length; index++) {
				HairStrand strand = strands[index];
				if (!strand.isVisible()) continue;

				if (remainingSegments <= 0) {
					strand.setSegments(0);
					adjustments++;
					continue;
				}
				adjustments += sanitizeStrand(strand, face, index, slot, limits, scratch);
				if (strand.getSegments() > remainingSegments) {
					strand.setSegments(remainingSegments);
					adjustments++;
				}
				remainingSegments -= strand.getSegments();
			}
		}
		if (adjustments > 0) hair.markChanged();
		return adjustments;
	}

	public static int sanitizeAndLog(CustomHair hair, HairStyleSlot slot, String playerName) {
		int adjustments = sanitize(hair, slot, HairLimits.current());
		if (adjustments > 0) LogUtil.warn(Env.SERVER, "Clamped {} hair value(s) received from {} for style {}", adjustments, playerName, slot);
		return adjustments;
	}

	private static int sanitizeStrand(HairStrand strand, CustomHair.HairFace face, int index, HairStyleSlot slot, HairLimits limits, StrandPose scratch) {
		int adjustments = 0;
		if (strand.getSegments() > limits.getMaxSegments()) {
			strand.setSegments(limits.getMaxSegments());
			adjustments++;
		}

		float maxWidth = limits.getMaxWidth();
		if (strand.getWidth() > maxWidth) {
			strand.setWidth(maxWidth);
			adjustments++;
		}
		if (strand.getDepth() > maxWidth) {
			strand.setDepth(maxWidth);
			adjustments++;
		}

		float maxOffset = limits.getMaxRootOffset();
		float offsetX = HairMath.clamp(strand.getOffsetX(), -maxOffset, maxOffset);
		float offsetY = HairMath.clamp(strand.getOffsetY(), -maxOffset, maxOffset);
		float offsetZ = HairMath.clamp(strand.getOffsetZ(), -maxOffset, maxOffset);
		if (offsetX != strand.getOffsetX() || offsetY != strand.getOffsetY() || offsetZ != strand.getOffsetZ()) {
			strand.setOffset(offsetX, offsetY, offsetZ);
			adjustments++;
		}

		for (HairSegmentOverride override : strand.getOverrides()) {
			float widthScaleLimit = maxWidth / strand.getWidth();
			float depthScaleLimit = maxWidth / strand.getDepth();
			if (override.getWidthScale() > widthScaleLimit) {
				override.setWidthScale(widthScaleLimit);
				adjustments++;
			}
			if (override.getDepthScale() > depthScaleLimit) {
				override.setDepthScale(depthScaleLimit);
				adjustments++;
			}
		}

		HairPoseBuilder.buildGeometry(strand, face, index, scratch);
		float totalLength = 0.0f;
		for (int k = 0; k < scratch.segments; k++) totalLength += scratch.lengths[k] / HairMath.PIXEL;
		float maxLength = limits.getMaxLength(slot);
		if (totalLength > maxLength && totalLength > 0.0f) {
			strand.setLength(strand.getLength() * (maxLength / totalLength));
			adjustments++;
		}
		return adjustments;
	}
}
