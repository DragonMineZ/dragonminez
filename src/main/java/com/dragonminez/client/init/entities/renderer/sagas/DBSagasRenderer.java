package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.MasterGlobalModel;
import com.dragonminez.client.init.entities.model.sagas.DBSagaModel;
import com.dragonminez.client.init.entities.renderer.sagas.layer.DBSagasAuraLayer;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.sagas.*;
import com.dragonminez.common.quest.Saga;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DBSagasRenderer<T extends DBSagasEntity> extends GeoEntityRenderer<T> {

    private static final ResourceLocation NAPPA_NORMAL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/saga_nappa.png");
    private static final ResourceLocation NAPPA_DAMAGED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/saga_nappa2.png");

    public DBSagasRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DBSagaModel<>());
        this.shadowRadius = 0.4f;

        addRenderLayer(new DBSagasAuraLayer<>(this));
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        if (entity instanceof SagaFreezer1stEntity) {
            poseStack.scale(0.8f, 0.8f, 0.8f);
        } else if(entity instanceof SagaFreezer2ndEntity){
            poseStack.scale(1.2f, 1.2f, 1.2f);
        } else if(entity instanceof SagaFreezer3rdEntity){
            poseStack.scale(1.3f, 1.3f, 1.3f);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(T animatable) {

        if (animatable instanceof SagaNappaEntity nappa) {

            // 2. Si es Nappa, usamos su lógica de vida
            if (nappa.isBattleDamaged()) {
                return NAPPA_DAMAGED;
            }
            return NAPPA_NORMAL;
        }

        return super.getTextureLocation(animatable);
    }


}
