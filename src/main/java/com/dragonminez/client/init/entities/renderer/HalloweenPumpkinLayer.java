package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.render.hair.HairColliders;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.util.HalloweenEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class HalloweenPumpkinLayer<T extends Entity & GeoAnimatable> extends GeoRenderLayer<T> {

    private static final float PUMPKIN_OVERSIZE = 1.2f;
    private static final float[] DEFAULT_HEAD_BOUNDS = {-0.25f, 1.5f, -0.25f, 0.25f, 2.0f, 0.25f};

    public HalloweenPumpkinLayer(GeoRenderer<T> geoRenderer) {
        super(geoRenderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        if (!"head".equals(bone.getName()) || bone.isHidden()) return;
        if (!HalloweenEvent.isActive() || animatable.isInvisible()) return;
        if (animatable instanceof QuestNPCEntity npc && !npc.isMasterNpc()) return;

        float[] b = HairColliders.boundsOf(bone);
        if (b == null) b = DEFAULT_HEAD_BOUNDS;
        float size = Math.max(b[3] - b[0], b[5] - b[2]) * PUMPKIN_OVERSIZE;

        BlockState pumpkin = Blocks.CARVED_PUMPKIN.defaultBlockState();

        poseStack.pushPose();
        poseStack.translate((b[0] + b[3]) * 0.5f - size * 0.5f, (b[1] + b[4]) * 0.5f - size * 0.5f, (b[2] + b[5]) * 0.5f - size * 0.5f);
        poseStack.scale(size, size, size);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(pumpkin, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        bufferSource.getBuffer(renderType);
    }
}
