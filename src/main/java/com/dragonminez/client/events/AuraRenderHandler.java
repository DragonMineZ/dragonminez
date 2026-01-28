package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.render.PlayerDMZRenderer;
import com.dragonminez.client.util.AuraRenderQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class AuraRenderHandler {
	private static final ResourceLocation AURA_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/entity/races/kiaura.geo.json");
	private static final ResourceLocation AURA_TEX_0 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/aura_ki_0.png");
	private static final ResourceLocation AURA_TEX_1 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/aura_ki_1.png");
	private static final ResourceLocation AURA_TEX_2 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/aura_ki_2.png");

    private static final ResourceLocation KI_WEAPONS_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/entity/races/kiweapons.geo.json");
    private static final ResourceLocation KI_WEAPONS_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/kiweapons.png");

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();

        var auras = AuraRenderQueue.getAndClearAuras();
        for (var entry : auras) {
            renderAuraEntry(entry, poseStack, buffers, dispatcher, mc);
        }

        var weapons = AuraRenderQueue.getAndClearWeapons();
        for (var entry : weapons) {
            var player = entry.player();
            EntityRenderer<? super Player> genericRenderer = dispatcher.getRenderer(player);

            if (genericRenderer instanceof PlayerDMZRenderer renderer) {
                BakedGeoModel weaponModel = renderer.getGeoModel().getBakedModel(KI_WEAPONS_MODEL);
                if (weaponModel == null) continue;

                resetModelParts(weaponModel);
                boolean isRight = player.getMainArm() == HumanoidArm.RIGHT;
                String boneName = getWeaponBoneName(entry.weaponType(), isRight);

                if (!boneName.isEmpty()) {
                    weaponModel.getBone(boneName).ifPresent(AuraRenderHandler::showBoneChain);
                    syncModelToPlayer(weaponModel, entry.playerModel());

                    poseStack.pushPose();
                    poseStack.last().pose().set(entry.poseMatrix());

                    renderer.reRender(weaponModel, poseStack, buffers, (GeoAnimatable)player,
                            ModRenderTypes.energy(KI_WEAPONS_TEXTURE),
                            buffers.getBuffer(ModRenderTypes.energy(KI_WEAPONS_TEXTURE)),
                            entry.partialTick(), 15728880, OverlayTexture.NO_OVERLAY,
                            entry.color()[0], entry.color()[1], entry.color()[2], 0.85f);

                    poseStack.popPose();
                }
            }
        }
        buffers.endBatch();
    }

    private static String getWeaponBoneName(String type, boolean isRight) {
        return switch (type.toLowerCase()) {
            case "blade" -> isRight ? "blade_right" : "blade_left";
            case "scythe" -> isRight ? "scythe_right" : "scythe_left";
            case "clawlance" -> isRight ? "trident_right" : "trident_left";
            default -> "";
        };
    }

	private static void syncModelToPlayer(BakedGeoModel auraModel, BakedGeoModel playerModel) {
		for (GeoBone auraBone : auraModel.topLevelBones()) {
			syncBoneRecursively(auraBone, playerModel);
		}
	}

    private static void showBoneChain(GeoBone bone) {
        setHiddenRecursive(bone, false);

        GeoBone parent = bone.getParent();
        while (parent != null) {
            parent.setHidden(false);
            parent = parent.getParent();
        }
    }

    private static void resetModelParts(BakedGeoModel model) {
        for (GeoBone bone : model.topLevelBones()) {
            setHiddenRecursive(bone, true);
        }
    }
    private static void setHiddenRecursive(GeoBone bone, boolean hidden) {
        bone.setHidden(hidden);
        for (GeoBone child : bone.getChildBones()) {
            setHiddenRecursive(child, hidden);
        }
    }

	private static void syncBoneRecursively(GeoBone destBone, BakedGeoModel sourceModel) {
		sourceModel.getBone(destBone.getName()).ifPresent(sourceBone -> {
			destBone.setRotX(sourceBone.getRotX());
			destBone.setRotY(sourceBone.getRotY());
			destBone.setRotZ(sourceBone.getRotZ());
			destBone.setPosX(sourceBone.getPosX());
			destBone.setPosY(sourceBone.getPosY());
			destBone.setPosZ(sourceBone.getPosZ());
			destBone.setScaleX(sourceBone.getScaleX());
			destBone.setScaleY(sourceBone.getScaleY());
			destBone.setScaleZ(sourceBone.getScaleZ());
		});

		for (GeoBone child : destBone.getChildBones()) {
			syncBoneRecursively(child, sourceModel);
		}
	}

	private static float[] getKiColor(StatsData stats) {
		var character = stats.getCharacter();
		String kiHex = character.getAuraColor();
		if (stats.getStatus().getActiveKaiokenPhase() >= 1) {
			kiHex = "#DB182C";
		} else if (character.hasActiveForm() && character.getActiveFormData() != null) {
			String formColor = character.getActiveFormData().getAuraColor();
			if (formColor != null && !formColor.isEmpty()) kiHex = formColor;
		}
		return ColorUtils.hexToRgb(kiHex);
	}

    private static void renderAuraEntry(AuraRenderQueue.AuraRenderEntry entry, PoseStack poseStack, MultiBufferSource.BufferSource buffers, EntityRenderDispatcher dispatcher, Minecraft mc) {
        var player = entry.player();
        if (!(player instanceof GeoAnimatable animatable)) return;

        EntityRenderer<? super Player> genericRenderer = dispatcher.getRenderer(player);
        if (!(genericRenderer instanceof @SuppressWarnings("rawtypes")PlayerDMZRenderer renderer)) return;

        BakedGeoModel auraModel = renderer.getGeoModel().getBakedModel(AURA_MODEL);
        if (auraModel == null) return;

        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null) return;
        float[] color = getKiColor(stats);

        syncModelToPlayer(auraModel, entry.playerModel());

        poseStack.pushPose();
        poseStack.last().pose().set(entry.poseMatrix());

        float scale = 1.025f;
        poseStack.scale(scale, scale, scale);

        long frame = (long) ((player.level().getGameTime() / 1.5f) % 3);
        ResourceLocation currentTexture = (frame == 0) ? AURA_TEX_0 : (frame == 1) ? AURA_TEX_1 : AURA_TEX_2;

        boolean isLocalPlayer = player == mc.player;
        float transparency = isLocalPlayer && mc.options.getCameraType().isFirstPerson() ? 0.075f : 0.15f;

        renderer.reRender(auraModel, poseStack, buffers, animatable, ModRenderTypes.energy(currentTexture),
                buffers.getBuffer(ModRenderTypes.energy(currentTexture)), entry.partialTick(), 15728880,
                OverlayTexture.NO_OVERLAY, color[0], color[1], color[2], transparency);

        poseStack.popPose();
    }
}