package com.dragonminez.client.gui.hair;

import com.dragonminez.client.render.hair.HairPickRecorder;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairMath;
import com.dragonminez.common.hair.HairPoseBuilder;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.hair.HairStrandSizing;
import com.dragonminez.common.hair.StrandPose;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class HairViewportTool {
	enum Mode {
		GROW,
		ROTATE,
		CURVE
	}

	private static final float CURVE_DEGREES_PER_PIXEL = 1.5f;
	private static final float ROTATION_PROBE = 0.2f;
	private static final float MIN_SCREEN_LENGTH = 2.0f;

	private final HairEditorState state;
	private final Matrix4f headMatrix = new Matrix4f();
	private final Matrix4f inverseHead = new Matrix4f();
	private final StrandPose originalPose = new StrandPose();
	private final Vector3f rootLocal = new Vector3f();
	private final Vector3f tipLocal = new Vector3f();
	private final Vector3f rootGui = new Vector3f();
	private final Vector3f tipGui = new Vector3f();
	private final Vector3f viewAxis = new Vector3f();
	private final Quaternionf baseRotation = new Quaternionf();
	private HairStrand original;
	private CustomHair.HairFace face;
	private int index;
	private Mode mode;
	private double startX;
	private double startY;
	private float guiPixelsPerHairPixel;
	private float rotationSign = 1.0f;
	private boolean active;
	private boolean atLimit;

	HairViewportTool(HairEditorState state) {
		this.state = state;
	}

	boolean isActive() {
		return active;
	}

	Mode mode() {
		return mode;
	}

	boolean begin(HairPickRecorder recorder, CustomHair.HairFace strandFace, int strandIndex, Mode toolMode, double mouseX, double mouseY) {
		if (!recorder.hasFrame()) return false;
		HairStrand strand = state.style().getStrand(strandFace, strandIndex);
		if (strand == null || !strand.isVisible()) return false;

		state.recordStep();
		face = strandFace;
		index = strandIndex;
		mode = toolMode;
		original = strand.copy();
		startX = mouseX;
		startY = mouseY;

		headMatrix.set(recorder.getHeadMatrix());
		headMatrix.invert(inverseHead);
		HairPoseBuilder.buildGeometry(original, face, index, originalPose);
		rootLocal.set(originalPose.root);
		tipLocal.set(originalPose.restTip);
		headMatrix.transformPosition(rootLocal, rootGui);
		headMatrix.transformPosition(tipLocal, tipGui);
		inverseHead.transformDirection(viewAxis.set(0.0f, 0.0f, -1.0f)).normalize();
		baseRotation.identity()
				.rotateX(original.getRotationX() * HairMath.DEG_TO_RAD)
				.rotateY(original.getRotationY() * HairMath.DEG_TO_RAD)
				.rotateZ(original.getRotationZ() * HairMath.DEG_TO_RAD);

		Vector3f scale = headMatrix.getScale(new Vector3f());
		guiPixelsPerHairPixel = Math.max(1.0e-3f, (Math.abs(scale.x) + Math.abs(scale.y)) * 0.5f * HairMath.PIXEL);
		if (mode == Mode.ROTATE) rotationSign = resolveRotationSign();
		atLimit = false;
		active = true;
		return true;
	}

	void drag(double mouseX, double mouseY) {
		if (!active) return;
		HairStrand strand = state.style().getStrand(face, index);
		if (strand == null) return;
		strand.copyFrom(original);
		switch (mode) {
			case GROW -> grow(strand, mouseX, mouseY);
			case ROTATE -> rotate(strand, mouseX, mouseY);
			case CURVE -> curve(strand, mouseX, mouseY);
		}
		state.propagateMirror();
		state.style().markChanged();
	}

	void finish() {
		active = false;
	}

	void cancel() {
		if (!active) return;
		HairStrand strand = state.style().getStrand(face, index);
		if (strand != null) {
			strand.copyFrom(original);
			state.propagateMirror();
			state.style().markChanged();
		}
		active = false;
	}

	private void grow(HairStrand strand, double mouseX, double mouseY) {
		float dirX = tipGui.x - rootGui.x;
		float dirY = tipGui.y - rootGui.y;
		float screenLength = (float) Math.sqrt(dirX * dirX + dirY * dirY);
		if (screenLength < MIN_SCREEN_LENGTH) {
			dirX = 0.0f;
			dirY = -1.0f;
		} else {
			dirX /= screenLength;
			dirY /= screenLength;
		}
		float projected = (float) ((mouseX - startX) * dirX + (mouseY - startY) * dirY);
		float length = original.getLength() + projected / guiPixelsPerHairPixel;
		float maxLength = state.limits().getMaxLength(state.slot());
		HairStrandSizing.resizeLength(strand, length, state.limits().getMaxSegments(), maxLength);
		boolean limited = length >= maxLength || length <= HairStrandSizing.MIN_SEGMENT_LENGTH;
		if (limited && !atLimit) HairEditorSounds.limit();
		atLimit = limited;
	}

	private void rotate(HairStrand strand, double mouseX, double mouseY) {
		double startAngle = Math.atan2(startY - rootGui.y, startX - rootGui.x);
		double currentAngle = Math.atan2(mouseY - rootGui.y, mouseX - rootGui.x);
		float angle = (float) (currentAngle - startAngle) * rotationSign;
		Quaternionf rotated = new Quaternionf().rotationAxis(angle, viewAxis).mul(baseRotation);
		Vector3f euler = rotated.getEulerAnglesXYZ(new Vector3f());
		strand.setRotation(euler.x * HairMath.RAD_TO_DEG, euler.y * HairMath.RAD_TO_DEG, euler.z * HairMath.RAD_TO_DEG);
	}

	private void curve(HairStrand strand, double mouseX, double mouseY) {
		Vector3f drag = inverseHead.transformDirection(new Vector3f((float) (mouseX - startX), (float) (mouseY - startY), 0.0f));
		float pixels = (float) Math.hypot(mouseX - startX, mouseY - startY);
		if (pixels < 1.0e-3f || drag.lengthSquared() < 1.0e-12f) return;
		drag.normalize();
		new Quaternionf(baseRotation).conjugate().transform(drag);
		float degrees = pixels * CURVE_DEGREES_PER_PIXEL;
		strand.setBend(original.getBendX() + drag.z * degrees, original.getBendY(), original.getBendZ() - drag.x * degrees);
	}

	private float resolveRotationSign() {
		Vector3f offset = new Vector3f(tipLocal).sub(rootLocal);
		double startAngle = Math.atan2(tipGui.y - rootGui.y, tipGui.x - rootGui.x);
		Vector3f probe = new Quaternionf().rotationAxis(ROTATION_PROBE, viewAxis).transform(new Vector3f(offset)).add(rootLocal);
		Vector3f probeGui = headMatrix.transformPosition(probe, new Vector3f());
		double probeAngle = Math.atan2(probeGui.y - rootGui.y, probeGui.x - rootGui.x);
		double delta = Math.atan2(Math.sin(probeAngle - startAngle), Math.cos(probeAngle - startAngle));
		return delta >= 0.0 ? 1.0f : -1.0f;
	}
}
