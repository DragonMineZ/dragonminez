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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DMZHairLayer<T extends DMZAnimatable> extends GeoRenderLayer<T> {

    public DMZHairLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            T animatable,
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

        if (!HairManager.canUseHair(character)) return;

        CustomHair effectiveHair = HairManager.getEffectiveHair(character);
        if (effectiveHair == null || effectiveHair.isEmpty()) return;

        Optional<GeoBone> headBoneOpt = model.getBone("head");
        if (headBoneOpt.isEmpty()) return;

        GeoBone headBone = headBoneOpt.get();

        poseStack.pushPose();

        // 1. CORRECCIÓN DE GIRO 180º (Sincronización con el cuerpo)
        // Minecraft renderiza las entidades rotadas 180 grados por defecto.
        // Además, necesitamos seguir la rotación del cuerpo del jugador.
        float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));

        // 2. CONSTRUIR JERARQUÍA
        List<GeoBone> boneChain = new ArrayList<>();
        CoreGeoBone currentBone = headBone;
        while (currentBone != null) {
            boneChain.add((GeoBone) currentBone);
            currentBone = currentBone.getParent();
        }

        // 3. APLICAR TRANSFORMACIONES (Root -> Head)
        for (int i = boneChain.size() - 1; i >= 0; i--) {
            GeoBone bone = boneChain.get(i);

            // Traslación de la animación (Invertimos X para Geckolib)
            poseStack.translate(-bone.getPosX() / 16f, bone.getPosY() / 16f, bone.getPosZ() / 16f);

            RenderUtils.translateToPivotPoint(poseStack, bone);

            // Rotación: Usamos los valores tal cual los da el hueso animado
            if (bone.getRotZ() != 0) poseStack.mulPose(Axis.ZP.rotation(bone.getRotZ()));
            if (bone.getRotY() != 0) poseStack.mulPose(Axis.YP.rotation(bone.getRotY()));
            if (bone.getRotX() != 0) poseStack.mulPose(Axis.XP.rotation(bone.getRotX()));

            RenderUtils.scaleMatrixForBone(poseStack, bone);
            RenderUtils.translateAwayFromPivotPoint(poseStack, bone);
        }

        // 4. POSICIONAR EN EL PIVOTE DE LA CABEZA
        RenderUtils.translateToPivotPoint(poseStack, headBone);

        // 5. RENDERIZAR
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