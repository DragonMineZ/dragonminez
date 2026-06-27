package com.dragonminez.common.init.armor.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.client.model.ArmorCapeModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ArmorCapeRenderer extends GeoArmorRenderer<DbzArmorCapeItem> {
    public ArmorCapeRenderer() {
        super(new ArmorCapeModel());
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        String name = bone.getName();
        if (name == null || !name.startsWith("cape")) return;
        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public ResourceLocation getTextureLocation(DbzArmorCapeItem animatable) {
        String modId = animatable.getModId();
        String itemId = animatable.getItemId();

        if (itemId.contains("pothala") || itemId.contains("scouter"))
            return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/blank.png");

        return ArmorTextureResolver.resolve(modId, itemId, this.getCurrentSlot(), this.getCurrentStack());
    }

    @Override
    public RenderType getRenderType(DbzArmorCapeItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.armorCutoutNoCull(texture);
    }
}
