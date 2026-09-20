package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.shader.EffectBloomRenderer;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.init.entities.ki.SPBlueHurricaneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class SPBlueHurricaneRenderer extends EntityRenderer<SPBlueHurricaneEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    private static final float HEIGHT = 13.0F;
    private static final float GROUND_RADIUS = 6.5F;
    private static final float GROW_TICKS = 10.0F;
    private static final float FADE_TICKS = 12.0F;
    private static final float BLOOM_INTENSITY = 0.6F;
    private static final float FIRST_PERSON_INTENSITY = 0.45F;

    private record Layer(float radiusBase, float radiusTop, float skirt, float spin, float twist, float arms, float bodyAlpha, float heightMult, float seed) {}

    private static final Layer[] LAYERS = {
            new Layer(0.55F, 1.5F, 0.4F, 1.5F, 2.2F, 2.0F, 0.34F, 1.00F, 0.0F),
            new Layer(1.50F, 4.0F, 1.3F, 0.9F, 1.3F, 3.0F, 0.46F, 0.94F, 17.3F),
            new Layer(2.30F, 5.0F, 1.6F, 0.6F, 0.9F, 2.0F, 0.20F, 0.86F, 41.9F)
    };

    public SPBlueHurricaneRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public boolean shouldRender(SPBlueHurricaneEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return frustum.isVisible(entity.getBoundingBox().inflate(7.0D, HEIGHT + 2.0D, 7.0D));
    }

    @Override
    public void render(SPBlueHurricaneEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Matrix4f basePose = new Matrix4f(poseStack.last().pose());

        PlayerEffectQueue.addKiAttack((stack, proj) -> {
            ShaderInstance shader = DMZShaders.blueHurricaneShader;
            if (shader == null) return;

            float age = entity.tickCount + partialTick;
            float castTime = Math.max(1.0F, entity.getCastTime());

            float grow;
            float intensity;
            float spread = 1.0F;

            if (!entity.isFiring()) {
                float charge = Mth.clamp(age / castTime, 0.0F, 1.0F);
                grow = 0.25F + 0.20F * charge;
                intensity = 0.6F * charge;
            } else {
                float active = Math.max(0.0F, age - castTime);
                float in = Mth.clamp(active / GROW_TICKS, 0.0F, 1.0F);
                in = in * in * (3.0F - 2.0F * in);
                float out = Mth.clamp((entity.getFiringTicks() - active) / FADE_TICKS, 0.0F, 1.0F);
                grow = Mth.lerp(in, 0.45F, 1.0F);
                intensity = Mth.lerp(in, 0.6F, 1.0F) * out;
                spread = 1.0F + 0.3F * (1.0F - out);
            }

            Minecraft mc = Minecraft.getInstance();
            Entity owner = entity.getOwner();
            if (mc.options.getCameraType().isFirstPerson() && owner != null && owner == mc.player) intensity *= FIRST_PERSON_INTENSITY;
            boolean bloom = EffectBloomRenderer.bloomPass;
            if (bloom) intensity *= BLOOM_INTENSITY;
            if (intensity <= 0.004F) return;

            float radiusMult = (0.4F + 0.6F * grow) * spread;
            float[] core = entity.getRgbColorMain();
            float[] border = entity.getRgbColorBorder();
            float[] outline = entity.getRgbColorOutline();

            stack.pushPose();
            stack.last().pose().set(basePose);

            if (owner != null) {
                Vec3 anchor = owner.getPosition(partialTick).subtract(entity.getPosition(partialTick));
                stack.translate(anchor.x, anchor.y, anchor.z);
            }

            shader.safeGetUniform("ProjMat").set(proj);
            shader.safeGetUniform("time").set(age / 20.0F);
            shader.safeGetUniform("intensity").set(intensity);
            shader.safeGetUniform("bloomMode").set(bloom ? 1.0F : 0.0F);
            shader.safeGetUniform("swayAmp").set(0.7F * grow);
            shader.safeGetUniform("colorCore").set(core[0], core[1], core[2]);
            shader.safeGetUniform("colorBorder").set(border[0], border[1], border[2]);
            shader.safeGetUniform("colorOutline").set(outline[0], outline[1], outline[2]);

            stack.pushPose();
            stack.translate(0.0D, 0.06D, 0.0D);
            shader.safeGetUniform("modelMatrix").set(stack.last().pose());
            shader.safeGetUniform("mode").set(1.0F);
            shader.safeGetUniform("seed").set(5.0F);
            shader.safeGetUniform("radiusBase").set(GROUND_RADIUS * radiusMult);
            shader.safeGetUniform("spin").set(0.9F);
            shader.safeGetUniform("arms").set(3.0F);
            shader.safeGetUniform("bodyAlpha").set(0.45F);

            VertexBuffer ground = AuraMeshFactory.getGroundQuad();
            ground.bind();
            ground.drawWithShader(stack.last().pose(), proj, shader);
            VertexBuffer.unbind();
            stack.popPose();

            stack.pushPose();
            stack.translate(0.0D, -0.2D, 0.0D);
            shader.safeGetUniform("modelMatrix").set(stack.last().pose());
            shader.safeGetUniform("mode").set(0.0F);

            VertexBuffer mesh = AuraMeshFactory.getTornadoMesh();
            mesh.bind();
            for (Layer layer : LAYERS) {
                shader.safeGetUniform("radiusBase").set(layer.radiusBase() * radiusMult);
                shader.safeGetUniform("radiusTop").set(layer.radiusTop() * radiusMult);
                shader.safeGetUniform("skirt").set(layer.skirt() * radiusMult);
                shader.safeGetUniform("spin").set(layer.spin());
                shader.safeGetUniform("twist").set(layer.twist());
                shader.safeGetUniform("arms").set(layer.arms());
                shader.safeGetUniform("bodyAlpha").set(layer.bodyAlpha());
                shader.safeGetUniform("height").set(HEIGHT * grow * layer.heightMult());
                shader.safeGetUniform("seed").set(layer.seed());
                mesh.drawWithShader(stack.last().pose(), proj, shader);
            }
            VertexBuffer.unbind();
            stack.popPose();

            shader.clear();
            stack.popPose();
        });
    }

    @Override
    public ResourceLocation getTextureLocation(SPBlueHurricaneEntity pEntity) {
        return TEXTURE;
    }
}
