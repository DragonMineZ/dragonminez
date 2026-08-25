package com.dragonminez.client.init.entities.renderer.rr.layer;

import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
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

public class DMZRedRibbonItemInHandLayer<T extends RedRibbonEntity> extends GeoRenderLayer<T> {

    public DMZRedRibbonItemInHandLayer(GeoEntityRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void renderForBone(PoseStack poseStack, RedRibbonEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (bone.getName().equals("right_hand_item")) {
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

                // Rendering the item above switches the shared BufferBuilder to the item's
                // render type and ends the entity's buffer mid-recursion. Re-fetch the entity
                // buffer so GeckoLib's remaining bones keep writing into a valid, set-up buffer.
                // Without this, strict GPU drivers (AMD/Intel) render the entity as garbage.
                // This mirrors GeckoLib's own BlockAndItemGeoLayer / DMZPlayerItemInHandLayer.
                bufferSource.getBuffer(renderType);

                poseStack.popPose();
            }
        }
    }
}