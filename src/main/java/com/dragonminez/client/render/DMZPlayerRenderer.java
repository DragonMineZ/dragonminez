package com.dragonminez.client.render;

import com.dragonminez.client.model.DMZPlayerModel;
import com.dragonminez.client.render.data.DMZAnimatable;
import com.dragonminez.client.render.layer.DMZPlayerItemInHandLayer;
import com.dragonminez.client.render.layer.DMZPlayerArmorLayer;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

public class DMZPlayerRenderer extends GeoReplacedEntityRenderer<AbstractClientPlayer, DMZAnimatable> {

    public static final DMZPlayerRenderer INSTANCE = new DMZPlayerRenderer();
    private final PlayerModel<AbstractClientPlayer> modelReference;

    public DMZPlayerRenderer() {
        super(DMZPlayerRenderer.createContext(), DMZPlayerRenderer.createModel(), DMZAnimatable.INSTANCE);
        this.shadowRadius = 0.4f;
        this.addRenderLayer(new DMZPlayerItemInHandLayer(this));
        this.addRenderLayer(new DMZPlayerArmorLayer<>(this));
        this.modelReference = new PlayerModel<>(Minecraft.getInstance().getEntityModels()
                .bakeLayer(ModelLayers.PLAYER), false);
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, DMZAnimatable animatable,
                                    BakedGeoModel model, boolean isReRender, float partialTick, int packedLight,
                                    int packedOverlay) {
        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, this.currentEntity);
        var stats = statsCap.orElse(new StatsData(this.currentEntity));

        var character = stats.getCharacter();
        var activeForm = character.getActiveFormData();

        float scaling;
        if (activeForm != null) {
            scaling = activeForm.getModelScaling();
        } else {
            scaling = (float) character.getModelScaling();
        }

        poseStack.scale(scaling, scaling, scaling);
        this.shadowRadius = 0.4f * scaling;
    }

    private static EntityRendererProvider.Context createContext() {
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher entityRenderDispatcher = minecraft.getEntityRenderDispatcher();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        BlockRenderDispatcher blockRenderDispatcher = minecraft.getBlockRenderer();
        ItemInHandRenderer heldItemRenderer = entityRenderDispatcher.getItemInHandRenderer();
        ResourceManager resourceManager = minecraft.getResourceManager();
        EntityModelSet entityModelSet = minecraft.getEntityModels();
        Font font = minecraft.font;
        return new EntityRendererProvider.Context(
                entityRenderDispatcher,
                itemRenderer,
                blockRenderDispatcher,
                heldItemRenderer,
                resourceManager,
                entityModelSet,
                font
        );
    }

    private static DMZPlayerModel createModel() {
        return new DMZPlayerModel();
    }
}
