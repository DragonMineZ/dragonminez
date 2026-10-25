package com.dragonminez.common.util;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairMath;
import com.dragonminez.common.hair.HairSegmentOverride;
import com.dragonminez.common.hair.HairStrand;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FusionHair {
	private static final float MIN_PARTNER_WEIGHT = 0.15f;
	private static final float MAX_PARTNER_WEIGHT = 0.6f;
	private static final float PARTNER_WEIGHT_CURVE = 1.5f;
	private static final float FILL_THRESHOLD = 0.45f;
	private static final float PARTNER_COLOR_SHARE = 0.45f;
	private static final float SHORTER_LENGTH_INFLUENCE = 0.35f;
	private static final float NEIGHBOR_INFLUENCE = 0.5f;
	private static final float BANG_WEIGHT = 0.65f;
	private static final float FILL_WEIGHT = 0.2f;
	private static final float DIAGONAL_NEIGHBOR_WEIGHT = 0.5f;
	private static final long WEIGHT_SALT = 0x5DEECE66DL;
	private static final long COLOR_SALT = 0x2545F4914F6CDD1DL;

	private record Slot(CustomHair.HairFace face, int index, HairStrand strand, HairStrand ownSource, HairStrand otherSource) {}

	private FusionHair() {}

	public static CustomHair fuse(CustomHair leader, CustomHair partner, String partnerColor, long seed) {
		CustomHair fused = new CustomHair();
		fused.setName("Fusion");
		fused.setGlobalColor(leader.getGlobalColor());
		List<Slot> slots = new ArrayList<>();
		Map<Integer, Integer> patchSizes = new LinkedHashMap<>();
		for (CustomHair.HairFace face : CustomHair.HairFace.values()) {
			for (int index = 0; index < face.maxStrands; index++) {
				Slot slot = fuseSlot(leader, partner, face, index, seed);
				if (slot == null) continue;
				slots.add(slot);
				patchSizes.merge(patchOf(face, index), 1, Integer::sum);
			}
		}

		Set<Integer> partnerPatches = partnerPatches(patchSizes, seed);
		for (Slot slot : slots) {
			if (partnerPatches.contains(patchOf(slot.face(), slot.index()))) paint(slot.strand(), slot.otherSource(), partnerColor);
			else paint(slot.strand(), slot.ownSource(), null);
			fused.setStrand(slot.face(), slot.index(), slot.strand());
		}
		return fused;
	}

	private static int patchOf(CustomHair.HairFace face, int index) {
		int patchRow = index / face.cols / 2;
		int patchColumn = index % face.cols / 2;
		int mirrorColumn = (face.cols - 1) / 2 - patchColumn;
		int id = face.ordinal() * 100 + patchRow * 10 + patchColumn;
		int mirrorId = CustomHair.mirrorFace(face).ordinal() * 100 + patchRow * 10 + mirrorColumn;
		return Math.min(id, mirrorId);
	}

	private static Set<Integer> partnerPatches(Map<Integer, Integer> patchSizes, long seed) {
		List<Integer> order = new ArrayList<>(patchSizes.keySet());
		order.sort(Comparator.comparingLong(patch -> mix(seed ^ COLOR_SALT ^ (patch * 0x9E3779B97F4A7C15L))));
		int total = 0;
		for (int size : patchSizes.values()) total += size;
		float target = total * PARTNER_COLOR_SHARE;
		Set<Integer> chosen = new HashSet<>();
		int painted = 0;
		for (int patch : order) {
			int size = patchSizes.get(patch);
			if (Math.abs(painted + size - target) < Math.abs(painted - target)) {
				chosen.add(patch);
				painted += size;
			}
		}
		return chosen;
	}

	private static Slot fuseSlot(CustomHair leader, CustomHair partner, CustomHair.HairFace face, int index, long seed) {
		HairStrand own = visible(leader.getStrand(face, index));
		HairStrand other = visible(partner.getStrand(face, index));
		float weight = HairMath.lerp((float) Math.pow(unit(face, index, seed ^ WEIGHT_SALT), PARTNER_WEIGHT_CURVE), MIN_PARTNER_WEIGHT, MAX_PARTNER_WEIGHT);

		if (own == null && other == null) return null;
		HairStrand ownSource = own != null ? own : neighborhood(leader, face, index);
		HairStrand otherSource = other != null ? other : neighborhood(partner, face, index);

		HairStrand fused;
		if (own != null && other != null) {
			fused = blend(own, other, weight, true);
		} else if (own != null) {
			fused = otherSource != null ? blend(own, otherSource, weight * NEIGHBOR_INFLUENCE, true) : own.copy();
		} else if (face == CustomHair.HairFace.FRONT) {
			fused = ownSource != null ? blend(ownSource, other, BANG_WEIGHT, false) : other.copy();
		} else {
			if (ownSource == null || weight < FILL_THRESHOLD) return null;
			fused = blend(ownSource, other, FILL_WEIGHT, false);
		}
		return new Slot(face, index, fused, ownSource, otherSource);
	}

	private static void paint(HairStrand target, HairStrand source, String fallbackColor) {
		if (source == null) {
			target.setColor(fallbackColor);
			target.setTipColor(null);
			return;
		}
		Samples samples = new Samples(source);
		int segments = Math.max(1, target.getSegments());
		for (int k = 0; k < segments; k++) {
			float param = segments > 1 ? (float) k / (segments - 1) : 0.0f;
			String color = samples.color[samples.nearest(param)];
			HairSegmentOverride override = target.getOverride(k);
			if (override == null && color == null) continue;
			if (override == null) override = target.getOrCreateOverride(k);
			override.setColor(color);
			if (override.isIdentity()) target.removeOverride(k);
		}
		target.setColor(source.getColor() != null ? source.getColor() : fallbackColor);
		target.setTipColor(source.getTipColor());
	}

	private static HairStrand visible(HairStrand strand) {
		return strand != null && strand.isVisible() ? strand : null;
	}

	private static float unit(CustomHair.HairFace face, int index, long seed) {
		CustomHair.HairFace mirrorFace = CustomHair.mirrorFace(face);
		int canonical = Math.min(face.strandId(index), mirrorFace.strandId(CustomHair.mirrorIndex(face, index)));
		long hash = mix(seed ^ (canonical * 0x9E3779B97F4A7C15L));
		return (hash >>> 40) / (float) (1L << 24);
	}

	private static long mix(long value) {
		value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
		value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
		return value ^ (value >>> 31);
	}

	private static HairStrand neighborhood(CustomHair style, CustomHair.HairFace face, int index) {
		int row = index / face.cols;
		int col = index % face.cols;
		HairStrand result = null;
		float total = 0.0f;
		for (int dr = -1; dr <= 1; dr++) {
			for (int dc = -1; dc <= 1; dc++) {
				if (dr == 0 && dc == 0) continue;
				int r = row + dr;
				int c = col + dc;
				if (r < 0 || c < 0 || r >= face.rows || c >= face.cols) continue;
				HairStrand neighbor = visible(style.getStrand(face, r * face.cols + c));
				if (neighbor == null) continue;
				float weight = dr != 0 && dc != 0 ? DIAGONAL_NEIGHBOR_WEIGHT : 1.0f;
				total += weight;
				result = result == null ? neighbor : blend(result, neighbor, weight / total, false);
			}
		}
		return result;
	}

	private static HairStrand blend(HairStrand a, HairStrand b, float t, boolean keepLength) {
		HairStrand shape = t < 0.5f ? a : b;
		int segments = Math.max(1, shape.getSegments());
		float lengthT = keepLength && b.getLength() < a.getLength() ? t * SHORTER_LENGTH_INFLUENCE : t;

		HairStrand out = new HairStrand();
		out.setSegments(segments);
		out.setLength(HairMath.lerp(lengthT, a.getLength(), b.getLength()));
		out.setLengthRatio((float) Math.exp(HairMath.lerp(t, (float) Math.log(a.getLengthRatio()), (float) Math.log(b.getLengthRatio()))));
		out.setWidth(HairMath.lerp(t, a.getWidth(), b.getWidth()));
		out.setDepth(HairMath.lerp(t, a.getDepth(), b.getDepth()));
		out.setTaper(HairMath.lerp(t, a.getTaper(), b.getTaper()));
		out.setTaperCurve(HairMath.lerp(t, a.getTaperCurve(), b.getTaperCurve()));
		out.setOffset(
				HairMath.lerp(t, a.getOffsetX(), b.getOffsetX()),
				HairMath.lerp(t, a.getOffsetY(), b.getOffsetY()),
				HairMath.lerp(t, a.getOffsetZ(), b.getOffsetZ()));
		Vector3f euler = rotationOf(a).slerp(rotationOf(b), t).getEulerAnglesXYZ(new Vector3f());
		out.setRotation(euler.x * HairMath.RAD_TO_DEG, euler.y * HairMath.RAD_TO_DEG, euler.z * HairMath.RAD_TO_DEG);
		out.setBend(
				HairMath.lerp(t, a.getBendX(), b.getBendX()),
				HairMath.lerp(t, a.getBendY(), b.getBendY()),
				HairMath.lerp(t, a.getBendZ(), b.getBendZ()));
		out.setTwist(HairMath.lerp(t, a.getTwist(), b.getTwist()));
		out.setJointStyle(shape.getJointStyle());

		Samples sa = new Samples(a);
		Samples sb = b == a ? sa : new Samples(b);
		Samples sc = t < 0.5f ? sa : sb;
		float lengthScale = 1.0f;
		float widthScale = 1.0f;
		float depthScale = 1.0f;
		for (int k = 0; k < segments; k++) {
			float param = segments > 1 ? (float) k / (segments - 1) : 0.0f;
			int ia = sa.nearest(param);
			int ib = sb.nearest(param);
			HairSegmentOverride override = out.getOrCreateOverride(k);
			override.setOffset(
					HairMath.lerp(t, sa.joint(sa.offsetX, k, param, segments), sb.joint(sb.offsetX, k, param, segments)),
					HairMath.lerp(t, sa.joint(sa.offsetY, k, param, segments), sb.joint(sb.offsetY, k, param, segments)),
					HairMath.lerp(t, sa.joint(sa.offsetZ, k, param, segments), sb.joint(sb.offsetZ, k, param, segments)));
			override.setRotation(
					HairMath.lerp(t, sa.joint(sa.rotationX, k, param, segments), sb.joint(sb.rotationX, k, param, segments)),
					HairMath.lerp(t, sa.joint(sa.rotationY, k, param, segments), sb.joint(sb.rotationY, k, param, segments)),
					HairMath.lerp(t, sa.joint(sa.rotationZ, k, param, segments), sb.joint(sb.rotationZ, k, param, segments)));
			override.setLengthScale(HairMath.lerp(lengthT, sa.length[ia], sb.length[ib]) / lengthScale);
			override.setWidthScale(HairMath.lerp(t, sa.width[ia], sb.width[ib]) / widthScale);
			override.setDepthScale(HairMath.lerp(t, sa.depth[ia], sb.depth[ib]) / depthScale);
			override.setColor(sc.color[sc.nearest(param)]);
			lengthScale *= override.getLengthScale();
			widthScale *= override.getWidthScale();
			depthScale *= override.getDepthScale();
			if (override.isIdentity()) out.removeOverride(k);
		}

		HairStrand colorSource = t < 0.5f ? a : b;
		out.setColor(colorSource.getColor());
		out.setTipColor(colorSource.getTipColor());
		return out;
	}

	private static Quaternionf rotationOf(HairStrand strand) {
		return new Quaternionf().rotationXYZ(
				strand.getRotationX() * HairMath.DEG_TO_RAD,
				strand.getRotationY() * HairMath.DEG_TO_RAD,
				strand.getRotationZ() * HairMath.DEG_TO_RAD);
	}

	private static final class Samples {
		private final int segments;
		private final float[] offsetX;
		private final float[] offsetY;
		private final float[] offsetZ;
		private final float[] rotationX;
		private final float[] rotationY;
		private final float[] rotationZ;
		private final float[] length;
		private final float[] width;
		private final float[] depth;
		private final String[] color;

		private Samples(HairStrand strand) {
			segments = Math.max(1, strand.getSegments());
			offsetX = new float[segments];
			offsetY = new float[segments];
			offsetZ = new float[segments];
			rotationX = new float[segments];
			rotationY = new float[segments];
			rotationZ = new float[segments];
			length = new float[segments];
			width = new float[segments];
			depth = new float[segments];
			color = new String[segments];
			float lengthScale = 1.0f;
			float widthScale = 1.0f;
			float depthScale = 1.0f;
			for (int k = 0; k < segments; k++) {
				HairSegmentOverride override = strand.getOverride(k);
				if (override != null) {
					offsetX[k] = override.getOffsetX();
					offsetY[k] = override.getOffsetY();
					offsetZ[k] = override.getOffsetZ();
					rotationX[k] = override.getRotationX();
					rotationY[k] = override.getRotationY();
					rotationZ[k] = override.getRotationZ();
					lengthScale *= override.getLengthScale();
					widthScale *= override.getWidthScale();
					depthScale *= override.getDepthScale();
					color[k] = override.getColor();
				}
				length[k] = lengthScale;
				width[k] = widthScale;
				depth[k] = depthScale;
			}
		}

		private int nearest(float param) {
			return Math.round(param * (segments - 1));
		}

		private float joint(float[] values, int k, float param, int targetSegments) {
			if (k == 0) return values[0];
			if (segments < 2) return 0.0f;
			int index = Math.max(1, Math.min(segments - 1, nearest(param)));
			return values[index] * (segments - 1.0f) / (targetSegments - 1.0f);
		}
	}
}
