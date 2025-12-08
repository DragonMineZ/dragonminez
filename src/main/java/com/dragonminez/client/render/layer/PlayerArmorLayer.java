package com.dragonminez.client.render.layer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

import javax.annotation.Nullable;

public class PlayerArmorLayer<T extends AbstractClientPlayer & GeoAnimatable> extends ItemArmorGeoLayer<T> {
    public PlayerArmorLayer(GeoRenderer<T> geoRenderer) {
        super(geoRenderer);
    }
    @Override
    protected @Nullable ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        String boneName = bone.getName();
        return switch (boneName) {
            case "armorHead" -> this.helmetStack;
            case "armorBody","armorBody2", "armorRightArm", "armorLeftArm" -> this.chestplateStack;
            case "armorLeggingsBody","armorLeftLeg", "armorRightLeg" -> this.leggingsStack;
            case "armorRightBoot", "armorLeftBoot" -> this.bootsStack;
            default -> null;
        };
    }

    @Override
    protected @NotNull EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, T animatable) {
        String boneName = bone.getName();
        return switch (boneName) {
            case "armorHead" -> EquipmentSlot.HEAD;
            case "armorBody","armorBody2", "armorRightArm", "armorLeftArm" -> EquipmentSlot.CHEST;
            case "armorLeggingsBody","armorRightLeg", "armorLeftLeg" -> EquipmentSlot.LEGS;
            case "armorRightBoot", "armorLeftBoot" -> EquipmentSlot.FEET;
            default -> super.getEquipmentSlotForBone(bone, stack, animatable);
        };
    }

    @Override
    protected @NotNull ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack, T animatable, HumanoidModel<?> baseModel) {
        String boneName = bone.getName();

        return switch (boneName) {
            case "armorHead" -> baseModel.head;
            case "armorBody","armorBody2", "armorLeggingsBody" -> baseModel.body;
            case "armorRightArm" -> baseModel.rightArm;
            case "armorLeftArm" -> baseModel.leftArm;
            case "armorRightLeg", "armorRightBoot" -> baseModel.rightLeg;
            case "armorLeftLeg", "armorLeftBoot" -> baseModel.leftLeg;
            default -> super.getModelPartForBone(bone, slot, stack, animatable, baseModel);
        };
    }
}