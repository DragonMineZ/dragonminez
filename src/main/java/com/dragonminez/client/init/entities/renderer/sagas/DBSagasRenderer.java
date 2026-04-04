package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.sagas.DBSagaModel;
import com.dragonminez.client.init.entities.renderer.sagas.layer.ItemInHandLayer;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.init.entities.sagas.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Random;

public class DBSagasRenderer<T extends DBSagasEntity> extends GeoEntityRenderer<T> {

    private static final ResourceLocation NAPPA_NORMAL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/saga_nappa.png");
    private static final ResourceLocation NAPPA_DAMAGED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/saga_nappa2.png");

    public DBSagasRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DBSagaModel<>());
        this.shadowRadius = 0.4f;

        this.addRenderLayer(new ItemInHandLayer(this));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        if (animatable instanceof ShadowDummyEntity) {
            return RenderType.entityTranslucent(texture);
        }
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        if (entity instanceof SagaFriezaEntity.SagaFriezaFirstForm) {
            poseStack.scale(0.8f, 0.8f, 0.8f);
        } else if(entity instanceof SagaFriezaEntity.SagaFriezaSecondForm){
            poseStack.scale(1.2f, 1.2f, 1.2f);
        } else if(entity instanceof SagaFriezaEntity.SagaFriezaThirdForm){
            poseStack.scale(1.3f, 1.3f, 1.3f);
        } else if(entity instanceof SagaCellEntity.SagaSemiPerfectCellEntity){
            poseStack.scale(1.2f, 1.2f, 1.2f);
        } else if(entity instanceof SagaVegetaEntity.SagaVegetaMidSSG2Entity){
            poseStack.scale(1.1f, 1.0f, 1.1f);
        } else if(entity instanceof SagaTrunksEntity.SagaFutureTrunksSSG3Entity){
            poseStack.scale(1.3f, 1.1f, 1.3f);
        } else if(entity instanceof SagaTrunksEntity.SagaFutureTrunksKidBaseEntity || entity instanceof SagaTrunksEntity.SagaFutureTrunksKidSSJEntity
                || entity instanceof SagaGohanEntity.SagaGohanMidBaseEntity || entity instanceof SagaGohanEntity.SagaGohanMidSSJEntity  || entity instanceof SagaGohanEntity.SagaGohanMidSSJ2Entity){
            poseStack.scale(0.8f, 0.8f, 0.8f);
        } else if(entity instanceof SagaCellEntity.SagaCellJREntity){
            poseStack.scale(0.8f, 0.8f, 0.8f);
        } else if(entity instanceof SagaZFightersEntity.SagaKrillinEntity){
            poseStack.scale(0.8f, 0.8f, 0.8f);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);


        boolean showAura = entity.isTransforming() || entity.isCharge();
        boolean showLightning = entity.isLightning();

        if (showAura || showLightning) {
            Minecraft mc = Minecraft.getInstance();

            poseStack.pushPose();

            if (showAura) {
                if (entity.onGround()) {
                    poseStack.pushPose();
                    poseStack.translate(0.0, 0.05, 0.0);
                    renderPulseAura(entity, poseStack, mc, partialTick);
                    poseStack.popPose();
                }

                poseStack.pushPose();
                executeAuraShaderDraw(entity, poseStack, mc, partialTick);
                poseStack.popPose();
            }

            if (showLightning) {
                poseStack.pushPose();
                executeLightningShaderDraw(entity, poseStack, partialTick);
                poseStack.popPose();
            }

            poseStack.popPose();
        }

        poseStack.popPose();

    }

    @Override
    public ResourceLocation getTextureLocation(T animatable) {

        if (animatable instanceof SagaNappaEntity nappa) {
            if (nappa.isBattleDamaged()) {
                return NAPPA_DAMAGED;
            }
            return NAPPA_NORMAL;

        }

        return super.getTextureLocation(animatable);
    }


    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {

        if (animatable instanceof ShadowDummyEntity) {
            return Color.ofRGBA(1.0f, 1.0f, 1.0f, 0.7f);
        }

        return super.getRenderColor(animatable, partialTick, packedLight);
    }

    private void renderPulseAura(T animatable, PoseStack poseStack, Minecraft mc, float partialTick) {
        float time = (animatable.tickCount + partialTick) * 0.02f;
        float progress1 = time % 1.0f;
        float progress2 = (progress1 + 0.5f) % 1.0f;

        drawSinglePulse(animatable, poseStack, mc, partialTick, progress1);
        drawSinglePulse(animatable, poseStack, mc, partialTick, progress2);
    }

    private void drawSinglePulse(T animatable, PoseStack poseStack, Minecraft mc, float partialTick, float progress) {
        float expansion = 1.0f + (6.0f * progress);
        float alphaCurve = (float) Math.sin(progress * Math.PI);

        String auraType = animatable.getAuraType() != null && !animatable.getAuraType().isEmpty() ? animatable.getAuraType().toLowerCase() : "kakarot";
        ResourceLocation crossTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + auraType + "_cross.png");

        ShaderInstance shader = DMZShaders.auraShader;
        if (shader == null) return;
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

        poseStack.pushPose();

        float cameraPitch = mc.gameRenderer.getMainCamera().getXRot();
        if (cameraPitch < 0.0f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        }

        float scaleMultiplier = 0.7f;
        float sX = expansion * scaleMultiplier;
        float sZ = expansion * scaleMultiplier;

        poseStack.scale(sX, 1.0f, sZ);

        float animSpeed = (animatable.tickCount + partialTick) * 0.5f;
        shader.safeGetUniform("speed").set(animSpeed);
        shader.safeGetUniform("ProjMat").set(projectionMatrix);
        shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

        float[] color = ColorUtils.rgbIntToFloat(animatable.getAuraColor());
        shader.safeGetUniform("color1").set(color[0] * 1.6f, color[1] * 1.6f, color[2] * 1.6f, 1.0f);
        shader.safeGetUniform("color2").set(color[0] * 1.3f, color[1] * 1.3f, color[2] * 1.3f, 1.0f);
        shader.safeGetUniform("color3").set(color[0] * 1.0f, color[1] * 1.0f, color[2] * 1.0f, 0.85f);
        shader.safeGetUniform("color4").set(color[0] * 0.75f, color[1] * 0.75f, color[2] * 0.75f, 0.65f);

        shader.safeGetUniform("alp1").set(alphaCurve * 0.6f);

        RenderType pulseRender = ModRenderTypes.getCustomAura(crossTex);
        pulseRender.setupRenderState();

        shader.apply();

        VertexBuffer mesh = AuraMeshFactory.getGroundQuad();
        mesh.bind();
        mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);

        pulseRender.clearRenderState();
        VertexBuffer.unbind();
        shader.clear();

        poseStack.popPose();
    }

    private void executeAuraShaderDraw(T animatable, PoseStack poseStack, Minecraft mc, float partialTick) {
        ShaderInstance shader = DMZShaders.auraShader;
        if (shader == null) return;

        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

        String auraType = animatable.getAuraType() != null && !animatable.getAuraType().isEmpty() ? animatable.getAuraType().toLowerCase() : "kakarot";
        ResourceLocation mainTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + auraType + "_aura.png");
        ResourceLocation crossTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + auraType + "_cross.png");
        ResourceLocation sparkingTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/sparking_effects.png");

        float animSpeed = (animatable.tickCount + partialTick) * 0.5f;
        shader.safeGetUniform("speed").set(animSpeed);
        shader.safeGetUniform("ProjMat").set(projectionMatrix);

        float[] color = ColorUtils.rgbIntToFloat(animatable.getAuraColor());

        shader.safeGetUniform("color1").set(color[0] * 1.6f, color[1] * 1.6f, color[2] * 1.6f, 1.0f);
        shader.safeGetUniform("color2").set(color[0] * 1.3f, color[1] * 1.3f, color[2] * 1.3f, 1.0f);
        shader.safeGetUniform("color3").set(color[0] * 1.0f, color[1] * 1.0f, color[2] * 1.0f, 0.85f);
        shader.safeGetUniform("color4").set(color[0] * 0.75f, color[1] * 0.75f, color[2] * 0.75f, 0.65f);

        float cameraPitch = mc.gameRenderer.getMainCamera().getXRot();
        float cameraYaw = mc.gameRenderer.getMainCamera().getYRot();
        float absPitch = Math.abs(cameraPitch);
        float crossFactor = 0.0f;
        float pitchSquash = 1.0f;

        if (absPitch > 45.0f) {
            crossFactor = (float) Math.pow((absPitch - 45.0f) / 45.0f, 2.0);
            pitchSquash = 1.0f - (crossFactor * 0.5f);
        }

        float scaleMultiplier = 2.2f;
        VertexBuffer mesh = AuraMeshFactory.getBillboardQuad();

        if (crossFactor < 1.0f) {
            poseStack.pushPose();

            poseStack.translate(0.0, animatable.getBbHeight() / 2.0f + 0.8f, 0.0);

            poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));

            poseStack.scale(scaleMultiplier, scaleMultiplier * pitchSquash, scaleMultiplier);

            shader.safeGetUniform("alp1").set(1.0f - crossFactor);
            shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

            RenderType mainRender = ModRenderTypes.getCustomAura(mainTex);
            mainRender.setupRenderState();
            shader.apply();
            mesh.bind();
            mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            mainRender.clearRenderState();

            poseStack.pushPose();
            float sparkingPulse = 1.0f + (float) Math.sin((animatable.tickCount + partialTick) * 0.2f) * 0.05f;
            poseStack.scale(0.8f * sparkingPulse, 0.65f * sparkingPulse, 0.8f * sparkingPulse);
            poseStack.translate(0.0, -0.25, 0.0);

            RenderType sparkingRender = ModRenderTypes.getCustomAura(sparkingTex);
            sparkingRender.setupRenderState();
            shader.safeGetUniform("alp1").set((1.0f - crossFactor) * 0.8f);
            shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
            shader.apply();
            mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            sparkingRender.clearRenderState();
            poseStack.popPose();

            poseStack.popPose();
        }

        if (crossFactor > 0.0f) {
            poseStack.pushPose();

            poseStack.translate(0.0, 0.05, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));

            if (cameraPitch < 0.0f) {
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }

            poseStack.scale(scaleMultiplier, 1.0f, scaleMultiplier);

            shader.safeGetUniform("alp1").set(crossFactor);
            shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());

            RenderType crossRender = ModRenderTypes.getCustomAura(crossTex);
            crossRender.setupRenderState();
            shader.apply();

            VertexBuffer groundMesh = AuraMeshFactory.getGroundQuad();
            groundMesh.bind();
            groundMesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            crossRender.clearRenderState();

            poseStack.popPose();
        }

        VertexBuffer.unbind();
        shader.clear();
    }

    private void executeLightningShaderDraw(T animatable, PoseStack poseStack, float partialTick) {
        ShaderInstance shader = DMZShaders.lightningShader;
        if (shader == null) return;

        boolean isAuraActive = animatable.isCharge() || animatable.isTransforming();
        float speedMod = isAuraActive ? 1.0f : 0.20f;
        int maxBranches = isAuraActive ? 5 : 3;
        float maxScale = isAuraActive ? 0.5f : 0.25f;

        float[] colorRgb = ColorUtils.rgbIntToFloat(animatable.getLightningColor());
        float time = (animatable.tickCount + partialTick) / 20.0f;
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

        shader.safeGetUniform("projectionMatrix").set(projectionMatrix);
        shader.safeGetUniform("time").set(time);
        shader.safeGetUniform("speedModifier").set(speedMod);

        shader.safeGetUniform("color1").set(
                Mth.lerp(0.8f, colorRgb[0], 1.0f),
                Mth.lerp(0.8f, colorRgb[1], 1.0f),
                Mth.lerp(0.8f, colorRgb[2], 1.0f)
        );
        shader.safeGetUniform("color2").set(colorRgb[0], colorRgb[1], colorRgb[2]);
        shader.safeGetUniform("alp1").set(1.0f);
        shader.safeGetUniform("alp2").set(0.1f);
        shader.safeGetUniform("power").set(3.0f);
        shader.safeGetUniform("divis").set(1.0f);

        RenderType renderType = ModRenderTypes.getCustomLightning(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png"));
        renderType.setupRenderState();

        shader.apply();
        VertexBuffer mesh = AuraRenderer.getLightningMesh();
        mesh.bind();

        long timeHash = animatable.level().getGameTime() / 2L;
        Random seededRand = new Random(animatable.getId() + timeHash);
        float bbHeight = animatable.getBbHeight() - 0.6f;

        for (int i = 0; i < maxBranches; i++) {
            poseStack.pushPose();

            float spread = isAuraActive ? 1.8f : 1.2f;
            float randomY = seededRand.nextFloat() * bbHeight;

            poseStack.translate((seededRand.nextFloat() - 0.5f) * spread, randomY, (seededRand.nextFloat() - 0.5f) * spread);
            poseStack.mulPose(Axis.YP.rotationDegrees(seededRand.nextFloat() * 360));

            float scale = 0.15f + seededRand.nextFloat() * maxScale;
            poseStack.scale(scale, scale, scale);

            shader.safeGetUniform("modelMatrix").set(poseStack.last().pose());
            shader.safeGetUniform("normalMatrix").set(new Matrix4f(new Matrix3f(poseStack.last().normal())));
            shader.apply();

            mesh.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            poseStack.popPose();
        }

        VertexBuffer.unbind();
        shader.clear();
        renderType.clearRenderState();
    }
}
