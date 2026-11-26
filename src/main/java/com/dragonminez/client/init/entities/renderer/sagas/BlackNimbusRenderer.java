package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.client.init.entities.model.BlackNimbusModel;
import com.dragonminez.client.init.entities.model.FlyingNimbusModel;
import com.dragonminez.common.init.entities.BlackNimbusEntity;
import com.dragonminez.common.init.entities.FlyingNimbusEntity;
import com.dragonminez.client.render.effects.AuraTrailRenderer;
import com.dragonminez.client.util.ColorUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BlackNimbusRenderer extends GeoEntityRenderer<BlackNimbusEntity> {
    private static final float[] TRAIL_COLOR = ColorUtils.rgbIntToFloat(0x272D67);
    private static final float TRAIL_ALPHA = 0.60f;
    private static final int TRAIL_SAMPLES = 64;
    private static final float TRAIL_HALF_WIDTH = 0.80f;
    private static final float TRAIL_ANCHOR_BACK = 0.70f;
    private static final float TRAIL_ANCHOR_HEIGHT = 0.55f;

    public BlackNimbusRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BlackNimbusModel<>());
    }

    @Override
    public void render(BlackNimbusEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        AuraTrailRenderer.submitEntityTrail(entity, poseStack.last().pose(), partialTick, TRAIL_COLOR, TRAIL_ALPHA,
                TRAIL_SAMPLES, TRAIL_HALF_WIDTH, TRAIL_ANCHOR_BACK, TRAIL_ANCHOR_HEIGHT);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public RenderType getRenderType(BlackNimbusEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
