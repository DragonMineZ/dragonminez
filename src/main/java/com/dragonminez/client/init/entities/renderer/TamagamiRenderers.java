package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.render.effects.AuraTrailRenderer;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class TamagamiRenderers {

    private TamagamiRenderers() {}

    public static class HammerRenderer extends EntityRenderer<AllWorldBossesEntity.TamagamiHammer> {

        private static final float SPIN_DEGREES_PER_TICK = 42.0F;
        private static final float[] TRAIL_COLOR = ColorUtils.rgbIntToFloat(0xFFFFFF);
        private static final float[] POWERED_TRAIL_COLOR = ColorUtils.rgbIntToFloat(0xFF8C1A);
        private static final float TRAIL_ALPHA = 0.55F;
        private static final int TRAIL_SAMPLES = 20;
        private static final float TRAIL_HALF_WIDTH = 0.7F;
        private static final float SCALE = AllWorldBossesEntity.Tamagami.SCALE;
        private static final double PIVOT_Y = 1.875D;

        private final ItemRenderer itemRenderer;
        private ItemStack stack;

        public HammerRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.itemRenderer = context.getItemRenderer();
        }

        @Override
        public void render(AllWorldBossesEntity.TamagamiHammer hammer, float entityYaw, float partialTick, PoseStack poseStack,
                           MultiBufferSource buffer, int packedLight) {
            if (this.stack == null) this.stack = new ItemStack(MainItems.TAMAGAMI_HAMMER.get());
            AuraTrailRenderer.submitEntityTrail(hammer, poseStack.last().pose(), partialTick,
                    hammer.isOwnerPowered() ? POWERED_TRAIL_COLOR : TRAIL_COLOR, TRAIL_ALPHA, TRAIL_SAMPLES, TRAIL_HALF_WIDTH, 0.0F,
                    hammer.getBbHeight() * 0.5F);

            float yaw = Mth.rotLerp(partialTick, hammer.yRotO, hammer.getYRot());
            float spin = hammer.getSpin(partialTick) * SPIN_DEGREES_PER_TICK;

            poseStack.pushPose();
            poseStack.translate(0.0D, hammer.getBbHeight() * 0.5D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
            poseStack.scale(SCALE, SCALE, SCALE);
            poseStack.translate(0.0D, -PIVOT_Y, 0.0D);
            this.itemRenderer.renderStatic(this.stack, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                    hammer.level(), hammer.getId());
            poseStack.popPose();

            super.render(hammer, entityYaw, partialTick, poseStack, buffer, packedLight);
        }

        @Override
        public ResourceLocation getTextureLocation(AllWorldBossesEntity.TamagamiHammer hammer) {
            return InventoryMenu.BLOCK_ATLAS;
        }
    }

    public static class PillarRenderer extends EntityRenderer<AllWorldBossesEntity.TamagamiPillar> {

        private static final float LAYER_HEIGHT = 1.25F;
        private static final float TAPER = 0.55F;
        private static final int TIP_PIECES = 3;
        private static final int BASE_SHARDS = 6;

        private final BlockRenderDispatcher blocks;

        public PillarRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.blocks = context.getBlockRenderDispatcher();
        }

        @Override
        public boolean shouldRender(AllWorldBossesEntity.TamagamiPillar pillar, Frustum frustum, double camX, double camY, double camZ) {
            return pillar.shouldRender(camX, camY, camZ);
        }

        @Override
        public void render(AllWorldBossesEntity.TamagamiPillar pillar, float entityYaw, float partialTick, PoseStack poseStack,
                           MultiBufferSource buffer, int packedLight) {
            float emergence = pillar.getEmergence(partialTick);
            if (emergence <= 0.0F) return;

            BlockState state = pillar.getPillarState();
            float height = pillar.getPillarHeight();
            float radius = pillar.getPillarRadius();
            float sunk = (1.0F - emergence) * height;
            RandomSource random = RandomSource.create(pillar.getId() * 341873128712L);

            poseStack.pushPose();
            poseStack.translate(0.0D, -sunk, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(pillar.getYRot()));

            int layers = Math.max(1, Mth.ceil(height - LAYER_HEIGHT * 0.5F));
            for (int i = 0; i < layers; i++) {
                float t = i / height;
                float width = radius * 2.0F * (1.0F - TAPER * t);
                float jitterX = (random.nextFloat() - 0.5F) * 0.2F * width;
                float jitterZ = (random.nextFloat() - 0.5F) * 0.2F * width;
                float twist = (random.nextFloat() - 0.5F) * 40.0F;
                int light = this.lightAt(pillar, i - sunk);
                this.piece(poseStack, buffer, state, jitterX, i, jitterZ, width, LAYER_HEIGHT, width, twist, 0.0F, light);

                if (i % 3 == 1) {
                    float angle = random.nextFloat() * 360.0F;
                    float size = width * 0.4F;
                    float tilt = 15.0F + random.nextFloat() * 25.0F;
                    this.shard(poseStack, buffer, state, angle, width * 0.45F, i, size, size * 1.4F, tilt, light);
                }
            }

            float top = layers;
            float tipWidth = radius * 2.0F * (1.0F - TAPER);
            for (int i = 0; i < TIP_PIECES; i++) {
                float scale = 1.0F - (i + 1) / (float) (TIP_PIECES + 1);
                float width = tipWidth * scale;
                float tilt = (random.nextFloat() - 0.5F) * 16.0F;
                this.piece(poseStack, buffer, state, 0.0F, top, 0.0F, width, LAYER_HEIGHT * 0.9F, width, 45.0F + i * 20.0F, tilt,
                        this.lightAt(pillar, top - sunk));
                top += LAYER_HEIGHT * 0.75F;
            }

            for (int i = 0; i < BASE_SHARDS; i++) {
                float angle = i * (360.0F / BASE_SHARDS) + random.nextFloat() * 25.0F;
                float size = radius * (0.55F + random.nextFloat() * 0.3F);
                float tilt = 25.0F + random.nextFloat() * 20.0F;
                this.shard(poseStack, buffer, state, angle, radius * 0.95F, -0.4F, size, size * 1.8F, tilt, this.lightAt(pillar, -sunk));
            }

            poseStack.popPose();
            super.render(pillar, entityYaw, partialTick, poseStack, buffer, packedLight);
        }

        private int lightAt(AllWorldBossesEntity.TamagamiPillar pillar, float offsetY) {
            BlockPos pos = BlockPos.containing(pillar.getX(), pillar.getY() + Math.max(0.0F, offsetY) + 0.5D, pillar.getZ());
            return LevelRenderer.getLightColor(pillar.level(), pos);
        }

        private void shard(PoseStack poseStack, MultiBufferSource buffer, BlockState state, float angle, float distance, float y,
                           float width, float height, float tilt, int light) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(distance, y, 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-tilt));
            this.block(poseStack, buffer, state, width, height, width, light);
            poseStack.popPose();
        }

        private void piece(PoseStack poseStack, MultiBufferSource buffer, BlockState state, float x, float y, float z,
                           float width, float height, float depth, float twist, float tilt, int light) {
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(twist));
            if (tilt != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(tilt));
            this.block(poseStack, buffer, state, width, height, depth, light);
            poseStack.popPose();
        }

        private void block(PoseStack poseStack, MultiBufferSource buffer, BlockState state, float width, float height, float depth, int light) {
            poseStack.pushPose();
            poseStack.scale(width, height, depth);
            poseStack.translate(-0.5D, 0.0D, -0.5D);
            this.blocks.renderSingleBlock(state, poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(AllWorldBossesEntity.TamagamiPillar pillar) {
            return InventoryMenu.BLOCK_ATLAS;
        }
    }
}
