package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.data.DMZAnimatable;
import com.dragonminez.client.render.hair.HairRenderer;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.stats.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Optional;

public class DMZHairLayer<T extends DMZAnimatable> extends GeoRenderLayer<T> {

    public DMZHairLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            DMZAnimatable animatable,
            BakedGeoModel model,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay) {

        AbstractClientPlayer player = getPlayer();
        if (player == null) return;

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, player);
        var stats = statsCap.orElse(new StatsData(player));
        Character character = stats.getCharacter();

        if (!HairManager.canUseHair(character)) {
            return;
        }

        CustomHair effectiveHair = HairManager.getEffectiveHair(character);
        if (effectiveHair == null || effectiveHair.isEmpty()) {
            return;
        }

        Optional<GeoBone> headBoneOpt = model.getBone("head");
        if (headBoneOpt.isEmpty()) {
            return;
        }

        GeoBone headBone = headBoneOpt.get();

        poseStack.pushPose();

        float scale = 1.0f / 0.9375f;
        poseStack.scale(scale, scale, scale);

        float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));

        poseStack.translate(
            headBone.getPivotX() / 16.0f,
            headBone.getPivotY() / 16.0f,
            headBone.getPivotZ() / 16.0f
        );

        if (headBone.getRotZ() != 0) {
            poseStack.mulPose(Axis.ZP.rotation(-headBone.getRotZ()));
        }
        if (headBone.getRotY() != 0) {
            poseStack.mulPose(Axis.YP.rotation(headBone.getRotY()));
        }
        if (headBone.getRotX() != 0) {
            poseStack.mulPose(Axis.XP.rotation(-headBone.getRotX()));
        }

        HairRenderer.render(
                poseStack,
                bufferSource,
                effectiveHair,
                character.getHairColor(),
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }


    private AbstractClientPlayer getPlayer() {
        if (this.renderer instanceof GeoReplacedEntityRenderer<?, ?> geoRenderer) {
            return (AbstractClientPlayer) geoRenderer.getCurrentEntity();
        }
        return null;
    }
}
