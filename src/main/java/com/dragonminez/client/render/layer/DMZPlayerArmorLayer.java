package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.client.util.SkinGathererProvider;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.init.armor.DbzArmorTextured;
import com.dragonminez.common.init.armor.client.model.ArmorBaseModel;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

import javax.annotation.Nullable;

public class DMZPlayerArmorLayer<T extends AbstractClientPlayer & GeoAnimatable> extends ItemArmorGeoLayer<T> {
	private ArmorBaseModel dmzArmorModel;

	public DMZPlayerArmorLayer(GeoRenderer<T> geoRenderer) {
		super(geoRenderer);
	}

	@Override
	public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		if (animatable.isSpectator()) return;
		super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
	}

	@Override
	public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
			MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
			int packedOverlay) {
		ItemStack stack = getArmorItemForBone(bone, animatable);
		if (stack == null || !(stack.getItem() instanceof DbzArmorTextured)) {
			super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick,
					packedLight, packedOverlay);
			return;
		}

		EquipmentSlot slot = getEquipmentSlotForBone(bone, stack, animatable);
		HumanoidModel<?> armorModel = getModelForItem(bone, slot, stack, animatable);
		ModelPart armorPart = getModelPartForBone(bone, slot, stack, animatable, armorModel);
		if (armorPart.isEmpty() || bone.getCubes().isEmpty()) return;

		poseStack.pushPose();
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		prepModelPartForRender(poseStack, bone, armorPart);

		VertexConsumer armorBuffer = getVanillaArmorBuffer(bufferSource, animatable, stack, slot, bone,
				null, packedLight, packedOverlay, false);
		armorPart.render(poseStack, armorBuffer, packedLight, packedOverlay, -1);

		if (stack.hasFoil()) {
			VertexConsumer glintBuffer = getVanillaArmorBuffer(bufferSource, animatable, stack, slot, bone,
					null, packedLight, packedOverlay, true);
			armorPart.render(poseStack, glintBuffer, packedLight, packedOverlay, -1);
		}

		poseStack.popPose();
	}

    @Override
    protected @Nullable ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        final String boneName = bone.getName();

        EquipmentSlot slot = switch (boneName) {
            case "armorHead", "armor_head" -> EquipmentSlot.HEAD;
            case "armorBody", "armor_body",
                 "armorRightArm", "armor_right_arm",
                 "armorLeftArm", "armor_left_arm" -> EquipmentSlot.CHEST;
            case "armorLeggingsBody", "armor_leggings_body",
                 "armorLeftLeg", "armor_left_leg",
                 "armorRightLeg", "armor_right_leg" -> EquipmentSlot.LEGS;
            case "armorRightBoot", "armor_right_boot",
                 "armorLeftBoot", "armor_left_boot" -> EquipmentSlot.FEET;
            default -> null;
        };
        if (slot == null) return null;

        ItemStack stack = animatable.getInventory().armor.get(slot.getIndex());
        if (CosmeticArmorCompat.isLoaded()) {
            ItemStack cosStack = CosmeticArmorCompat.getCosmeticStack(animatable, slot);
            if (cosStack != null) {
                if (cosStack.isEmpty()) return null;
                stack = cosStack;
            }
        }

        if (stack.isEmpty()) return null;
        if (!(stack.getItem() instanceof ArmorItem) && !(stack.getItem() instanceof DbzArmorItem)) return null;
        if (!stack.canEquip(slot, animatable) && !(stack.getItem() instanceof DbzArmorItem)) return null;

        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
        if (stats != null) {
            var character = stats.getCharacter();
            String race = character.getRaceName().toLowerCase();
            String gender = character.getGender().toLowerCase();

            var bodyType = character.getBodyType();
            String logicKey = character.getRenderLogicKey();
            if (logicKey.equals("candy")) return null;

            if (boneName.equals("armorBody") || boneName.equals("armor_body")) {
                boolean isArmored = character.getArmored();
                boolean isMajin = logicKey.equals("majin");
                boolean isFemaleHumanOrSaiyan = (race.equals("human") || race.equals("saiyan")) && gender.equals(Character.GENDER_FEMALE);
                boolean isOozaru = character.isOozaruCached() || logicKey.contains("oozaru");

                boolean isBuffed = logicKey.contains("buffed") || logicKey.contains("frostdemon_fp") || logicKey.contains("majin_ultra")
                        || logicKey.contains("namekian_orange") || logicKey.contains("bioandroid_ultra") || logicKey.contains("ssj4d") || logicKey.contains("ssj4gt")
                        || logicKey.contains("frostdemon_fifth") || logicKey.contains("frostdemon_metalcore") || logicKey.contains("namekian_buffed")
                        || logicKey.contains("4arms") || logicKey.contains("bioandroid_xeno") || logicKey.contains("janemba_super");
                boolean isDbzArmor = stack.getItem() instanceof DbzArmorTextured;

				boolean isRestrictedMajin = (isMajin && bodyType != 2) || logicKey.equals("janemba_fat");
                boolean isCustomModel = SkinGathererProvider.modelFamily(logicKey).equals("custom");

                if (isRestrictedMajin || isFemaleHumanOrSaiyan || isOozaru) {
                    if (!isArmored) return null;
                } else if (isBuffed || isCustomModel) {
                    if (isDbzArmor) return null;
                }
            }
        }

        return stack;
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

	@Override
	protected HumanoidModel<?> getModelForItem(GeoBone bone, EquipmentSlot slot, ItemStack stack, T animatable) {
		if (!(stack.getItem() instanceof DbzArmorTextured)) {
			return super.getModelForItem(bone, slot, stack, animatable);
		}

		if (dmzArmorModel == null) {
			dmzArmorModel = new ArmorBaseModel(Minecraft.getInstance().getEntityModels().bakeLayer(ArmorBaseModel.LAYER_LOCATION));
		}
		return dmzArmorModel;
	}

	@Override
	protected VertexConsumer getVanillaArmorBuffer(MultiBufferSource bufferSource, T animatable, ItemStack stack,
			EquipmentSlot slot, GeoBone bone, ArmorMaterial.Layer layer, int packedLight, int packedOverlay,
			boolean glint) {
		if (!(stack.getItem() instanceof DbzArmorTextured textured)) {
			return super.getVanillaArmorBuffer(bufferSource, animatable, stack, slot, bone, layer,
					packedLight, packedOverlay, glint);
		}

		if (glint) return bufferSource.getBuffer(RenderType.armorEntityGlint());

		String namespace = Reference.MOD_ID;
		if (stack.getItem() instanceof DbzArmorItem dbzArmor) namespace = dbzArmor.getModId();
		ResourceLocation texture = ArmorTextureResolver.resolve(namespace, textured.getItemId(), slot, stack);
		return bufferSource.getBuffer(RenderType.armorCutoutNoCull(texture));
	}


}
