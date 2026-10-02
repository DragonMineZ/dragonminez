package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class TamagamiRenderers {

    private TamagamiRenderers() {}

    public static class HammerRenderer extends EntityRenderer<AllWorldBossesEntity.TamagamiHammer> {

        private static final float SPIN_DEGREES_PER_TICK = 42.0F;
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

            float yaw = Mth.rotLerp(partialTick, hammer.yRotO, hammer.getYRot());
            float spin = (hammer.tickCount + partialTick) * SPIN_DEGREES_PER_TICK;

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
}
