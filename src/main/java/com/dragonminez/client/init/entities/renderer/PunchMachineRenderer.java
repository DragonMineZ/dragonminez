package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.PunchMachineModel;
import com.dragonminez.client.init.entities.model.SpacePodModel;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PunchMachineRenderer extends GeoEntityRenderer<PunchMachineEntity> {

    public PunchMachineRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PunchMachineModel<>());
    }
}
