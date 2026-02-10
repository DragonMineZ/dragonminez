package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.util.AuraRenderQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.particles.AuraParticle;
import com.dragonminez.common.init.particles.DivineParticle;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.BetaWhitelist;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
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

    private static final ResourceLocation SPARK_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/entity/races/kirayos.geo.json");
    private static final ResourceLocation SPARK_TEX_0 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/rayo_0.png");
    private static final ResourceLocation SPARK_TEX_1 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/rayo_1.png");
    private static final ResourceLocation SPARK_TEX_2 = new ResourceLocation(Reference.MOD_ID, "textures/entity/ki/rayo_2.png");


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

        var sparks = AuraRenderQueue.getAndClearSparks();
        for (var entry : sparks) {
            renderSparkEntry(entry, poseStack, buffers, dispatcher, mc);
        }

        var weapons = AuraRenderQueue.getAndClearWeapons();
        for (var entry : weapons) {
            var player = entry.player();
            EntityRenderer<? super Player> genericRenderer = dispatcher.getRenderer(player);

            if (genericRenderer instanceof DMZPlayerRenderer renderer) {
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

    private static void renderSparkEntry(AuraRenderQueue.SparkRenderEntry entry, PoseStack poseStack, MultiBufferSource.BufferSource buffers, EntityRenderDispatcher dispatcher, Minecraft mc) {
        var player = entry.player();
        if (!(player instanceof GeoAnimatable animatable)) return;

        EntityRenderer<? super Player> genericRenderer = dispatcher.getRenderer(player);
        if (!(genericRenderer instanceof @SuppressWarnings("rawtypes")DMZPlayerRenderer renderer)) return;

        BakedGeoModel sparkModel = renderer.getGeoModel().getBakedModel(SPARK_MODEL);
        if (sparkModel == null) return;

        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null) return;
        var character = stats.getCharacter();
        var formData = character.getActiveFormData();
		if (!stats.getCharacter().hasActiveForm() || formData == null) return;
		if (!formData.hasLightnings()) return;
        float[] color = ColorUtils.hexToRgb(formData.getLightningColor());

        float formScaleX = 1.0f;
        float formScaleY = 1.0f;
        float formScaleZ = 1.0f;
        String formName = character.getActiveForm().toLowerCase();

        float[] scales = formData.getModelScaling();
        if (scales != null && scales.length >= 3) {
            formScaleX = scales[0];
            formScaleY = scales[1];
            formScaleZ = scales[2];
        }

        float extraSize = 0.0f;

        if (formName.contains("supersaiyan2") ||
                formName.contains("supersaiyan3") ||
                formName.contains("overdrive") ||
                formName.contains("supernamekian") ||
                formName.contains("fifth") ||
                formName.contains("ultra") ||
                formName.contains("superperfect")) {

            extraSize = 0.3f;
        }

        syncModelToPlayer(sparkModel, entry.playerModel());

        poseStack.pushPose();
        poseStack.last().pose().set(entry.poseMatrix());

        poseStack.scale(formScaleX + extraSize + 0.2f, formScaleY + extraSize+ 0.2f, formScaleZ + extraSize+ 0.2f);

        long frame = (long) ((player.level().getGameTime() / 1.0f) % 3);
        ResourceLocation currentTexture = (frame == 0) ? SPARK_TEX_0 : (frame == 1) ? SPARK_TEX_1 : SPARK_TEX_2;

        float transparency = 0.8f;

        renderer.reRender(sparkModel, poseStack, buffers, animatable, ModRenderTypes.lightning(currentTexture),
                buffers.getBuffer(ModRenderTypes.lightning(currentTexture)), entry.partialTick(), 15728880, // Luz máxima
                OverlayTexture.NO_OVERLAY, color[0], color[1], color[2], transparency);

        poseStack.popPose();
    }

    private static void renderAuraEntry(AuraRenderQueue.AuraRenderEntry entry, PoseStack poseStack, MultiBufferSource.BufferSource buffers, EntityRenderDispatcher dispatcher, Minecraft mc) {
        var player = entry.player();
        if (!(player instanceof GeoAnimatable animatable)) return;

        EntityRenderer<? super Player> genericRenderer = dispatcher.getRenderer(player);
        if (!(genericRenderer instanceof @SuppressWarnings("rawtypes")DMZPlayerRenderer renderer)) return;

        BakedGeoModel auraModel = renderer.getGeoModel().getBakedModel(AURA_MODEL);
        if (auraModel == null) return;

        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null) return;

        float formScaleX = 1.0f;
        float formScaleY = 1.0f;
        float formScaleZ = 1.0f;
        String formName = "";

        var character = stats.getCharacter();

        if (character.hasActiveForm()) {
            var activeForm = character.getActiveFormData();
            if (activeForm != null) {
                formName = character.getActiveForm().toLowerCase();

                float[] scales = activeForm.getModelScaling();
                if (scales != null && scales.length >= 3) {
                    formScaleX = scales[0];
                    formScaleY = scales[1];
                    formScaleZ = scales[2];
                }
            }
        }

        float extraSize = 0.0f;

        if (formName.contains("supersaiyan2") ||
                formName.contains("supersaiyan3") ||
                formName.contains("overdrive") ||
                formName.contains("supernamekian") ||
                formName.contains("fifth") ||
                formName.contains("ultra") ||
                formName.contains("superperfect")) {

            extraSize = 0.3f;
        }

        float[] color = getKiColor(stats);

        var totalScale = formScaleX + extraSize;

        if (player.onGround()) {
            spawnGroundDust(player, totalScale);
        }

        spawnFloatingRubble(player, totalScale);

        syncModelToPlayer(auraModel, entry.playerModel());

        poseStack.pushPose();
        poseStack.last().pose().set(entry.poseMatrix());

        poseStack.scale(formScaleX + extraSize + 0.2f, formScaleY + extraSize + 0.2f, formScaleZ + extraSize + 0.2f);

        long frame = (long) ((player.level().getGameTime() / 1.5f) % 3);
        ResourceLocation currentTexture = (frame == 0) ? AURA_TEX_0 : (frame == 1) ? AURA_TEX_1 : AURA_TEX_2;

        boolean isLocalPlayer = player == mc.player;
        float transparency = isLocalPlayer && mc.options.getCameraType().isFirstPerson() ? 0.075f : 0.15f;

        renderer.reRender(auraModel, poseStack, buffers, animatable, ModRenderTypes.energy(currentTexture),
                buffers.getBuffer(ModRenderTypes.energy(currentTexture)), entry.partialTick(), 15728880,
                OverlayTexture.NO_OVERLAY, color[0], color[1], color[2], transparency);

        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.side != net.minecraftforge.fml.LogicalSide.CLIENT) return;

        Player player = event.player;

        if (!BetaWhitelist.isAllowed(player.getGameProfile().getName())) return;

        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null) return;

        float scale = 1.0f;
        int particleColor = 0xFFFFFF;

        var character = stats.getCharacter();

        try {
            String hex = character.getAuraColor();
            if (hex != null && !hex.isEmpty()) {
                particleColor = Integer.decode(hex);
            }
        } catch (Exception ignored) {}

        if (character.hasActiveForm()) {
            var activeForm = character.getActiveFormData();
            if (activeForm != null) {
                float[] scales = activeForm.getModelScaling();
                if (scales != null && scales.length >= 1) {
                    scale = scales[0];
                }

                String formHex = activeForm.getAuraColor();

                if (formHex != null && !formHex.isEmpty()) {
                    try {
                        particleColor = Integer.decode(formHex);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        for (int i = 0; i < 1; i++) {
            spawnCalmAuraParticle(player, scale, particleColor);
        }

        if (player.getRandom().nextInt(20) == 0) {
            int divineCount = 5 + player.getRandom().nextInt(10);
            for (int i = 0; i < divineCount; i++) {
                spawnPassiveDivineParticle(player, scale, 0xFFFFFF);
            }
        }
    }


    private static void spawnCalmAuraParticle(Player player, float totalScale, int colorHex) {
        var mc = Minecraft.getInstance();
        var level = player.level();
        var random = player.getRandom();

        double radius = (0.2f + random.nextDouble() * 0.3f) * totalScale;
        double angle = random.nextDouble() * 2 * Math.PI;

        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;

        double heightOffset = (random.nextDouble() * 1.8f) * totalScale;

        double x = player.getX() + offsetX;
        double y = player.getY() + heightOffset;
        double z = player.getZ() + offsetZ;

        float r = ((colorHex >> 16) & 0xFF) / 255f;
        float g = ((colorHex >> 8) & 0xFF) / 255f;
        float b = (colorHex & 0xFF) / 255f;

        Particle p = mc.particleEngine.createParticle(MainParticles.AURA.get(), x, y, z, r, g, b);

        if (p instanceof AuraParticle auraP) {
            auraP.resize(totalScale);

            double driftSpeed = 0.02f;
            double velX = (offsetX / radius) * driftSpeed;
            double velZ = (offsetZ / radius) * driftSpeed;

            double velY = 0.01f + (random.nextDouble() * 0.02f);

            auraP.setParticleSpeed(velX, velY, velZ);
        }
    }

    private static void spawnPassiveDivineParticle(Player player, float totalScale, int colorHex) {
        var mc = Minecraft.getInstance();
        var level = player.level();
        var random = player.getRandom();

        double widthSpread = player.getBbWidth() * totalScale * 2.0;
        double offsetX = (random.nextDouble() - 0.5) * widthSpread;
        double offsetZ = (random.nextDouble() - 0.5) * widthSpread;

        double x = player.getX() + offsetX;
        double z = player.getZ() + offsetZ;

        double heightSpread = (random.nextDouble() * 1.2) * totalScale;
        double y = player.getY() + heightSpread;

        float r = ((colorHex >> 16) & 0xFF) / 255f;
        float g = ((colorHex >> 8) & 0xFF) / 255f;
        float b = (colorHex & 0xFF) / 255f;

        Particle p = mc.particleEngine.createParticle(MainParticles.DIVINE.get(), x, y, z, r, g, b);

        if (p instanceof DivineParticle divineP) {
            divineP.resize(totalScale);

            double velY = 0.02 + (random.nextDouble() * 0.03);
            divineP.setParticleSpeed(0, velY, 0);
        }
    }

    private static void spawnGroundDust(Player player, float totalScale) {
        if (player.getRandom().nextFloat() > 0.3f) return;

        var level = player.level();
        var random = player.getRandom();

        double angle = random.nextDouble() * 2 * Math.PI;

        double radius = (0.6f + random.nextDouble() * 0.4f) * totalScale;

        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;

        double x = player.getX() + offsetX;
        double y = player.getY()+ 0.3;
        double z = player.getZ() + offsetZ;

        double speedBase = 0.15f;
        double velX = Math.cos(angle) * speedBase;
        double velY = 0.1f;
        double velZ = Math.sin(angle) * speedBase;

        level.addParticle(MainParticles.DUST.get(),
                x, y, z,
                velX, velY, velZ);
    }

    private static void spawnFloatingRubble(Player player, float totalScale) {
        if (player.getRandom().nextFloat() > 0.15f) return;

        var level = player.level();
        var random = player.getRandom();

        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = (0.5f + random.nextDouble() * 1.9f) * totalScale;

        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;

        double x = player.getX() + offsetX;
        double y = player.getY() + 0.1;
        double z = player.getZ() + offsetZ;

        double velX = (random.nextDouble() - 0.5) * 0.05;
        double velZ = (random.nextDouble() - 0.5) * 0.05;

        double velY = 0.05 + (random.nextDouble() * 0.1);

            level.addParticle(MainParticles.ROCK.get(), x, y, z, velX, velY, velZ);
    }

}