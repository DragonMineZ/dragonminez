package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.HashSet;
import java.util.Set;

/**
 * Dynamic GeckoLib model for QuestNPCEntity.
 * Resolves model/texture/animation from the entity's npcId with automatic fallback to generic_npc
 * when the NPC-specific asset files don't exist in the resource pack.
 */
public class QuestNPCModel extends GeoModel<QuestNPCEntity> {

	private static final String FALLBACK = "generic_npc";

	private static final ResourceLocation FALLBACK_GEO =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/questnpc/generic_npc.geo.json");
	private static final ResourceLocation FALLBACK_TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/questnpc/generic_npc.png");
	private static final ResourceLocation FALLBACK_ANIMATION =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/questnpc/generic_npc.animation.json");

	/** Cache which npcIds/modelKeys have been confirmed to have assets, to avoid repeated resource lookups. */
	private static final Set<String> VALID_GEO_KEYS = new HashSet<>();
	private static final Set<String> VALID_TEXTURE_KEYS = new HashSet<>();
	private static final Set<String> VALID_ANIMATION_KEYS = new HashSet<>();
	private static final Set<String> MISSING_GEO_KEYS = new HashSet<>();
	private static final Set<String> MISSING_TEXTURE_KEYS = new HashSet<>();
	private static final Set<String> MISSING_ANIMATION_KEYS = new HashSet<>();

	@Override
	public ResourceLocation getModelResource(QuestNPCEntity animatable) {
		String modelKey = animatable.getModelKey();
		if (FALLBACK.equals(modelKey) || MISSING_GEO_KEYS.contains(modelKey)) return FALLBACK_GEO;
		if (VALID_GEO_KEYS.contains(modelKey)) {
			return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/questnpc/" + modelKey + ".geo.json");
		}

		ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/questnpc/" + modelKey + ".geo.json");
		if (resourceExists(candidate)) {
			VALID_GEO_KEYS.add(modelKey);
			return candidate;
		} else {
			MISSING_GEO_KEYS.add(modelKey);
			return FALLBACK_GEO;
		}
	}

	@Override
	public ResourceLocation getTextureResource(QuestNPCEntity animatable) {
		String npcId = animatable.getNpcId();
		if (FALLBACK.equals(npcId) || MISSING_TEXTURE_KEYS.contains(npcId)) return FALLBACK_TEXTURE;
		if (VALID_TEXTURE_KEYS.contains(npcId)) {
			return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/questnpc/" + npcId + ".png");
		}

		ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/questnpc/" + npcId + ".png");
		if (resourceExists(candidate)) {
			VALID_TEXTURE_KEYS.add(npcId);
			return candidate;
		} else {
			MISSING_TEXTURE_KEYS.add(npcId);
			return FALLBACK_TEXTURE;
		}
	}

	@Override
	public ResourceLocation getAnimationResource(QuestNPCEntity animatable) {
		String modelKey = animatable.getModelKey();
		if (FALLBACK.equals(modelKey) || MISSING_ANIMATION_KEYS.contains(modelKey)) return FALLBACK_ANIMATION;
		if (VALID_ANIMATION_KEYS.contains(modelKey)) {
			return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/questnpc/" + modelKey + ".animation.json");
		}

		ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/questnpc/" + modelKey + ".animation.json");
		if (resourceExists(candidate)) {
			VALID_ANIMATION_KEYS.add(modelKey);
			return candidate;
		} else {
			MISSING_ANIMATION_KEYS.add(modelKey);
			return FALLBACK_ANIMATION;
		}
	}

	@Override
	public void setCustomAnimations(QuestNPCEntity animatable, long instanceId, AnimationState<QuestNPCEntity> animationState) {
		CoreGeoBone head = getAnimationProcessor().getBone("head");

		if (head != null) {
			EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}
	}

	/**
	 * Checks whether a resource exists in the current resource manager.
	 */
	private static boolean resourceExists(ResourceLocation location) {
		try {
			ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
			return resourceManager.getResource(location).isPresent();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Clears the asset cache. Call on resource reload if needed.
	 */
	public static void clearCache() {
		VALID_GEO_KEYS.clear();
		VALID_TEXTURE_KEYS.clear();
		VALID_ANIMATION_KEYS.clear();
		MISSING_GEO_KEYS.clear();
		MISSING_TEXTURE_KEYS.clear();
		MISSING_ANIMATION_KEYS.clear();
	}
}

