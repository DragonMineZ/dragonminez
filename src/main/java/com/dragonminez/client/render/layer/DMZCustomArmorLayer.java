package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Set;

public class DMZCustomArmorLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {

	private static final ResourceLocation MAJIN_ARMOR_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/armor/armormajinfat.geo.json");
	private static final ResourceLocation MAJIN_SLIM_ARMOR_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/armor/armormajinslim.geo.json");
	private static final ResourceLocation OOZARU_ARMOR_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/armor/armoroozaru.geo.json");

	private static final Set<String> SLIM_SUPPORTED_MODELS = Set.of(
			"majin_evil", "majin_kid", "majin_super", "majin_ultra", "majin", "saiyan", "human", "saiyan_ssj4"
	);

	public DMZCustomArmorLayer(GeoRenderer<T> entityRendererIn) {
		super(entityRendererIn);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel playerModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (animatable.isSpectator()) return;

		ItemStack stack = animatable.getItemBySlot(EquipmentSlot.CHEST);
		if (CosmeticArmorCompat.isLoaded()) {
			ItemStack cosmeticStack = CosmeticArmorCompat.getCosmeticStack(animatable, EquipmentSlot.CHEST);
			if (cosmeticStack != null) stack = cosmeticStack;
		}
		if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) return;

		StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
		if (stats == null || stats.getCharacter().getArmored()) return;

		var character = stats.getCharacter();
		String raceName = character.getRaceName().toLowerCase();
		String gender = character.getGender().toLowerCase();
		String currentForm = character.getActiveForm();

		var raceConfig = ConfigManager.getRaceCharacter(raceName);
		String raceCustomModel = (raceConfig != null && raceConfig.getCustomModel() != null) ? raceConfig.getCustomModel().toLowerCase() : "";
		String formCustomModel = (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasCustomModel())
				? character.getActiveFormData().getCustomModel().toLowerCase() : "";

		String logicKey = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
		if (logicKey.isEmpty()) logicKey = raceName;

		boolean isVanilla = ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace().equals("minecraft");
		boolean isDbzArmor = stack.getItem() instanceof DbzArmorItem;
		boolean isPothala = stack.getItem().getDescriptionId().contains("pothala");

		if (isPothala || (!isVanilla && !isDbzArmor)) return;

		boolean shouldRender = false;
		boolean isSlimTarget = false;
		boolean isOozaruTarget = false;

		if (character.isOozaruCached() || logicKey.equals("oozaru")) {
			shouldRender = true;
			isOozaruTarget = true;
		} else if (logicKey.contains("buffed") || logicKey.contains("frostdemon_fp") || logicKey.contains("majin_ultra")
				|| logicKey.contains("namekian_orange") || logicKey.contains("bioandroid_ultra") || logicKey.contains("frostdemon_second")
				|| logicKey.contains("frostdemon_third") || logicKey.contains("frostdemon_fifth") || logicKey.contains("bioandroid_semi")) {
			if (isDbzArmor) shouldRender = true;
		} else if ((logicKey.equals("majin") && (gender.equals("male") || gender.equals("hombre"))) || (raceName.equals("majin") && (gender.equals("male") || gender.equals("hombre")))) {
			shouldRender = true;
		} else if (gender.equals("female") || gender.equals("mujer") || gender.equals("fem")) {
			boolean isKnownModel = SLIM_SUPPORTED_MODELS.contains(logicKey);
			boolean hasGenderConfig = (raceConfig != null && raceConfig.getHasGender());
			boolean hasVisibleBoobas = playerModel.getBone("boobas").map(b -> !b.isHidden()).orElse(false);

			if (isKnownModel || hasGenderConfig || hasVisibleBoobas) {
				shouldRender = true;
				isSlimTarget = true;
			}
		}

		if (!shouldRender) return;

