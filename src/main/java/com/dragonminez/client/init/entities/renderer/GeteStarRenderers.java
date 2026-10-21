package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.worldboss.GeteStarEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class GeteStarRenderers {

    private GeteStarRenderers() {}

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Vec3 pos, float u, float v,
                               int light, Vec3 n) {
        consumer.vertex(pose, (float) pos.x, (float) pos.y, (float) pos.z).color(1.0F, 1.0F, 1.0F, 1.0F).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, (float) n.x, (float) n.y, (float) n.z).endVertex();
    }

    public static class BindRenderer extends EntityRenderer<GeteStarEntities.Bind> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/gete_cable.png");
        private static final int SEGMENTS = 10;
        private static final double HALF_WIDTH = 0.16D;
        private static final double ANCHOR_SIDE = 2.4D;
        private static final double ANCHOR_DEPTH = 1.8D;
        private static final double ARM_REACH = 0.8D;
        private static final float SCROLL_SPEED = 0.12F;

        public BindRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public boolean shouldRender(GeteStarEntities.Bind bind, Frustum frustum, double camX, double camY, double camZ) {
            return frustum.isVisible(new AABB(bind.position(), bind.position()).inflate(4.0D, 0.0D, 4.0D).expandTowards(0.0D, 4.0D, 0.0D));
        }

        @Override
        public void render(GeteStarEntities.Bind bind, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
            float age = bind.tickCount + partialTick;
            float progress = bind.isRetracting()
                    ? 1.0F - Mth.clamp((age - bind.retractAt()) / GeteStarEntities.Bind.RETRACT_TICKS, 0.0F, 1.0F)
                    : Mth.clamp(age / GeteStarEntities.Bind.EMERGE_TICKS, 0.0F, 1.0F);
            if (progress <= 0.01F) return;

            Vec3 origin = bind.getPosition(partialTick);
            float facing = bind.getYRot() * Mth.DEG_TO_RAD;
            Vec3 forward = new Vec3(-Mth.sin(facing), 0.0D, Mth.cos(facing));
            Vec3 right = new Vec3(-Mth.cos(facing), 0.0D, -Mth.sin(facing));

            Vec3[] anchors = {
                    right.scale(ANCHOR_SIDE).add(forward.scale(ANCHOR_DEPTH)),
                    right.scale(-ANCHOR_SIDE).add(forward.scale(ANCHOR_DEPTH)),
                    right.scale(ANCHOR_SIDE * 0.7D).add(forward.scale(-ANCHOR_DEPTH)),
                    right.scale(-ANCHOR_SIDE * 0.7D).add(forward.scale(-ANCHOR_DEPTH))
            };
            Vec3[] targets = this.targets(bind, origin, right, partialTick);

            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            PoseStack.Pose last = poseStack.last();
            float scroll = age * SCROLL_SPEED;
            for (int i = 0; i < anchors.length; i++) {
                Vec3 from = anchors[i];
                Vec3 to = targets[i];
                Vec3 outward = new Vec3(from.x, 0.0D, from.z).normalize();
                Vec3 control = from.add(to).scale(0.5D).add(outward.scale(0.7D)).add(0.0D, 0.4D, 0.0D);
                Vec3 previous = from;
                double travelled = 0.0D;
                for (int s = 1; s <= SEGMENTS; s++) {
                    double t = progress * s / (double) SEGMENTS;
                    double u = 1.0D - t;
                    Vec3 current = from.scale(u * u).add(control.scale(2.0D * u * t)).add(to.scale(t * t));
                    double step = current.distanceTo(previous);
                    this.tube(consumer, last.pose(), last.normal(), previous, current,
                            (float) (travelled - scroll), (float) (travelled + step - scroll), packedLight);
                    travelled += step;
                    previous = current;
                }
            }
        }

        private Vec3[] targets(GeteStarEntities.Bind bind, Vec3 origin, Vec3 right, float partialTick) {
            Entity entity = bind.level().getEntity(bind.victimId());
            if (entity instanceof LivingEntity victim) {
                Vec3 base = victim.getPosition(partialTick).subtract(origin);
                double height = victim.getBbHeight();
                double arm = victim.getBbWidth() * 0.5D + ARM_REACH;
                Vec3 hands = base.add(0.0D, height * 0.78D, 0.0D);
                Vec3 ankles = base.add(0.0D, 0.15D, 0.0D);
                return new Vec3[] {hands.add(right.scale(arm)), hands.add(right.scale(-arm)), ankles.add(right.scale(0.2D)), ankles.add(right.scale(-0.2D))};
            }
            Vec3 up = new Vec3(0.0D, 2.6D, 0.0D);
            return new Vec3[] {up.add(right.scale(0.5D)), up.add(right.scale(-0.5D)), up.add(0.0D, -0.6D, 0.0D).add(right.scale(0.3D)),
                    up.add(0.0D, -0.6D, 0.0D).add(right.scale(-0.3D))};
        }

        private void tube(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Vec3 from, Vec3 to, float v0, float v1, int light) {
            Vec3 dir = to.subtract(from);
            if (dir.lengthSqr() < 1.0E-6D) return;
            dir = dir.normalize();
            Vec3 up = Math.abs(dir.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 a = dir.cross(up).normalize().scale(HALF_WIDTH);
            Vec3 b = dir.cross(a).normalize().scale(HALF_WIDTH);
            Vec3[] corners = {a.add(b), b.subtract(a), a.scale(-1.0D).subtract(b), a.subtract(b)};
            for (int side = 0; side < 4; side++) {
                Vec3 c0 = corners[side];
                Vec3 c1 = corners[(side + 1) % 4];
                Vec3 n = c0.add(c1).normalize();
                float u0 = side * 0.25F;
                float u1 = u0 + 0.25F;
                vertex(consumer, pose, normal, from.add(c0), u0, v0, light, n);
                vertex(consumer, pose, normal, from.add(c1), u1, v0, light, n);
                vertex(consumer, pose, normal, to.add(c1), u1, v1, light, n);
                vertex(consumer, pose, normal, to.add(c0), u0, v1, light, n);
            }
        }

        @Override
        public ResourceLocation getTextureLocation(GeteStarEntities.Bind bind) {
            return TEXTURE;
        }
    }

    public static class ScrapRenderer extends EntityRenderer<GeteStarEntities.Scrap> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/gete_scrap.png");
        private static final int PIECES = 9;

        private final ItemRenderer itemRenderer;

        public ScrapRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.itemRenderer = context.getItemRenderer();
        }

        @Override
        public boolean shouldRender(GeteStarEntities.Scrap scrap, Frustum frustum, double camX, double camY, double camZ) {
            return frustum.isVisible(new AABB(scrap.sphereCenter(), scrap.sphereCenter()).inflate(GeteStarEntities.Scrap.SPHERE_RADIUS + 1.0D));
        }

        @Override
        public void render(GeteStarEntities.Scrap scrap, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
            float progress = scrap.progress(partialTick);
            float age = scrap.tickCount + partialTick;
            double orbit = GeteStarEntities.Scrap.SPHERE_RADIUS * (1.0D - 0.9D * progress * progress);
            float scale = 0.45F + 0.35F * progress;
            ItemStack stack = new ItemStack(MainItems.GETE_SCRAP.get());
            int seed = scrap.getId();
            for (int i = 0; i < PIECES; i++) {
                poseStack.pushPose();
                double phase = i * (Math.PI * 2.0D / PIECES) + seed;
                double tilt = Math.sin(phase * 1.7D) * 0.8D;
                double angle = phase + age * (0.05D + 0.1D * progress);
                poseStack.translate(Math.cos(angle) * orbit * Math.cos(tilt), GeteStarEntities.Scrap.SPHERE_LIFT + Math.sin(tilt) * orbit,
                        Math.sin(angle) * orbit * Math.cos(tilt));
                poseStack.mulPose(Axis.YP.rotationDegrees(age * 9.0F + i * 40.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(age * 6.0F + i * 23.0F));
                poseStack.scale(scale, scale, scale);
                this.itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack,
                        buffer, scrap.level(), seed + i);
                poseStack.popPose();
            }
        }

        @Override
        public ResourceLocation getTextureLocation(GeteStarEntities.Scrap scrap) {
            return TEXTURE;
        }
    }
}
