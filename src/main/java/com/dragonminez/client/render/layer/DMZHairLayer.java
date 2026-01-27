package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.render.hair.HairRenderer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.Character;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DMZHairLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {

    public DMZHairLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        // No renderizar si tiene un casco que no sea pothala, es invisible o spectator
        if (animatable.isInvisible() || animatable.isSpectator()) return;
		if (FirstPersonManager.shouldRenderFirstPerson(animatable)) return;

        var headItem = animatable.getItemBySlot(EquipmentSlot.HEAD);
        if (!headItem.isEmpty() && !headItem.getItem().getDescriptionId().contains("pothala") && !headItem.getItem().getDescriptionId().contains("scouter")) return;

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
        var stats = statsCap.orElse(new StatsData(animatable));
        Character character = stats.getCharacter();
        if (!HairManager.canUseHair(character)) return;

        CustomHair effectiveHair = HairManager.getEffectiveHair(character);
        if (effectiveHair == null || effectiveHair.isEmpty()) return;

		CustomHair hairFrom = character.getHairBase();
		CustomHair hairTo = character.getHairBase();
		float factor = 0.0f;

		if (character.hasActiveForm()) {
			hairFrom = getHairForForm(character, character.getActiveFormGroup(), character.getActiveForm());
			hairTo = hairFrom;
			factor = 1.0f;
		} else if (stats.getStatus().isActionCharging() && stats.getStatus().getSelectedAction() == ActionMode.FORM) {
			hairFrom = character.getHairBase();
			String targetGroup = character.getSelectedFormGroup();
			var nextForm = TransformationsHelper.getNextAvailableForm(stats);
			if (nextForm != null) {
				hairTo = getHairForForm(character, targetGroup, nextForm.getName());
			}
			factor = stats.getResources().getActionCharge() / 100.0f;
		}

        Optional<GeoBone> headBoneOpt = model.getBone("head");
        if (headBoneOpt.isEmpty()) return;

        GeoBone headBone = headBoneOpt.get();

        poseStack.pushPose();

        float bodyYaw = Mth.lerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));

        List<GeoBone> boneChain = new ArrayList<>();
        CoreGeoBone currentBone = headBone;
        while (currentBone != null) {
            boneChain.add((GeoBone) currentBone);
            currentBone = currentBone.getParent();
        }

        for (int i = boneChain.size() - 1; i >= 0; i--) {
            GeoBone bone = boneChain.get(i);

            poseStack.translate(-bone.getPosX() / 16f, bone.getPosY() / 16f, bone.getPosZ() / 16f);

            RenderUtils.translateToPivotPoint(poseStack, bone);

            if (bone.getRotZ() != 0) poseStack.mulPose(Axis.ZP.rotation(bone.getRotZ()));
            if (bone.getRotY() != 0) poseStack.mulPose(Axis.YP.rotation(bone.getRotY()));
            if (bone.getRotX() != 0) poseStack.mulPose(Axis.XP.rotation(bone.getRotX()));

            RenderUtils.scaleMatrixForBone(poseStack, bone);
            RenderUtils.translateAwayFromPivotPoint(poseStack, bone);
        }

        RenderUtils.translateToPivotPoint(poseStack, headBone);
		HairRenderer.render(poseStack, bufferSource, hairFrom, hairTo, factor, character, stats, animatable, character.getHairColor(), partialTick, packedLight, packedOverlay);

		poseStack.popPose();
	}

	private CustomHair getHairForForm(Character character, String group, String formName) {
		FormConfig config = ConfigManager.getFormGroup(character.getRaceName(), group);
		if (config != null) {
			var formData = config.getForm(formName);
			if (formData != null && formData.hasHairCodeOverride()) {
				CustomHair override = HairManager.fromCode(formData.getHairCode());
				if (override != null) return override;
			}
		}

		String lowerForm = formName.toLowerCase();

		if (lowerForm.contains("ssj3") || lowerForm.contains("3")) {
			return character.getHairSSJ3();
		} else if (lowerForm.contains("super") || lowerForm.contains("rose") || lowerForm.contains("blue") || lowerForm.contains("ssj")) {
			return character.getHairSSJ();
		}

		return character.getHairBase();
	}
}