package com.dragonminez.client.render.layer.base;

import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.data.DMZAnimatable;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.function.BiFunction;

public class BlockAndItemLayer extends GeoRenderLayer<DMZAnimatable> {

    protected final BiFunction<GeoBone, DMZAnimatable, ItemStack> stackForBone;
    protected final BiFunction<GeoBone, DMZAnimatable, BlockState> blockForBone;

    public BlockAndItemLayer(GeoRenderer<DMZAnimatable> renderer) {
        this(renderer, (bone, animatable) -> null,
                (bone, animatable) -> null);
    }

    public BlockAndItemLayer(GeoRenderer<DMZAnimatable> renderer,
                             BiFunction<GeoBone, DMZAnimatable, ItemStack> stackForBone,
                             BiFunction<GeoBone, DMZAnimatable, BlockState> blockForBone) {
        super(renderer);
        this.stackForBone = stackForBone;
        this.blockForBone = blockForBone;
    }

    /**
     * Return an ItemStack relevant to this bone for rendering, or null if no ItemStack to render
     */
    protected ItemStack getStackForBone(GeoBone bone, DMZAnimatable animatable) {
        return this.stackForBone.apply(bone, animatable);
    }

    /**
     * Return a BlockState relevant to this bone for rendering, or null if no BlockState to render
     */
    protected BlockState getBlockForBone(GeoBone bone, DMZAnimatable animatable) {
        return this.blockForBone.apply(bone, animatable);
    }

    /**
     * Return a specific TransFormType for this {@link ItemStack} render for this bone.
     */
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack,
                                                          DMZAnimatable animatable) {
        return ItemDisplayContext.NONE;
    }

    /**
     * This method is called by the {@link GeoRenderer} for each bone being rendered.<br>
     * This is a more expensive call, particularly if being used to render something on a different buffer.<br>
     * It does however have the benefit of having the matrix translations and other transformations already applied from render-time.<br>
     * It's recommended to avoid using this unless necessary.<br>
     * <br>
     * The {@link GeoBone} in question has already been rendered by this stage.<br>
     * <br>
     * If you <i>do</i> use it, and you render something that changes the {@link VertexConsumer buffer}, you need to reset it back to the previous buffer
     * using {@link MultiBufferSource#getBuffer} before ending the method
     */
    @Override
    public void renderForBone(PoseStack poseStack, DMZAnimatable animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource,
                              VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        ItemStack stack = getStackForBone(bone, animatable);
        BlockState blockState = getBlockForBone(bone, animatable);

        if (stack == null && blockState == null)
            return;

        poseStack.pushPose();
        RenderUtils.translateAndRotateMatrixForBone(poseStack, bone);

        if (stack != null)
            renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight,
                    packedOverlay);

        if (blockState != null)
            renderBlockForBone(poseStack, bone, blockState, animatable, bufferSource, partialTick, packedLight,
                    packedOverlay);

        buffer = bufferSource.getBuffer(renderType);

        poseStack.popPose();
    }

    /**
     * Render the given {@link ItemStack} for the provided {@link GeoBone}.
     */
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, DMZAnimatable animatable,
                                      MultiBufferSource bufferSource,
                                      float partialTick, int packedLight, int packedOverlay) {
        if (!(this.renderer instanceof DMZPlayerRenderer dmzPlayerRenderer)) {
            return;
        }

        final AbstractClientPlayer livingEntity = dmzPlayerRenderer.getCurrentEntity();
        Minecraft.getInstance().getItemRenderer().renderStatic(livingEntity, stack,
                getTransformTypeForStack(bone, stack, animatable), false, poseStack, bufferSource,
                livingEntity.level(),
                packedLight, packedOverlay, livingEntity.getId());
    }

    /**
     * Render the given {@link BlockState} for the provided {@link GeoBone}.
     */
    protected void renderBlockForBone(PoseStack poseStack, GeoBone bone, BlockState state, DMZAnimatable animatable,
                                      MultiBufferSource bufferSource,
                                      float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(-0.25f, -0.25f, -0.25f);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, packedLight,
                packedOverlay, ModelData.EMPTY, null);
        poseStack.popPose();
    }
}
