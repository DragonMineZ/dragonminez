package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.LightningBoltRenderer;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.shader.EffectBloomRenderer;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.KiEmberRenderer;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.server.events.players.combat.KiTechniqueHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;


public class KiWaveRenderer extends EntityRenderer<KiWaveEntity> {
    private static final ResourceLocation TEXTURE_WAVE_CORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiwave.png");

    private static final float MUZZLE_FLAME_SCALE = 1.55F;
    private static final float DOUBLE_WAVE_BALL_SCALE = 0.82F;
    /** Ticks of offset between the two orbs of a double wave, so they do not shed in mirror. */
    private static final float EMBER_PAIR_OFFSET = 6.5F;
    private static final float CHARGE_GROW_TICKS = 12.0F;
    private static final float CHARGE_RAYS_REACH = 11.0F;
    private static final float CHARGE_RAYS_MIN_REACH = 4.0F;
    private static final float CHARGE_RAYS_INTENSITY = 0.9F;
    private static final float CHARGE_RAYS_BLOOM = 0.35F;
    private static final float CHARGE_RAYS_FIRST_PERSON = 0.3F;
    private static final float BALL_LIGHTNING_RADIUS = 0.9F;
    private static final float BALL_LIGHTNING_SPEED = 1.5F;
    private static final float HEAT_DOME_SPHERE_ALPHA = 0.85F;
    private static final float HEAT_DOME_SPHERE_ALPHA_FIRST_PERSON = 0.35F;

    public KiWaveRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(KiWaveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Matrix4f basePose = new Matrix4f(poseStack.last().pose());

        PlayerEffectQueue.addKiAttack((stack, proj) -> {
            stack.pushPose();
            stack.last().pose().set(basePose);

            float[] auraColor = entity.getRgbColorMain();
            float[] borderColor = entity.getRgbColorBorder();

            float exactAge = entity.tickCount + partialTick;
            float fadeAlpha = 1.0F;

            int maxLife = entity.getMaxLife();
            int fireTick = entity.getFireTick();
            int firingWindow = fireTick >= 0 ? maxLife - fireTick : maxLife;
            int fadeTicks = Math.max(1, Math.min(20, firingWindow / 2));

            if (entity.tickCount >= maxLife - fadeTicks) {
                fadeAlpha = (maxLife - exactAge) / (float) fadeTicks;
                fadeAlpha = Math.max(0.0F, fadeAlpha);
            }

            int renderType = entity.getKiRenderType();

            if (!entity.isFiring() && KiTechniqueHandler.HeatDome.is(entity.getTechniqueId())) {
                renderHeatDomeSphere(entity, exactAge, stack, proj, auraColor, borderColor, fadeAlpha);
                stack.popPose();
                return;
            }

            switch (renderType) {
                case 1:
                case 2:
                    KiRenderWaveBrightness(entity, exactAge, stack, proj, auraColor, borderColor, fadeAlpha);
                    break;
                case 3:
                    KiRenderWaveDouble(entity, exactAge, stack, proj, auraColor, borderColor, fadeAlpha, true);
                    break;
                case 5:
                    KiRenderWaveDouble(entity, exactAge, stack, proj, auraColor, borderColor, fadeAlpha, false);
                    break;
                default:
                    KiRenderWave(entity, exactAge, stack, proj, auraColor, borderColor, fadeAlpha);
                    break;
            }
            stack.popPose();
        });
    }

