package com.yuseix.dragonminez.common.init.entity.client.renderer.saiyansaga;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.init.entity.client.model.saiyansaga.SaibamanModel;
import com.yuseix.dragonminez.common.init.entity.custom.saiyansaga.SaibamanEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class KyukonmanRenderer extends LivingEntityRenderer<SaibamanEntity, PlayerModel<SaibamanEntity>> {
    public KyukonmanRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new SaibamanModel<>(pContext.bakeLayer(SaibamanModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SaibamanEntity saibamanEntity) {
        return new ResourceLocation(Reference.MOD_ID,"textures/entity/sagas/saiyan/saibaman_2.png");
    }

    @Override
    public void render(SaibamanEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        pPoseStack.pushPose();
        pPoseStack.scale(0.9f,0.8f,0.9f);
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
        pPoseStack.popPose();
    }

    @Override
    protected void renderNameTag(SaibamanEntity pEntity, Component pDisplayName, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
    }
    @Override
    protected @Nullable RenderType getRenderType(SaibamanEntity pLivingEntity, boolean pBodyVisible, boolean pTranslucent, boolean pGlowing) {
        return RenderType.entityCutout(getTextureLocation(pLivingEntity));
    }
}
