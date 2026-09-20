package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DMZSagaItemInHandLayer<T extends DBSagasEntity> extends GeoRenderLayer<T> {

    public DMZSagaItemInHandLayer(GeoEntityRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void renderForBone(PoseStack poseStack, DBSagasEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (bone.getName().equals("right_hand_item")) {
            renderHeldItem(poseStack, animatable, animatable.getItemBySlot(EquipmentSlot.MAINHAND), false, renderType, bufferSource, packedLight, packedOverlay);
        } else if (bone.getName().equals("left_hand_item")) {
            renderHeldItem(poseStack, animatable, animatable.getItemBySlot(EquipmentSlot.OFFHAND), true, renderType, bufferSource, packedLight, packedOverlay);
        }
    }

    private void renderHeldItem(PoseStack poseStack, DBSagasEntity animatable, ItemStack stack, boolean leftHand, RenderType renderType, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (stack.isEmpty()) return;

        poseStack.pushPose();

        poseStack.mulPose(Axis.XP.rotationDegrees(-90f));

        poseStack.translate(leftHand ? -0.4D : 0.4D, 0.1D, 0.73D);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                animatable,
                stack,
                leftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                leftHand,
                poseStack,
                bufferSource,
                animatable.level(),
                packedLight,
                packedOverlay,
                animatable.getId()
        );

        // Rendering the item above switches the shared BufferBuilder to the item's
        // render type and ends the entity's buffer mid-recursion. Re-fetch the entity
        // buffer so GeckoLib's remaining bones keep writing into a valid, set-up buffer.
        // Without this, strict GPU drivers (AMD/Intel) render the entity as garbage.
        // This mirrors GeckoLib's own BlockAndItemGeoLayer / DMZPlayerItemInHandLayer.
        bufferSource.getBuffer(renderType);

        poseStack.popPose();
    }
}