		if (isDbzArmor) {
			ResourceLocation texture = getDbzArmorTexture((DbzArmorItem) stack.getItem(), stack);
			poseStack.pushPose();

			float translateY = isOozaruTarget ? -0.087f : -0.025f;
			float inflation = isOozaruTarget ? 1.021f : 1.02f;

			poseStack.translate(0, translateY, 0);

			playerModel.getBone("body").ifPresent(bodyBone -> {
				scaleBoneRecursively(bodyBone, inflation);

				GeoBone armorBody = getChild(bodyBone, "armorBody");
				GeoBone armorLeggingsBody = getChild(bodyBone, "armorLeggingsBody");
				GeoBone bodyLayer = getChild(bodyBone, "body_layer");

				boolean ob1 = armorBody != null && armorBody.isHidden();
				boolean ob2 = armorLeggingsBody != null && armorLeggingsBody.isHidden();
				boolean ob3 = bodyLayer != null && bodyLayer.isHidden();

				if (armorBody != null) armorBody.setHidden(true);
				if (armorLeggingsBody != null) armorLeggingsBody.setHidden(true);
				if (bodyLayer != null) bodyLayer.setHidden(true);

				renderTargetedBone(bodyBone, poseStack, bufferSource, animatable, texture, partialTick, packedLight);

				if (armorBody != null) armorBody.setHidden(ob1);
				if (armorLeggingsBody != null) armorLeggingsBody.setHidden(ob2);
				if (bodyLayer != null) bodyLayer.setHidden(ob3);

				scaleBoneRecursively(bodyBone, 1.0f / inflation);
			});

			poseStack.popPose();
		} else {
			ResourceLocation targetModelLoc = isOozaruTarget ? OOZARU_ARMOR_MODEL : (isSlimTarget ? MAJIN_SLIM_ARMOR_MODEL : MAJIN_ARMOR_MODEL);
			BakedGeoModel vanillaArmorModel = getGeoModel().getBakedModel(targetModelLoc);

			if (vanillaArmorModel != null) {
				ResourceLocation texture = getVanillaArmorTexture(animatable, stack, EquipmentSlot.CHEST, null);
				for (GeoBone bone : vanillaArmorModel.topLevelBones()) {
					syncBoneRecursively(bone, playerModel);
				}

				poseStack.pushPose();
				poseStack.translate(0, -0.05f, 0);
				renderFullModel(vanillaArmorModel, poseStack, bufferSource, animatable, texture, 1.0F, 1.0F, 1.0F, partialTick, packedLight);
				poseStack.popPose();

				if (armorItem instanceof DyeableArmorItem) {
					ResourceLocation overlayTex = getVanillaArmorTexture(animatable, stack, EquipmentSlot.CHEST, "overlay");
					renderFullModel(vanillaArmorModel, poseStack, bufferSource, animatable, overlayTex, 1f, 1f, 1f, partialTick, packedLight);
				}
			}
		}
	}

	private GeoBone getChild(GeoBone parent, String name) {
		for (GeoBone child : parent.getChildBones()) {
			if (child.getName().equals(name)) return child;
		}
		return null;
	}

	private void scaleBoneRecursively(GeoBone bone, float multiplier) {
		bone.setScaleX(bone.getScaleX() * multiplier);
		bone.setScaleY(bone.getScaleY() * multiplier);
		bone.setScaleZ(bone.getScaleZ() * multiplier);
		for (GeoBone child : bone.getChildBones()) {
			scaleBoneRecursively(child, multiplier);
		}
	}

	private void syncBoneRecursively(GeoBone destBone, BakedGeoModel sourceModel) {
		sourceModel.getBone(destBone.getName()).ifPresent(sourceBone -> {
			destBone.setRotX(sourceBone.getRotX());
			destBone.setRotY(sourceBone.getRotY());
			destBone.setRotZ(sourceBone.getRotZ());
			destBone.setPosX(sourceBone.getPosX());
			destBone.setPosY(sourceBone.getPosY());
			destBone.setPosZ(sourceBone.getPosZ());
			destBone.setPivotX(sourceBone.getPivotX());
			destBone.setPivotY(sourceBone.getPivotY());
			destBone.setPivotZ(sourceBone.getPivotZ());

			float inflation = 1.05f;
			destBone.setScaleX(sourceBone.getScaleX() * inflation);
			destBone.setScaleY(sourceBone.getScaleY() * inflation);
			destBone.setScaleZ(sourceBone.getScaleZ() * inflation);
		});

		for (GeoBone child : destBone.getChildBones()) {
			syncBoneRecursively(child, sourceModel);
		}
	}

	private ResourceLocation getDbzArmorTexture(DbzArmorItem item, ItemStack stack) {
		String itemId = item.getItemId();
		boolean isDamaged = item.isDamageOn() && stack.getDamageValue() > stack.getMaxDamage() / 2;
		String suffix = isDamaged ? "_damaged_layer1.png" : "_layer1.png";
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/" + itemId + suffix);
	}

	private ResourceLocation getVanillaArmorTexture(LivingEntity entity, ItemStack stack, EquipmentSlot slot, String type) {
		ArmorItem item = (ArmorItem) stack.getItem();
		String materialName = item.getMaterial().getName();
		String domain = "minecraft";
		if (materialName.contains(":")) {
			String[] split = materialName.split(":", 2);
			domain = split[0];
			materialName = split[1];
		} else {
			ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(item);
			if (itemRegistryName != null) domain = itemRegistryName.getNamespace();
		}
		String typeSuffix = (type == null || type.isEmpty()) ? "" : "_" + type;
		String textureLocation = String.format("%s:textures/models/armor/%s_layer_1%s.png", domain, materialName, typeSuffix);
		return ResourceLocation.parse(ForgeHooksClient.getArmorTexture(entity, stack, textureLocation, slot, type));
	}

	@SuppressWarnings("unchecked")
	private void renderTargetedBone(GeoBone targetBone, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, ResourceLocation texture, float partialTick, int packedLight) {
		RenderType armorRenderType = RenderType.armorCutoutNoCull(texture);
		((GeoRenderer<T>)getRenderer()).renderRecursively(poseStack, animatable, targetBone, armorRenderType, bufferSource, bufferSource.getBuffer(armorRenderType), true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
	}

	@SuppressWarnings("unchecked")
	private void renderFullModel(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, ResourceLocation texture, float r, float g, float b, float partialTick, int packedLight) {
		RenderType armorRenderType = RenderType.armorCutoutNoCull(texture);
		VertexConsumer buffer = bufferSource.getBuffer(armorRenderType);
		GeoRenderer<T> renderer = (GeoRenderer<T>) getRenderer();
		for (GeoBone bone : model.topLevelBones()) {
			renderer.renderRecursively(poseStack, animatable, bone, armorRenderType, bufferSource, buffer, true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, r, g, b, 1.0f);
		}
	}
}