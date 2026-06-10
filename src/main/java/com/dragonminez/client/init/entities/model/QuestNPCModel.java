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
import java.util.Map;
import java.util.Set;

/**
 * Dynamic GeckoLib model for QuestNPCEntity.
 * Resolves quest NPC visuals from existing saga/master assets with a Goku fallback.
 */
public class QuestNPCModel extends GeoModel<QuestNPCEntity> {

	private static final String FALLBACK_MODEL = "saga_goku";
	private static final String FALLBACK_TEXTURE = "saga_goku_early";
	private static final String SAGA_BASE_ANIMATION = "animations/entity/sagas/saga_base.animation.json";

	private static final AssetPaths FALLBACK = saga(FALLBACK_MODEL, FALLBACK_TEXTURE);
	private static final Map<String, AssetPaths> NPC_ASSETS = Map.ofEntries(
			Map.entry("generic_npc", FALLBACK),
			Map.entry("goku", FALLBACK),
			Map.entry("bulma", saga("saga_bulma", "saga_bulma")),
			Map.entry("krillin", saga("saga_vegeta", "saga_krillin")),
			Map.entry("yamcha", saga("saga_yamcha", "saga_yamcha")),
			Map.entry("tien", saga("saga_goku", "saga_tien_early")),
			Map.entry("chiaotzu", saga("saga_chaoz", "saga_chaoz")),
			Map.entry("piccolo", saga("saga_piccolo", "saga_piccolo")),
			Map.entry("gohan", saga("saga_gohan_mid", "saga_gohan_mid_base")),
			Map.entry("vegeta", saga("saga_vegeta", "saga_vegeta")),
			Map.entry("trunks", saga("saga_trunks", "saga_ftrunks_base")),
			Map.entry("videl", saga("saga_videl", "saga_videl")),
			Map.entry("shin", saga("saga_shin", "saga_shin")),
			Map.entry("namek_elder", master("master_guru"))
	);

	/** Cache which resource keys have been confirmed to have assets, to avoid repeated resource lookups. */
	private static final Set<String> VALID_GEO_KEYS = new HashSet<>();
	private static final Set<String> VALID_TEXTURE_KEYS = new HashSet<>();
	private static final Set<String> VALID_ANIMATION_KEYS = new HashSet<>();
	private static final Set<String> MISSING_GEO_KEYS = new HashSet<>();
	private static final Set<String> MISSING_TEXTURE_KEYS = new HashSet<>();
	private static final Set<String> MISSING_ANIMATION_KEYS = new HashSet<>();

	@Override
	public ResourceLocation getModelResource(QuestNPCEntity animatable) {
		String modelKey = animatable.getModelKey();
		AssetPaths asset = resolveAsset(animatable);
		return existingOrFallback(asset.model(), fallbackModel(), VALID_GEO_KEYS, MISSING_GEO_KEYS);
	}

	@Override
	public ResourceLocation getTextureResource(QuestNPCEntity animatable) {
		String textureKey = animatable.getTextureKey();
		AssetPaths asset = resolveAsset(animatable);
		ResourceLocation texture = assetFromKey(textureKey, "textures/entity/sagas/", ".png");
		if (resourceExistsCached(texture, VALID_TEXTURE_KEYS, MISSING_TEXTURE_KEYS)) {
			return texture;
		}
		return existingOrFallback(asset.texture(), fallbackTexture(), VALID_TEXTURE_KEYS, MISSING_TEXTURE_KEYS);
	}

	@Override
	public ResourceLocation getAnimationResource(QuestNPCEntity animatable) {
		AssetPaths asset = resolveAsset(animatable);
		return existingOrFallback(asset.animation(), fallbackAnimation(), VALID_ANIMATION_KEYS, MISSING_ANIMATION_KEYS);
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

	private static boolean resourceExistsCached(ResourceLocation location, Set<String> valid, Set<String> missing) {
		String key = location.toString();
		if (valid.contains(key)) {
			return true;
		}
		if (missing.contains(key)) {
			return false;
		}
		if (resourceExists(location)) {
			valid.add(key);
			return true;
		}
		missing.add(key);
		return false;
	}

	private static ResourceLocation existingOrFallback(ResourceLocation candidate, ResourceLocation fallback,
													  Set<String> valid, Set<String> missing) {
		return resourceExistsCached(candidate, valid, missing) ? candidate : fallback;
	}

	private static AssetPaths resolveAsset(QuestNPCEntity animatable) {
		String modelKey = animatable.getModelKey();
		AssetPaths explicit = NPC_ASSETS.get(modelKey);
		if (explicit != null) {
			return explicit;
		}
		AssetPaths byNpc = NPC_ASSETS.get(animatable.getNpcId());
		if (byNpc != null && (modelKey == null || modelKey.isBlank() || modelKey.equals(animatable.getNpcId()))) {
			return byNpc;
		}
		ResourceLocation sagaModel = assetFromKey(modelKey, "geo/entity/sagas/", ".geo.json");
		if (resourceExistsCached(sagaModel, VALID_GEO_KEYS, MISSING_GEO_KEYS)) {
			return saga(modelKey, animatable.getTextureKey());
		}
		ResourceLocation masterModel = assetFromKey(modelKey, "geo/entity/master/", ".geo.json");
		if (resourceExistsCached(masterModel, VALID_GEO_KEYS, MISSING_GEO_KEYS)) {
			return master(modelKey);
		}
		return byNpc != null ? byNpc : FALLBACK;
	}

	private static AssetPaths saga(String modelKey, String textureKey) {
		String safeTexture = textureKey == null || textureKey.isBlank() ? FALLBACK_TEXTURE : textureKey;
		return new AssetPaths(
				assetFromKey(modelKey, "geo/entity/sagas/", ".geo.json"),
				assetFromKey(safeTexture, "textures/entity/sagas/", ".png"),
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, SAGA_BASE_ANIMATION)
		);
	}

	private static AssetPaths master(String key) {
		return new AssetPaths(
				assetFromKey(key, "geo/entity/master/", ".geo.json"),
				assetFromKey(key, "textures/entity/master/", ".png"),
				assetFromKey(key, "animations/entity/master/", ".animation.json")
		);
	}

	private static ResourceLocation assetFromKey(String key, String prefix, String suffix) {
		String safeKey = key == null || key.isBlank() ? FALLBACK_MODEL : key;
		if (safeKey.contains(":")) {
			return ResourceLocation.parse(safeKey);
		}
		if (safeKey.contains("/")) {
			return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, safeKey);
		}
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + safeKey + suffix);
	}

	private static ResourceLocation fallbackModel() {
		return FALLBACK.model();
	}

	private static ResourceLocation fallbackTexture() {
		return FALLBACK.texture();
	}

	private static ResourceLocation fallbackAnimation() {
		return FALLBACK.animation();
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

	private record AssetPaths(ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
	}
}

