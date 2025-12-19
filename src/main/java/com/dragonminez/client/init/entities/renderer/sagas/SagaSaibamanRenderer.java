package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.client.init.entities.model.sagas.DBSagaModel;
import com.dragonminez.client.init.entities.model.sagas.DBSaibamanModel;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.SagaSaibamanEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SagaSaibamanRenderer<T extends SagaSaibamanEntity> extends GeoEntityRenderer<T> {

    public SagaSaibamanRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DBSaibamanModel<>());
        this.shadowRadius = 0.4f;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }
}
