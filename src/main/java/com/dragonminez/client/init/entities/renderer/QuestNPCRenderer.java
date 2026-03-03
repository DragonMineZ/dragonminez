package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.QuestNPCModel;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class QuestNPCRenderer extends GeoEntityRenderer<QuestNPCEntity> {

	public QuestNPCRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new QuestNPCModel());
		this.shadowRadius = 0.4f;
	}

	@Override
	public RenderType getRenderType(QuestNPCEntity animatable, ResourceLocation texture,
									@Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutout(texture);
	}
}

