package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.client.init.entities.model.MasterGlobalModel;
import com.dragonminez.client.init.entities.model.sagas.DBSagaModel;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DBSagasRenderer<T extends DBSagasEntity> extends GeoEntityRenderer<T> {

    public DBSagasRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DBSagaModel<>());
        this.shadowRadius = 0.4f;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }
}
