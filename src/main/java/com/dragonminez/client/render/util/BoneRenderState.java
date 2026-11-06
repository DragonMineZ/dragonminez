package com.dragonminez.client.render.util;

import software.bernie.geckolib.cache.object.GeoBone;

public final class BoneRenderState {
	private final GeoBone bone;
	private final float posX, posY, posZ;
	private final float rotX, rotY, rotZ;
	private final float scaleX, scaleY, scaleZ;
	private final boolean posChanged, rotChanged, scaleChanged;

	private BoneRenderState(GeoBone bone) {
		this.bone = bone;
		this.posX = bone.getPosX();
		this.posY = bone.getPosY();
		this.posZ = bone.getPosZ();
		this.rotX = bone.getRotX();
		this.rotY = bone.getRotY();
		this.rotZ = bone.getRotZ();
		this.scaleX = bone.getScaleX();
		this.scaleY = bone.getScaleY();
		this.scaleZ = bone.getScaleZ();
		this.posChanged = bone.hasPositionChanged();
		this.rotChanged = bone.hasRotationChanged();
		this.scaleChanged = bone.hasScaleChanged();
	}

	public static BoneRenderState capture(GeoBone bone) {
		return new BoneRenderState(bone);
	}

	public void restore() {
		bone.setPosX(posX);
		bone.setPosY(posY);
		bone.setPosZ(posZ);
		bone.setRotX(rotX);
		bone.setRotY(rotY);
		bone.setRotZ(rotZ);
		bone.setScaleX(scaleX);
		bone.setScaleY(scaleY);
		bone.setScaleZ(scaleZ);
		bone.resetStateChanges();
		if (posChanged) bone.markPositionAsChanged();
		if (rotChanged) bone.markRotationAsChanged();
		if (scaleChanged) bone.markScaleAsChanged();
	}
}
