package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.data.DMZAnimatable;
import com.dragonminez.client.render.layer.base.ItemArmorLayer;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;

import javax.annotation.Nullable;

public class DMZPlayerArmorLayer<T extends DMZAnimatable> extends ItemArmorLayer<T> {
    public DMZPlayerArmorLayer(GeoRenderer<T> geoRenderer) {
        super(geoRenderer);
    }

    @Override
    protected @Nullable ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        final AbstractClientPlayer player = this.player();
        if (player == null) {
            return null;
        }

        final String boneName = bone.getName();
        final LazyOptional<StatsData> optStats = StatsProvider.get(StatsCapability.INSTANCE, player);
        if (!optStats.isPresent()) {
            return null;
        }

        // Shut up the compiler
        final var stats = optStats.orElse(new StatsData(player));
        var character = stats.getCharacter();
        var race = character.getRace();
        var gender = character.getGender();

        if (race.equals("majin") && gender.equals("male") || gender.equals("female")) {
            if (boneName.equals("armorBody")) {
                return null;
            }
            if (boneName.equals("boobas")) {
                return null;
            }
        }

        return switch (boneName) {
            case "armorHead" -> this.helmetStack;
            case "armorBody", "armorRightArm", "armorLeftArm" -> this.chestplateStack;
            case "armorLeggingsBody", "armorLeftLeg", "armorRightLeg" -> this.leggingsStack;
            case "armorRightBoot", "armorLeftBoot" -> this.bootsStack;
            default -> null;
        };
    }

    @Override
    protected @NotNull EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, T animatable) {
        String boneName = bone.getName();
        return switch (boneName) {
            case "armorHead" -> EquipmentSlot.HEAD;
            case "armorBody", "armorRightArm", "armorLeftArm" -> EquipmentSlot.CHEST;
            case "armorLeggingsBody", "armorRightLeg", "armorLeftLeg" -> EquipmentSlot.LEGS;
            case "armorRightBoot", "armorLeftBoot" -> EquipmentSlot.FEET;
            default -> super.getEquipmentSlotForBone(bone, stack, animatable);
        };
    }

    @Override
    protected @NotNull ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack, T animatable, HumanoidModel<?> baseModel) {
        String boneName = bone.getName();

        return switch (boneName) {
            case "armorHead" -> baseModel.head;
            case "armorBody", "armorLeggingsBody" -> baseModel.body;
            case "armorRightArm" -> baseModel.rightArm;
            case "armorLeftArm" -> baseModel.leftArm;
            case "armorRightLeg", "armorRightBoot" -> baseModel.rightLeg;
            case "armorLeftLeg", "armorLeftBoot" -> baseModel.leftLeg;
            default -> super.getModelPartForBone(bone, slot, stack, animatable, baseModel);
        };
    }
}