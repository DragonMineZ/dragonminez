package com.dragonminez.client.render.layer;

import com.dragonminez.common.init.armor.client.render.WeightCapeRenderer;
import com.dragonminez.common.init.item.WeightItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;

public class DMZWeightCapeLayer<T extends AbstractClientPlayer & GeoAnimatable> extends ItemArmorGeoLayer<T> {

    private final WeightCapeRenderer capeRenderer = new WeightCapeRenderer();

    public DMZWeightCapeLayer(GeoRenderer<T> geoRenderer) {
        super(geoRenderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isSpectator()) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected @Nullable ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        if (!"armorBody".equals(bone.getName())) return null;

        ItemStack stack = getWeightStack(animatable);
        if (stack.isEmpty() || !(stack.getItem() instanceof WeightItem weightItem)) return null;
        if (weightItem.getWeightType() != WeightItem.WeightType.PICCOLO_CAPE) return null;
        return stack;
    }

    @Override
    protected @NotNull EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, T animatable) {
        return EquipmentSlot.CHEST;
    }

    @Override
    protected @NotNull HumanoidModel<?> getModelForItem(GeoBone bone, EquipmentSlot slot, ItemStack stack, T animatable) {
        return capeRenderer;
    }

    private ItemStack getWeightStack(T animatable) {
        var inventory = CuriosApi.getCuriosInventory(animatable).orElse(null);
        if (inventory == null) return ItemStack.EMPTY;

        var stacksHandler = inventory.getCurios().get("weights");
        if (stacksHandler == null) return ItemStack.EMPTY;

        if (!stacksHandler.getRenders().get(0)) return ItemStack.EMPTY;

        return stacksHandler.getStacks().getStackInSlot(0);
    }
}
