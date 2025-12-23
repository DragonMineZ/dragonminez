package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.MasterGlobalModel;
import com.dragonminez.client.init.entities.model.sagas.DBSagaModel;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.SagaNappaEntity;
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
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
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