    private static float chargeScale(KiWaveEntity entity, float ageInTicks) {
        float castTime = (float) entity.getCastWave();
        float ramp = castTime > 0.1F ? castTime : CHARGE_GROW_TICKS;
        float t = Mth.clamp(ageInTicks / ramp, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private void renderHeatDomeSphere(KiWaveEntity entity, float ageInTicks, PoseStack poseStack, Matrix4f proj, float[] auraColor, float[] borderColor, float fadeAlpha) {
        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        float scale = entity.getCastSize() * 1.5F * chargeScale(entity, ageInTicks);
        if (scale <= 0.01F) return;

        float alpha = HEAT_DOME_SPHERE_ALPHA;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == mc.player && entity.getOwner() == mc.player) alpha = HEAT_DOME_SPHERE_ALPHA_FIRST_PERSON;

        float[] outlineColor = entity.getRgbColorOutline();
        shader.safeGetUniform("colorCore").set(auraColor[0], auraColor[1], auraColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);
        shader.safeGetUniform("blotchMode").set(0.0f);
        shader.safeGetUniform("flameMode").set(0.0f);
        shader.safeGetUniform("orbMode").set(0.0f);
        shader.safeGetUniform("zCut").set(-1.0f);

        VertexBuffer mesh = KiMeshFactory.getSphereMesh();
        mesh.bind();

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(alpha * fadeAlpha);
        shader.apply();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();

        VertexBuffer.unbind();
        shader.clear();
    }

    private void KiRenderWave(KiWaveEntity entity, float ageInTicks, PoseStack poseStack, Matrix4f proj, float[] auraColor, float[] borderColor, float fadeAlpha) {
        float targetCastSize = entity.getCastSize();
        float finalSize = entity.getSize();
        float[] outlineColor = entity.getRgbColorOutline();

        boolean isFiring = entity.isFiring();
        float currentWidth = isFiring ? finalSize : targetCastSize * chargeScale(entity, ageInTicks);

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();

        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        shader.safeGetUniform("colorCore").set(auraColor[0], auraColor[1], auraColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);

        VertexBuffer sphereMesh = KiMeshFactory.getSphereMesh();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        float ballScale = currentWidth * 1.5F;

        if (!isFiring) {
            poseStack.pushPose();
            poseStack.scale(ballScale, ballScale, ballScale);

            shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
            shader.safeGetUniform("alphaMult").set(1.0f * fadeAlpha);
            shader.safeGetUniform("zCut").set(-1.0f);
            shader.safeGetUniform("blotchMode").set(1.0f);
            shader.safeGetUniform("flameMode").set(1.0f);
            shader.safeGetUniform("orbMode").set(1.0f);
            shader.apply();
            sphereMesh.bind();
            sphereMesh.drawWithShader(poseStack.last().pose(), proj, shader);
            poseStack.popPose();
            VertexBuffer.unbind();
            shader.safeGetUniform("blotchMode").set(0.0f);
            shader.safeGetUniform("flameMode").set(0.0f);
            shader.safeGetUniform("orbMode").set(0.0f);

            poseStack.pushPose();
            poseStack.scale(ballScale, ballScale, ballScale);
            renderEmbers(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, KiEmberRenderer.LOCAL_BACK, KiEmberRenderer.CHARGE_BACKDRAFT);
            poseStack.popPose();
        }

        if (isFiring) {
            float length = Math.max(entity.getBeamLength(), 0.1F);
            VertexBuffer cylinderMesh = KiMeshFactory.getCylinderMesh();

            poseStack.pushPose();
            float cylinderWidth = currentWidth * 1.2F;
            poseStack.scale(cylinderWidth, cylinderWidth, length);

            float zCut = Math.min((ballScale * 0.95F) / length, 0.49F);
            shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
            shader.safeGetUniform("alphaMult").set(1.0f * fadeAlpha);
            shader.safeGetUniform("zCut").set(zCut);
            shader.safeGetUniform("zCutFar").set(2.0f);
            shader.apply();
            cylinderMesh.bind();
            cylinderMesh.drawWithShader(poseStack.last().pose(), proj, shader);
            poseStack.popPose();
            shader.safeGetUniform("zCut").set(-1.0f);
            VertexBuffer.unbind();

            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, length);
            float endBallScale = currentWidth * 1.5F;
            poseStack.scale(endBallScale, endBallScale, endBallScale);
            shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
            shader.safeGetUniform("alphaMult").set(1.0f * fadeAlpha);
            shader.safeGetUniform("zCut").set(-1.0f);
            shader.safeGetUniform("blotchMode").set(1.0f);
            shader.safeGetUniform("flameMode").set(1.0f);
            shader.apply();
            sphereMesh.bind();
            sphereMesh.drawWithShader(poseStack.last().pose(), proj, shader);
            poseStack.popPose();
            VertexBuffer.unbind();
            shader.safeGetUniform("blotchMode").set(0.0f);
            shader.safeGetUniform("flameMode").set(0.0f);

            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, length);
            poseStack.scale(endBallScale, endBallScale, endBallScale);
            renderEmbers(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, KiEmberRenderer.LOCAL_BACK, 1.0F);
            poseStack.popPose();

            renderMuzzleFlame(entity, poseStack, proj, auraColor, borderColor, ageInTicks, ballScale * MUZZLE_FLAME_SCALE, ballScale * MUZZLE_FLAME_SCALE, fadeAlpha, KiEmberRenderer.LOCAL_BACK, 1.0F);
        }

