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

public class ItemInHandLayer<T extends DBSagasEntity> extends GeoRenderLayer<T> {

    public ItemInHandLayer(GeoEntityRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void renderForBone(PoseStack poseStack, DBSagasEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (bone.getName().equals("right_item_hand")) {
            ItemStack mainHandItem = animatable.getItemBySlot(EquipmentSlot.MAINHAND);

            if (!mainHandItem.isEmpty()) {
                poseStack.pushPose();

                poseStack.mulPose(Axis.XP.rotationDegrees(-90f));

                poseStack.mulPose(Axis.YP.rotationDegrees(0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(0f));

                poseStack.translate(0.4D, 0.1D, 0.73D);

                Minecraft.getInstance().getItemRenderer().renderStatic(
                        mainHandItem,
                        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        bufferSource,
                        animatable.level(),
                        animatable.getId()
                );

                poseStack.popPose();
            }
        }
    }
}