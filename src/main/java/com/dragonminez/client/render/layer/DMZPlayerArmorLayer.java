package com.dragonminez.client.render.layer;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

import javax.annotation.Nullable;

public class DMZPlayerArmorLayer<T extends AbstractClientPlayer & GeoAnimatable> extends ItemArmorGeoLayer<T> {

    public DMZPlayerArmorLayer(GeoRenderer<T> geoRenderer) {
        super(geoRenderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isSpectator()) return;
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected @Nullable ItemStack getArmorItemForBone(GeoBone bone, T animatable) {

        final String boneName = bone.getName().trim();

        final LazyOptional<StatsData> optStats = StatsProvider.get(StatsCapability.INSTANCE, animatable);
        if (!optStats.isPresent()) {
            return null;
        }

        final var stats = optStats.orElse(new StatsData(animatable));
        var character = stats.getCharacter();
        String race = character.getRace().toLowerCase();
        String gender = character.getGender().toLowerCase();
        String currentForm = character.getActiveForm();

        var raceConfig = ConfigManager.getRaceCharacter(race);
        String raceCustomModel = (raceConfig != null && raceConfig.getCustomModel() != null) ? raceConfig.getCustomModel().toLowerCase() : "";
        String formCustomModel = (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasCustomModel())
                ? character.getActiveFormData().getCustomModel().toLowerCase() : "";

        String logicKey = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
        if (logicKey.isEmpty()) {
            logicKey = race;
        }

        boolean isArmored = character.getArmored();

        if (boneName.equals("armorBody") || boneName.equals("armor_body")) {

            boolean isMajin = race.equals("majin") || logicKey.startsWith("majin");
            boolean isFemaleHumanOrSaiyan = gender.equals("female") && (race.equals("human") || race.equals("saiyan"));
            boolean isOozaru = race.equals("saiyan") && SaiyanForms.OOZARU.equalsIgnoreCase(currentForm) || logicKey.startsWith("oozaru") ;
            boolean isBuffed = logicKey.startsWith("buffed") || logicKey.startsWith("frostdemon_fp") || logicKey.startsWith("majin_ultra")
                    || logicKey.startsWith("namekian_orange") || logicKey.startsWith("bioandroid_ultra");

            boolean isDbzArmor = this.chestplateStack != null && this.chestplateStack.getItem() instanceof DbzArmorItem;

            if (isMajin || isFemaleHumanOrSaiyan || isOozaru) {
                if (!isArmored) {
                    return null;
                }
            } else if (isBuffed) {
            if (isDbzArmor) {
                return null;
            }
        }
        }

        return switch (boneName) {
            case "armorHead", "armor_head" -> this.helmetStack;
            case "armorBody", "armor_body",
                 "armorRightArm", "armor_right_arm",
                 "armorLeftArm", "armor_left_arm" -> this.chestplateStack;

            case "armorLeggingsBody", "armor_leggings_body",
                 "armorLeftLeg", "armor_left_leg",
                 "armorRightLeg", "armor_right_leg" -> this.leggingsStack;

            case "armorRightBoot", "armor_right_boot",
                 "armorLeftBoot", "armor_left_boot" -> this.bootsStack;
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