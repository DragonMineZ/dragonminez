package com.dragonminez.client.init.entities.renderer.sagas.layer;

import com.dragonminez.common.init.MainItems;
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

    private static final float STANDARD_HAND_PIVOT_X = 6.5F;
    private static final float STANDARD_HAND_PIVOT_Y = 14.0F;

    public DMZSagaItemInHandLayer(GeoEntityRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void renderForBone(PoseStack poseStack, DBSagasEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (bone.getName().equals("right_hand_item")) {
            renderHeldItem(poseStack, animatable, bone, animatable.getItemBySlot(EquipmentSlot.MAINHAND), false, renderType, bufferSource, packedLight, packedOverlay);
        } else if (bone.getName().equals("left_hand_item")) {
            renderHeldItem(poseStack, animatable, bone, animatable.getItemBySlot(EquipmentSlot.OFFHAND), true, renderType, bufferSource, packedLight, packedOverlay);
        }
    }

    private void renderHeldItem(PoseStack poseStack, DBSagasEntity animatable, GeoBone bone, ItemStack stack, boolean leftHand, RenderType renderType, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (stack.isEmpty()) return;
        if (animatable.isCasting() && stack.is(MainItems.POWER_POLE.get())) return;

        poseStack.pushPose();

        float standardPivotX = leftHand ? -STANDARD_HAND_PIVOT_X : STANDARD_HAND_PIVOT_X;
        poseStack.translate((bone.getPivotX() - standardPivotX) / 16F, (bone.getPivotY() - STANDARD_HAND_PIVOT_Y) / 16F, bone.getPivotZ() / 16F);

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
        bufferSource.getBuffer(renderType);

        poseStack.popPose();
    }
}
