package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.client.init.entities.model.DinoGlobalModel;
import com.dragonminez.client.init.entities.model.ki.SPSkillsModel;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.dragonminez.common.init.entities.animal.SabertoothEntity;
import com.dragonminez.common.init.entities.ki.SPBlueHurricaneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SPSkillsRenderer<T extends SPBlueHurricaneEntity> extends GeoEntityRenderer<T> {

    public SPSkillsRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SPSkillsModel<>());
        this.shadowRadius = 0.8f;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }

}
