package com.dragonminez.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.model.KiBladeModel;
import com.dragonminez.client.model.KiScytheModel;
import com.dragonminez.client.model.KiTridentModel;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.util.AuraRenderQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.NonNull;

@OnlyIn(Dist.CLIENT)
public class DMZRenderHand extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public static final ResourceLocation KI_WEAPON_TEX = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/kiweapons.png");

	public static final KiScytheModel KI_SCYTHE_MODEL = new KiScytheModel(KiScytheModel.createBodyLayer().bakeRoot());
	public static final KiBladeModel KI_BLADE_MODEL = new KiBladeModel(KiBladeModel.createBodyLayer().bakeRoot());
	public static final KiTridentModel KI_TRIDENT_MODEL = new KiTridentModel(KiTridentModel.createBodyLayer().bakeRoot());

	public DMZRenderHand(EntityRendererProvider.Context pContext, PlayerModel<AbstractClientPlayer> pModel) {
		super(pContext, new PlayerModel(pContext.bakeLayer(ModelLayers.PLAYER), false), 0.4f);
	}

	public void renderRightHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, pPlayer).orElse(new StatsData(pPlayer));

		if (stats.getStatus().isBlocking()) {
			pPoseStack.pushPose();
			applyBlockingTransform(pPoseStack, 1.0F);
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.rightArm, this.model.rightSleeve);
			pPoseStack.popPose();

			pPoseStack.pushPose();
			applyBlockingTransform(pPoseStack, -1.0F);
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.leftArm, this.model.leftSleeve);
			pPoseStack.popPose();
		} else {
			pPoseStack.pushPose();
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.rightArm, this.model.rightSleeve);
			pPoseStack.popPose();
		}

		this.renderKiWeapon(pPoseStack, pBuffer, pCombinedLight, pPlayer, stats, HumanoidArm.RIGHT);

		if (stats.getStatus().isAuraActive() && !stats.getStatus().isAndroidUpgraded())
			queueFirstPersonAura(pPlayer, pPoseStack, pCombinedLight);
	}

	public void renderLeftHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer) {
		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, pPlayer);
		var stats = statsCap.orElse(new StatsData(pPlayer));

		if (stats.getStatus().isBlocking()) {
			pPoseStack.pushPose();
			applyBlockingTransform(pPoseStack, 1.0F);
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.rightArm, this.model.rightSleeve);
			pPoseStack.popPose();

			pPoseStack.pushPose();
			applyBlockingTransform(pPoseStack, -1.0F);
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.leftArm, this.model.leftSleeve);
			pPoseStack.popPose();
		} else {
			pPoseStack.pushPose();
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.leftArm, this.model.leftSleeve);
			pPoseStack.popPose();
		}

		this.renderKiWeapon(pPoseStack, pBuffer, pCombinedLight, pPlayer, stats, HumanoidArm.LEFT);

	}

	private void applyBlockingTransform(PoseStack stack, float side) {
		stack.translate(side * -0.25F, -0.15F, -0.4F);
		stack.mulPose(Axis.XP.rotationDegrees(-20.0F));
		stack.mulPose(Axis.YP.rotationDegrees(100.0F));
		stack.mulPose(Axis.ZP.rotationDegrees(side * 330.0F));
	}

	private void renderHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer, ModelPart pRendererArm, ModelPart pRendererArmwear) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, pPlayer).orElse(new StatsData(pPlayer));
		int kaiokenPhase = TransformationsHelper.getKaiokenPhase(stats);

		this.model.attackTime = 0.0F;
		this.model.crouching = false;
		this.model.swimAmount = 0.0F;
		this.model.setupAnim(pPlayer, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		pRendererArm.xRot = 0.0F;

		String raceName = stats.getCharacter().getRaceName().toLowerCase();
		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
		boolean forceVanilla = (raceConfig != null && raceConfig.getUseVanillaSkin());

		java.util.function.BiConsumer<ResourceLocation, float[]> layerConsumer = (texture, color) -> {
			float[] finalColor = applyKaiokenTint(color, kaiokenPhase);
			renderPart(pPoseStack, pBuffer, pCombinedLight, pRendererArm, texture, finalColor);
		};

		if (forceVanilla) {
			float[] skinTint = applyKaiokenTint(new float[]{1.0f, 1.0f, 1.0f}, kaiokenPhase);
			renderPart(pPoseStack, pBuffer, pCombinedLight, pRendererArm, pPlayer.getSkinTextureLocation(), skinTint);
		} else {
			float pt = Minecraft.getInstance().getFrameTime();
			DMZSkinLayer.gatherBodyLayers(pPlayer, stats, pt, layerConsumer);
			DMZSkinLayer.gatherAndroidLayers(pPlayer, stats, pt, layerConsumer);
			DMZSkinLayer.gatherTattooLayers(pPlayer, stats, pt, layerConsumer);
		}

		renderDbzArmor(pPoseStack, pBuffer, pCombinedLight, pPlayer, pRendererArm);
	}

	private void renderKiWeapon(PoseStack ps, MultiBufferSource buffer, int light, AbstractClientPlayer player, StatsData stats, HumanoidArm arm) {
		if (!stats.getSkills().isSkillActive("kimanipulation")) return;
		String type = stats.getStatus().getKiWeaponType();
		if (type == null || type.equalsIgnoreCase("none")) return;
		float[] color = getKiColor(stats);
		boolean isRight = arm == HumanoidArm.RIGHT;

		ps.pushPose();
		switch (type.toLowerCase()) {
			case "blade" -> {
				KI_BLADE_MODEL.rightArm.copyFrom(isRight ? this.model.rightArm : this.model.leftArm);
				ps.translate(isRight ? -0.02D : 0.15D, 0.1D, -0.1D);
				ps.mulPose(Axis.XP.rotationDegrees(5.0F));
				renderKiPart(ps, buffer, light, KI_BLADE_MODEL.right_arm, color);
			}
			case "scythe" -> {
				KI_SCYTHE_MODEL.rightArm.copyFrom(isRight ? this.model.rightArm : this.model.leftArm);
				ps.translate(isRight ? -0.06D : 0.65D, 0.1D, isRight ? -0.2D : 0.5D);
				ps.mulPose(Axis.YP.rotationDegrees(isRight ? 15.0F : -15.0F));
				renderKiPart(ps, buffer, light, KI_SCYTHE_MODEL.scythe_right, color);
			}
			case "clawlance" -> {
				KI_TRIDENT_MODEL.rightArm.copyFrom(isRight ? this.model.rightArm : this.model.leftArm);
				ps.translate(isRight ? -0.05D : 0.8D, isRight ? 0.0D : 0, isRight ? -0.3D : 0.5);
				ps.mulPose(Axis.XP.rotationDegrees(isRight ? 25.0F : -05.0F));
				renderKiPart(ps, buffer, light, KI_TRIDENT_MODEL.trident_right, color);
			}
		}
		ps.popPose();
	}

	private void renderDbzArmor(PoseStack ps, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer player, ModelPart pRendererArm) {
		ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
		if (chestStack.isEmpty()) return;

		String itemId = null;

		if (chestStack.getItem() instanceof DbzArmorItem armorItem) {
			itemId = armorItem.getItemId();
		} else if (chestStack.getItem() instanceof DbzArmorCapeItem capeItem) {
			itemId = capeItem.getItemId();
		}

		if (itemId != null) {
			String texturePath = "textures/armor/" + itemId + "_layer1.png";
			ResourceLocation armorResource = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, texturePath);

			ps.pushPose();

			boolean isRightArm = (pRendererArm == this.model.rightArm);

			float armorInflation = 1.05F;
			ps.scale(armorInflation, armorInflation, armorInflation);

			ps.translate(isRightArm ? 0.02D : -0.01, 0.02D, 0.0D);

			renderPart(ps, pBuffer, pCombinedLight, pRendererArm, armorResource, new float[]{1.0F, 1.0F, 1.0F});

			ps.popPose();

		}
	}

	private void renderPart(PoseStack stack, MultiBufferSource buffer, int light, ModelPart part, ResourceLocation texture, float[] rgb) {
		VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(texture));
		part.render(stack, vc, light, OverlayTexture.NO_OVERLAY, rgb[0], rgb[1], rgb[2], 1.0F);
	}

	private ResourceLocation loc(String path) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
	}

	private static HumanoidModel.ArmPose getArmPose(AbstractClientPlayer pPlayer, InteractionHand pHand) {
		ItemStack itemstack = pPlayer.getItemInHand(pHand);
		if (itemstack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
		else {
			if (pPlayer.getUsedItemHand() == pHand && pPlayer.getUseItemRemainingTicks() > 0) {
				UseAnim useanim = itemstack.getUseAnimation();
				if (useanim == UseAnim.BLOCK) return HumanoidModel.ArmPose.BLOCK;
				if (useanim == UseAnim.BOW) return HumanoidModel.ArmPose.BOW_AND_ARROW;
				if (useanim == UseAnim.SPEAR) return HumanoidModel.ArmPose.THROW_SPEAR;
				if (useanim == UseAnim.CROSSBOW && pHand == pPlayer.getUsedItemHand())
					return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
				if (useanim == UseAnim.SPYGLASS) return HumanoidModel.ArmPose.SPYGLASS;
				if (useanim == UseAnim.TOOT_HORN) return HumanoidModel.ArmPose.TOOT_HORN;
				if (useanim == UseAnim.BRUSH) return HumanoidModel.ArmPose.BRUSH;
			} else if (!pPlayer.swinging && itemstack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack)) {
				return HumanoidModel.ArmPose.CROSSBOW_HOLD;
			}
			HumanoidModel.ArmPose forgeArmPose = IClientItemExtensions.of(itemstack).getArmPose(pPlayer, pHand, itemstack);
			return forgeArmPose != null ? forgeArmPose : HumanoidModel.ArmPose.ITEM;
		}
	}

	protected void setupRotations(AbstractClientPlayer pEntityLiving, @NonNull PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
		float f = pEntityLiving.getSwimAmount(pPartialTicks);
		if (pEntityLiving.isFallFlying()) {
			super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
			float f1 = (float) pEntityLiving.getFallFlyingTicks() + pPartialTicks;
			float f2 = Mth.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
			if (!pEntityLiving.isAutoSpinAttack())
				pPoseStack.mulPose(Axis.XP.rotationDegrees(f2 * (-90.0F - pEntityLiving.getXRot())));
			Vec3 vec3 = pEntityLiving.getViewVector(pPartialTicks);
			Vec3 vec31 = pEntityLiving.getDeltaMovementLerped(pPartialTicks);
			double d0 = vec31.horizontalDistanceSqr();
			double d1 = vec3.horizontalDistanceSqr();
			if (d0 > 0.0D && d1 > 0.0D) {
				double d2 = (vec31.x * vec3.x + vec31.z * vec3.z) / Math.sqrt(d0 * d1);
				double d3 = vec31.x * vec3.z - vec31.z * vec3.x;
				pPoseStack.mulPose(Axis.YP.rotation((float) (Math.signum(d3) * Math.acos(d2))));
			}
		} else if (f > 0.0F) {
			super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
			float f3 = pEntityLiving.isInWater() || pEntityLiving.isInFluidType((fluidType, height) -> pEntityLiving.canSwimInFluidType(fluidType)) ? -90.0F - pEntityLiving.getXRot() : -90.0F;
			float f4 = Mth.lerp(f, 0.0F, f3);
			pPoseStack.mulPose(Axis.XP.rotationDegrees(f4));
			if (pEntityLiving.isVisuallySwimming()) pPoseStack.translate(0.0F, -1.0F, 0.3F);
		} else super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
	}

	@Override
	public @NonNull ResourceLocation getTextureLocation(AbstractClientPlayer pEntity) {
		return pEntity.getSkinTextureLocation();
	}

	private void renderKiPart(PoseStack ps, MultiBufferSource buffer, int light, ModelPart part, float[] color) {
		VertexConsumer vc = buffer.getBuffer(ModRenderTypes.kiblast(KI_WEAPON_TEX));
		part.render(ps, vc, light, OverlayTexture.NO_OVERLAY, color[0], color[1], color[2], 0.85F);
	}

	private float[] getKiColor(StatsData stats) {
		var character = stats.getCharacter();
		String kiHex = character.getAuraColor();
		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			String formColor = character.getActiveFormData().getAuraColor();
			if (formColor != null && !formColor.isEmpty()) kiHex = formColor;
		}
		return ColorUtils.hexToRgb(kiHex);
	}

	private float[] applyKaiokenTint(float[] rgb, int phase) {
		if (phase <= 0) return rgb;

		float intensity = Math.min(0.6f, phase * 0.1f);

		float newR = rgb[0] * (1.0f - intensity) + (intensity);
		float newG = rgb[1] * (1.0f - intensity);
		float newB = rgb[2] * (1.0f - intensity);

		return new float[]{newR, newG, newB};
	}

	private void queueFirstPersonAura(AbstractClientPlayer player, PoseStack poseStack, int packedLight) {
		float partialTick = Minecraft.getInstance().getFrameTime();
		AuraRenderQueue.addFirstPersonAura(player, poseStack, partialTick, packedLight);
	}
}