        poseStack.popPose();
        shader.clear();
    }

    private void KiRenderWaveBrightness(KiWaveEntity entity, float ageInTicks, PoseStack poseStack, Matrix4f proj, float[] auraColor, float[] borderColor, float fadeAlpha) {
        float targetCastSize = entity.getCastSize();
        float finalSize = entity.getSize();

        boolean isFiring = entity.isFiring();
        float currentWidth = isFiring ? finalSize : targetCastSize * chargeScale(entity, ageInTicks);

        float basePulse = isFiring ? (1.0F + (float) Math.sin(ageInTicks * 1.5F) * 0.15F) : 1.0F;
        float width = currentWidth * basePulse;

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 back = dir.scale(-1.0D);

        poseStack.pushPose();

        if (!isFiring) {
            poseStack.pushPose();
            float startBallScale = width * 1.5F;
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, true);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderEmbers(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, back, KiEmberRenderer.CHARGE_BACKDRAFT);
            poseStack.popPose();

            if (entity.getKiRenderType() == 1) {
                renderChargeRays(entity, poseStack, proj, auraColor, borderColor, ageInTicks, startBallScale, fadeAlpha);
            } else if (entity.getKiRenderType() == 2) {
                renderGalickLightning(poseStack, proj, entity, 0, borderColor, fadeAlpha, ageInTicks, startBallScale);
            }

            poseStack.popPose();
            return;
        }

        float length = Math.max(entity.getBeamLength(), 0.1F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        float tubeLength = Math.max(length, 0.1F);
        renderKiCylinderWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, width, tubeLength, fadeAlpha, width * 1.5F, width * 2.5F);
        poseStack.popPose();

        boolean galick = entity.getKiRenderType() == 2;

        poseStack.pushPose();
        Vec3 startPosSphere = dir.scale(0.1D);
        poseStack.translate(startPosSphere.x, startPosSphere.y, startPosSphere.z);
        renderMuzzleFlame(entity, poseStack, proj, auraColor, borderColor, ageInTicks, width * 1.5F * MUZZLE_FLAME_SCALE, currentWidth * 1.5F * MUZZLE_FLAME_SCALE, fadeAlpha, back, 1.0F);
        if (galick) {
            renderGalickLightning(poseStack, proj, entity, 0, borderColor, fadeAlpha, ageInTicks, currentWidth * 1.5F * MUZZLE_FLAME_SCALE);
        }
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 endPosSphere = dir.scale(length);
        poseStack.translate(endPosSphere.x, endPosSphere.y, endPosSphere.z);

        poseStack.pushPose();
        float endBallScaleDisp = width * 2.5F;
        poseStack.scale(endBallScaleDisp, endBallScaleDisp, endBallScaleDisp);
        renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, false);
        poseStack.popPose();

        if (galick) {
            renderGalickLightning(poseStack, proj, entity, 1, borderColor, fadeAlpha, ageInTicks, currentWidth * 2.5F);
        }

        // Embers ride the steady radius, not the breathing one, so the cloud does not throb with it.
        float endEmberScale = currentWidth * 2.5F;
        poseStack.scale(endEmberScale, endEmberScale, endEmberScale);
        renderEmbers(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, back, 1.0F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void KiRenderWaveDouble(KiWaveEntity entity, float ageInTicks, PoseStack poseStack, Matrix4f proj, float[] auraColor, float[] borderColor, float fadeAlpha, boolean withLightning) {
        float targetCastSize = entity.getCastSize();
        float finalSize = entity.getSize();

        boolean isFiring = entity.isFiring();
        float currentWidth = isFiring ? finalSize : targetCastSize * chargeScale(entity, ageInTicks);

        float basePulse = isFiring ? (1.0F + (float) Math.sin(ageInTicks * 1.5F) * 0.15F) : 1.0F;
        float width = currentWidth * basePulse;

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        // These call sites work in the unrotated entity frame, so backwards is just -dir.
        Vec3 back = dir.scale(-1.0D);

        poseStack.pushPose();

        if (!isFiring) {
            float startBallScale = width * DOUBLE_WAVE_BALL_SCALE;
            float initialSpread = 1.8F;
            float hitboxWidth = entity.getOwner() != null ? entity.getOwner().getBbWidth() : 0.5F;
            float holdTicks = 23F;
            float joinTicks = 24F;
            float convergeProgress = ageInTicks <= holdTicks ? 0.0F : Math.min(1.0F, (ageInTicks - holdTicks) / (joinTicks - holdTicks));
            float lateralOffset = (hitboxWidth * initialSpread) * (1.0F - convergeProgress);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.translate(lateralOffset, 0.0D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.pushPose();
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, true);
            poseStack.popPose();
            if (withLightning) {
                renderGalickLightning(poseStack, proj, entity, 2, borderColor, fadeAlpha, ageInTicks, startBallScale);
            }
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderEmbers(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, back, KiEmberRenderer.CHARGE_BACKDRAFT);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.translate(-lateralOffset, 0.0D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.pushPose();
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, true);
            poseStack.popPose();
            if (withLightning) {
                renderGalickLightning(poseStack, proj, entity, 3, borderColor, fadeAlpha, ageInTicks, startBallScale);
            }
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderEmbers(entity, poseStack, proj, auraColor, borderColor, ageInTicks + EMBER_PAIR_OFFSET, fadeAlpha, back, KiEmberRenderer.CHARGE_BACKDRAFT);
            poseStack.popPose();

            poseStack.popPose();
            return;
        }

        float length = Math.max(entity.getBeamLength(), 0.1F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        renderKiCylinderWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, width, length, fadeAlpha, width * 1.8F, width * 2.5F);
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 startPos = dir.scale(0.1D);
        poseStack.translate(startPos.x, startPos.y, startPos.z);
        renderMuzzleFlame(entity, poseStack, proj, auraColor, borderColor, ageInTicks, width * 1.5F * MUZZLE_FLAME_SCALE * DOUBLE_WAVE_BALL_SCALE, currentWidth * 1.5F * MUZZLE_FLAME_SCALE * DOUBLE_WAVE_BALL_SCALE, fadeAlpha, back, 1.0F);
        if (withLightning) {
            renderGalickLightning(poseStack, proj, entity, 0, borderColor, fadeAlpha, ageInTicks, currentWidth * 1.5F * MUZZLE_FLAME_SCALE * DOUBLE_WAVE_BALL_SCALE);
        }
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 endPos = dir.scale(length);
        poseStack.translate(endPos.x, endPos.y, endPos.z);

        poseStack.pushPose();
        float endScale = width * 2.5F * DOUBLE_WAVE_BALL_SCALE;
        poseStack.scale(endScale, endScale, endScale);
        renderKiSphereWithShader(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, false);
        poseStack.popPose();

        if (withLightning) {
            renderGalickLightning(poseStack, proj, entity, 1, borderColor, fadeAlpha, ageInTicks, currentWidth * 2.5F * DOUBLE_WAVE_BALL_SCALE);
        }

        float endEmberScale = currentWidth * 2.5F * DOUBLE_WAVE_BALL_SCALE;
        poseStack.scale(endEmberScale, endEmberScale, endEmberScale);
        renderEmbers(entity, poseStack, proj, auraColor, borderColor, ageInTicks, fadeAlpha, back, 1.0F);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderGalickLightning(PoseStack poseStack, Matrix4f proj, KiWaveEntity entity, int slot, float[] color, float alpha, float ageInTicks, float ballRadius) {
        if (ballRadius <= 0.05F) return;
        LightningBoltRenderer.drawSphereKiPass(poseStack.last().pose(), proj, entity.getId() * 4 + slot, ageInTicks,
                ballRadius * BALL_LIGHTNING_RADIUS, color, BALL_LIGHTNING_SPEED, alpha);
    }

    private void renderChargeRays(KiWaveEntity entity, PoseStack poseStack, Matrix4f proj, float[] coreColor, float[] borderColor, float ageInTicks, float ballRadius, float alphaMultiplier) {
        ShaderInstance shader = DMZShaders.chargeRaysShader;
        if (shader == null || ballRadius <= 0.001F) return;

        Minecraft mc = Minecraft.getInstance();
        float charge = chargeScale(entity, ageInTicks);
        float pulse = 0.85F + 0.15F * (float) Math.sin(ageInTicks * 0.9F);
        float intensity = CHARGE_RAYS_INTENSITY * charge * pulse * alphaMultiplier;
        if (EffectBloomRenderer.bloomPass) intensity *= CHARGE_RAYS_BLOOM;
        if (mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == mc.player && entity.getOwner() == mc.player) intensity *= CHARGE_RAYS_FIRST_PERSON;
        if (intensity <= 0.004F) return;

        float rayScale = Math.max(ballRadius * CHARGE_RAYS_REACH, CHARGE_RAYS_MIN_REACH);

        poseStack.pushPose();
        poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());
        poseStack.scale(rayScale, rayScale, rayScale);

        shader.safeGetUniform("ProjMat").set(proj);
        shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("seed").set((entity.getId() % 97) * 0.37f);
        shader.safeGetUniform("intensity").set(intensity);
        shader.safeGetUniform("innerRadius").set(ballRadius / rayScale);
        shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);

        VertexBuffer mesh = AuraMeshFactory.getBillboardQuad();
        mesh.bind();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);
        VertexBuffer.unbind();
        shader.clear();

        poseStack.popPose();
    }

    private void renderMuzzleFlame(KiWaveEntity entity, PoseStack poseStack, Matrix4f proj, float[] coreColor, float[] borderColor, float ageInTicks, float scale, float steadyScale, float alphaMultiplier, Vec3 back, float backdraft) {
        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        float[] outlineColor = entity.getRgbColorOutline();
        float breath = 1.0F + (float) Math.sin(ageInTicks * 3.1F) * 0.09F + (float) Math.sin(ageInTicks * 7.3F) * 0.04F;
        float finalScale = scale * breath;

        shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);
        shader.safeGetUniform("flameMode").set(1.0f);
        shader.safeGetUniform("blotchMode").set(1.0f);

        VertexBuffer mesh = KiMeshFactory.getSphereMesh();
        mesh.bind();

        poseStack.pushPose();
        poseStack.scale(finalScale, finalScale, finalScale);
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        shader.safeGetUniform("zCut").set(-1.0f);
        shader.safeGetUniform("zCutFar").set(2.0f);
        shader.apply();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();

        VertexBuffer.unbind();
        shader.safeGetUniform("flameMode").set(0.0f);
        shader.safeGetUniform("blotchMode").set(0.0f);

        poseStack.pushPose();
        poseStack.scale(steadyScale, steadyScale, steadyScale);
        renderEmbers(entity, poseStack, proj, coreColor, borderColor, ageInTicks, alphaMultiplier, back, backdraft);
        poseStack.popPose();

        shader.clear();
    }

    private void renderKiSphereWithShader(KiWaveEntity entity, PoseStack poseStack, Matrix4f proj, float[] coreColor, float[] borderColor, float ageInTicks, float alphaMultiplier, boolean orb) {
        float[] outlineColor = entity.getRgbColorOutline();
        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);

        shader.safeGetUniform("blotchMode").set(1.0f);
        shader.safeGetUniform("flameMode").set(1.0f);
        shader.safeGetUniform("orbMode").set(orb ? 1.0f : 0.0f);

        VertexBuffer mesh = KiMeshFactory.getSphereMesh();
        mesh.bind();

        poseStack.pushPose();
        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        shader.safeGetUniform("zCut").set(-1.0f);
        shader.apply();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();
        VertexBuffer.unbind();
        shader.safeGetUniform("blotchMode").set(0.0f);
        shader.safeGetUniform("flameMode").set(0.0f);
        shader.safeGetUniform("orbMode").set(0.0f);

        shader.clear();
    }

    /** Fire-ash flakes shed by the ball; see {@link KiEmberRenderer} for the model. */
    private void renderEmbers(KiWaveEntity entity, PoseStack poseStack, Matrix4f proj, float[] coreColor, float[] borderColor, float ageInTicks, float alphaMultiplier, Vec3 back, float backdraft) {
        KiEmberRenderer.render(poseStack, proj, coreColor, borderColor, entity.getRgbColorOutline(), ageInTicks, alphaMultiplier, back, backdraft, 1.0F);
    }

    private void renderKiCylinderWithShader(KiWaveEntity entity, PoseStack poseStack, Matrix4f proj, float[] coreColor, float[] borderColor, float ageInTicks, float radius, float length, float alphaMultiplier, float cutRadius, float cutRadiusEnd) {
        float[] outlineColor = entity.getRgbColorOutline();
        ShaderInstance shader = DMZShaders.ki3dShader;
        if (shader == null) return;

        shader.safeGetUniform("colorCore").set(coreColor[0], coreColor[1], coreColor[2]);
        shader.safeGetUniform("colorBorder").set(borderColor[0], borderColor[1], borderColor[2]);
        shader.safeGetUniform("colorOutline").set(outlineColor[0], outlineColor[1], outlineColor[2]);
        shader.safeGetUniform("time").set(ageInTicks / 20.0f);
        shader.safeGetUniform("ProjMat").set(proj);
        shader.safeGetUniform("blotchMode").set(0.0f);

        VertexBuffer mesh = KiMeshFactory.getCylinderMesh();
        mesh.bind();

        float zCut = (length > 0.001f) ? Math.min((cutRadius * 0.95f) / length, 0.49f) : -1.0f;
        float zCutFar = (length > 0.001f) ? Math.max(1.0f - (cutRadiusEnd * 0.95f) / length, 0.51f) : 2.0f;

        poseStack.pushPose();
        poseStack.scale(radius, radius, length);

        shader.safeGetUniform("ModelViewMat").set(poseStack.last().pose());
        shader.safeGetUniform("alphaMult").set(1.0f * alphaMultiplier);
        shader.safeGetUniform("zCut").set(zCut);
        shader.safeGetUniform("zCutFar").set(zCutFar);
        shader.apply();
        mesh.drawWithShader(poseStack.last().pose(), proj, shader);
        poseStack.popPose();
        shader.safeGetUniform("zCut").set(-1.0f);
        shader.safeGetUniform("zCutFar").set(2.0f);
        VertexBuffer.unbind();
        shader.clear();
    }

    @Override
    public ResourceLocation getTextureLocation(KiWaveEntity pEntity) {
        return TEXTURE_WAVE_CORE;
    }
}