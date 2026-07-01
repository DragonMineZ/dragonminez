package com.dragonminez.client.gui.radial;

import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Status;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

public final class ModelFormPreview {
	private ModelFormPreview() {}

	public static void render(GuiGraphics graphics, int x, int y, int scale, float mouseX, float mouseY, FormPreview preview) {
		LivingEntity player = Minecraft.getInstance().player;
		if (player == null) return;

		PreviewSwap swap = applyPreview(preview);
		int adjustedScale = adjustedScale(scale);

		float xRotation = (float) Math.atan((double) ((float) y - mouseY) / 40.0F);
		float yRotation = (float) Math.atan((double) ((float) x - mouseX) / 40.0F);

		Quaternionf pose = (new Quaternionf()).rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(xRotation * 20.0F * ((float) Math.PI / 180F));
		pose.mul(cameraOrientation);

		float yBodyRotO = player.yBodyRot;
		float yRotO = player.getYRot();
		float xRotO = player.getXRot();
		float yHeadRotO = player.yHeadRotO;
		float yHeadRot = player.yHeadRot;

		player.yBodyRot = 180.0F + yRotation * 20.0F;
		player.setYRot(180.0F + yRotation * 40.0F);
		player.setXRot(-xRotation * 20.0F);
		player.yHeadRot = player.getYRot();
		player.yHeadRotO = player.getYRot();

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 150.0D);
		DMZSkinLayer.PREVIEW_MODE = swap.applied();
		try {
			InventoryScreen.renderEntityInInventory(graphics, x, y, adjustedScale, pose, cameraOrientation, player);
		} finally {
			DMZSkinLayer.PREVIEW_MODE = false;
		}
		graphics.pose().popPose();

		player.yBodyRot = yBodyRotO;
		player.setYRot(yRotO);
		player.setXRot(xRotO);
		player.yHeadRotO = yHeadRotO;
		player.yHeadRot = yHeadRot;

		restorePreview(swap);
	}

	private static int adjustedScale(int baseScale) {
		var player = Minecraft.getInstance().player;
		if (player == null) return baseScale;

		final float[] inverseScale = {1.0f};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			Float[] resolved = stats.getCharacter().getResolvedModelScaling();
			float currentScale = (resolved[0] + resolved[1]) / 2.0f;
			if (currentScale > 1.0f) inverseScale[0] = 0.9375f / currentScale;
		});
		return (int) (baseScale * inverseScale[0]);
	}

	private record PreviewSwap(Character character, Status status, String formGroup, String form, String stackGroup, String stackForm, boolean androidUpgraded, boolean androidOverride, boolean applied) { }

	private static PreviewSwap applyPreview(FormPreview preview) {
		var player = Minecraft.getInstance().player;
		if (player == null || preview == null) return empty();

		var cap = StatsProvider.get(StatsCapability.INSTANCE, player).resolve();
		if (cap.isEmpty()) return empty();

		Character character = cap.get().getCharacter();
		Status status = cap.get().getStatus();

		String formGroup = character.getActiveFormGroup();
		String form = character.getActiveForm();
		String stackGroup = character.getActiveStackFormGroup();
		String stackForm = character.getActiveStackForm();
		boolean androidUpgraded = status.isAndroidUpgraded();

		character.clearActiveForm();
		character.clearActiveStackForm();

		boolean stack = ConfigManager.getStackFormGroup(preview.group()) != null;
		if (stack) character.setActiveStackForm(preview.group(), preview.form());
		else character.setActiveForm(preview.group(), preview.form());

		boolean androidOverride = false;
		if ("androidforms".equals(preview.group()) && !androidUpgraded) {
			status.setAndroidUpgraded(true);
			androidOverride = true;
		}

		return new PreviewSwap(character, status, formGroup, form, stackGroup, stackForm, androidUpgraded, androidOverride, true);
	}

	private static void restorePreview(PreviewSwap swap) {
		if (!swap.applied() || swap.character() == null) return;
		swap.character().clearActiveForm();
		swap.character().clearActiveStackForm();
		swap.character().setActiveForm(swap.formGroup(), swap.form());
		swap.character().setActiveStackForm(swap.stackGroup(), swap.stackForm());
		if (swap.androidOverride() && swap.status() != null) swap.status().setAndroidUpgraded(swap.androidUpgraded());
	}

	private static PreviewSwap empty() {
		return new PreviewSwap(null, null, null, null, null, null, false, false, false);
	}
}
