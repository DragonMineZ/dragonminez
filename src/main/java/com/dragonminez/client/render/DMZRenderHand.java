package com.dragonminez.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.model.KiBladeModel;
import com.dragonminez.client.model.KiScytheModel;
import com.dragonminez.client.model.KiTridentModel;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class DMZRenderHand extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final Map<ResourceLocation, Boolean> TEXTURE_CACHE = new HashMap<>();
    public static final ResourceLocation KI_WEAPON_TEX = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/kiweapons.png");

    public static final KiScytheModel KI_SCYTHE_MODEL = new KiScytheModel(KiScytheModel.createBodyLayer().bakeRoot());
    public static final KiBladeModel KI_BLADE_MODEL = new KiBladeModel(KiBladeModel.createBodyLayer().bakeRoot());
    public static final KiTridentModel KI_TRIDENT_MODEL = new KiTridentModel(KiTridentModel.createBodyLayer().bakeRoot());

    public DMZRenderHand(EntityRendererProvider.Context pContext, PlayerModel<AbstractClientPlayer> pModel) {
        super(pContext,  new PlayerModel(pContext.bakeLayer(ModelLayers.PLAYER), false),0.4f);
    }

    public void renderRightHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer) {
        var stats = StatsProvider.get(StatsCapability.INSTANCE, pPlayer).orElse(new StatsData(pPlayer));

        if (stats.getStatus().isBlocking()) {
            pPoseStack.pushPose();
            applyBlockingTransform(pPoseStack, 1.0F);
            this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.rightArm, this.model.rightSleeve);
            pPoseStack.popPose();

            pPoseStack.pushPose();
            applyBlockingTransform(pPoseStack, -1.0F);
            this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.leftArm, this.model.leftSleeve);
            pPoseStack.popPose();
        } else {
            pPoseStack.pushPose();
            this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.rightArm, this.model.rightSleeve);
            pPoseStack.popPose();
        }

        this.renderKiWeapon(pPoseStack, pBuffer, pCombinedLight, pPlayer, stats, HumanoidArm.RIGHT);

    }
    public void renderLeftHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer) {
        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, pPlayer);
        var stats = statsCap.orElse(new StatsData(pPlayer));

        if (stats.getStatus().isBlocking()) {
            pPoseStack.pushPose();
            applyBlockingTransform(pPoseStack, 1.0F);
            this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.rightArm, this.model.rightSleeve);
            pPoseStack.popPose();

            pPoseStack.pushPose();
            applyBlockingTransform(pPoseStack, -1.0F);
            this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.leftArm, this.model.leftSleeve);
            pPoseStack.popPose();
        } else {
            pPoseStack.pushPose();
            this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.leftArm, this.model.leftSleeve);
            pPoseStack.popPose();
        }

        this.renderKiWeapon(pPoseStack, pBuffer, pCombinedLight, pPlayer, stats, HumanoidArm.LEFT);

    }

    private void applyBlockingTransform(PoseStack stack, float side) {
        stack.translate(side * -0.25F, -0.15F, -0.4F);
        stack.mulPose(Axis.XP.rotationDegrees(-20.0F));
        stack.mulPose(Axis.YP.rotationDegrees(100.0F));
        stack.mulPose(Axis.ZP.rotationDegrees(side * 330.0F));
    }

    private void renderHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer, ModelPart pRendererArm, ModelPart pRendererArmwear) {
        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, pPlayer);
        var stats = statsCap.orElse(new StatsData(pPlayer));
        var character = stats.getCharacter();

        PlayerModel<AbstractClientPlayer> playermodel = this.getModel();
        playermodel.attackTime = 0.0F;
        playermodel.crouching = false;
        playermodel.swimAmount = 0.0F;
        playermodel.setupAnim(pPlayer, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        pRendererArm.xRot = 0.0F;

        String raceName = character.getRace().toLowerCase();
        int bodyType = character.getBodyType();
        String currentForm = character.getActiveForm();
        boolean hasForm = (currentForm != null && !currentForm.isEmpty() && !currentForm.equals("base"));

        float[] b1 = ColorUtils.hexToRgb(character.getBodyColor());
        float[] b2 = ColorUtils.hexToRgb(character.getBodyColor2());
        float[] b3 = ColorUtils.hexToRgb(character.getBodyColor3());
        float[] hair = ColorUtils.hexToRgb(character.getHairColor());

        if (hasForm && character.getActiveFormData() != null) {
            var activeForm = character.getActiveFormData();
            if (!activeForm.getBodyColor1().isEmpty()) b1 = ColorUtils.hexToRgb(activeForm.getBodyColor1());
            if (!activeForm.getBodyColor2().isEmpty()) b2 = ColorUtils.hexToRgb(activeForm.getBodyColor2());
            if (!activeForm.getBodyColor3().isEmpty()) b3 = ColorUtils.hexToRgb(activeForm.getBodyColor3());
            if (!activeForm.getHairColor().isEmpty()) hair = ColorUtils.hexToRgb(activeForm.getHairColor());
        }

        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
        boolean isStandard = raceName.equals("human") || raceName.equals("saiyan");
        boolean forceVanilla = (raceConfig != null && raceConfig.useVanillaSkin());
		String gender = character.getGender().toLowerCase();

        if (forceVanilla || (isStandard && bodyType == 0)) {
            renderPart(pPoseStack, pBuffer, pCombinedLight, pRendererArm, pPlayer.getSkinTextureLocation(), new float[]{1, 1, 1});
        } else {
            renderRaceLayers(pPoseStack, pBuffer, pCombinedLight, pRendererArm, raceName, currentForm, gender, bodyType, b1, b2, b3, hair);
        }

        renderTattoos(pPoseStack, pBuffer, pCombinedLight, pRendererArm, stats);

        renderDbzArmor(pPoseStack, pBuffer, pCombinedLight, pPlayer,pRendererArm);

    }

    private void renderKiWeapon(PoseStack ps, MultiBufferSource buffer, int light, AbstractClientPlayer player, StatsData stats, HumanoidArm arm) {
        if (!stats.getSkills().isSkillActive("kimanipulation")) return;

        String type = stats.getStatus().getKiWeaponType();
        if (type == null || type.equalsIgnoreCase("none")) return;

        float[] color = getKiColor(stats);
        boolean isRight = arm == HumanoidArm.RIGHT;

        ps.pushPose();

        // --- AJUSTES DE POSICIÓN MANUALES POR TIPO ---
        switch (type.toLowerCase()) {
            case "blade" -> {
                KI_BLADE_MODEL.rightArm.copyFrom(isRight ? this.model.rightArm : this.model.leftArm);

                ps.translate(isRight ? -0.02D : 0.02D, 0.1D, -0.1D);
                ps.mulPose(Axis.XP.rotationDegrees(5.0F));

                renderKiPart(ps, buffer, light, isRight ? KI_BLADE_MODEL.rightArm : KI_BLADE_MODEL.leftArm, color);
            }
            case "scythe" -> {
                KI_SCYTHE_MODEL.rightArm.copyFrom(isRight ? this.model.rightArm : this.model.leftArm);

                // Ajustes manuales para la Guadaña (ej: inclinada para que se vea el filo en 1ra persona)
                ps.translate(isRight ? -0.1D : 0.1D, -0.2D, -0.2D);
                ps.mulPose(Axis.ZP.rotationDegrees(isRight ? 15.0F : -15.0F));

                renderKiPart(ps, buffer, light, isRight ? KI_SCYTHE_MODEL.rightArm : KI_SCYTHE_MODEL.leftArm, color);
            }
            case "clawlance" -> {
                KI_TRIDENT_MODEL.rightArm.copyFrom(isRight ? this.model.rightArm : this.model.leftArm);

                ps.translate(0.0D, 0.0D, -0.3D);
                renderKiPart(ps, buffer, light, isRight ? KI_TRIDENT_MODEL.rightArm : KI_TRIDENT_MODEL.leftArm, color);
            }
        }

        ps.popPose();
    }

    private void renderRaceLayers(PoseStack stack, MultiBufferSource buffer, int light, ModelPart arm, String race, String form, String gender, int bodyType, float[] b1, float[] b2, float[] b3, float[] h) {
        String pathPrefix;
        boolean isBio = race.equals("bioandroid");
        boolean isFrost = race.equals("frostdemon");

        if (isFrost && (Objects.equals(form, FrostDemonForms.FINAL_FORM) || Objects.equals(form, FrostDemonForms.FULLPOWER))) {
            pathPrefix = "textures/entity/races/frostdemon/finalform_bodytype_" + bodyType + "_";
            if (bodyType == 0) {
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer1.png"), b1);
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer2.png"), h);
            } else {
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer1.png"), b1);
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer2.png"), b2);
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer3.png"), (bodyType == 1 ? b3 : h));
            }
        } else {
            if (isBio) {
                pathPrefix = "textures/entity/races/bioandroid/" + (form == null || form.isEmpty() ? "base" : form.toLowerCase()) + "_" + bodyType + "_";
            } else if (race.equals("majin")) {
                pathPrefix = "textures/entity/races/majin/" + (form == null || form.isEmpty() ? "base" : form.toLowerCase()) + "_" + bodyType + "_male_";
            } else {
                pathPrefix = "textures/entity/races/" + (race.equals("human") || race.equals("saiyan") ? "humansaiyan" : race) + "/bodytype_" + gender + "_";
            }

			if (pathPrefix.contains("humansaiyan") && (form == null || !form.contains("oozaru"))) {
				renderPart(stack, buffer, light, arm, loc(pathPrefix + "1.png"), b1);
				renderPart(stack, buffer, light, arm, loc(pathPrefix + "2.png"), b2);
			} else {
				renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer1.png"), b1);
				renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer2.png"), b2);
				renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer3.png"), b3);
			}

			if (isFrost || isBio) {
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer4.png"), h);
            }

            if (isBio || (isFrost && bodyType == 0)) {
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer5.png"), ColorUtils.hexToRgb("#e67d40"));
            }
        }
    }

    private void renderDbzArmor(PoseStack ps, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer player,ModelPart pRendererArm) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);

        if (!chestStack.isEmpty() && chestStack.getItem() instanceof DbzArmorItem armorItem) {
            String texturePath = "textures/armor/" + armorItem.getItemId() + "_layer1.png";
            ResourceLocation armorResource = new ResourceLocation(Reference.MOD_ID, texturePath);

            if (textureExists(armorResource)) {
                ps.pushPose();

                float armorInflation = 1.05F;
                ps.scale(armorInflation, armorInflation, armorInflation);
                ps.translate(0.02D, 0.02D, 0.0D);

                renderPart(ps, pBuffer, pCombinedLight, pRendererArm, armorResource, new float[]{1.0F, 1.0F, 1.0F});

                ps.popPose();
            }
        }
    }

    private void renderTattoos(PoseStack stack, MultiBufferSource buffer, int light, ModelPart arm, StatsData stats) {
        if (stats.getEffects() != null && stats.getEffects().hasEffect("majin")) {
            renderPart(stack, buffer, light, arm, new ResourceLocation(Reference.MOD_ID, "textures/entity/races/majinm.png"), new float[]{1,1,1});
        }

        int tattooType = stats.getCharacter().getTattooType();
        if (tattooType != 0) {
            ResourceLocation tattooLoc = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/tattoos/tattoo_" + tattooType + ".png");
            renderPart(stack, buffer, light, arm, tattooLoc, new float[]{1,1,1});
        }
    }

    private void renderPart(PoseStack stack, MultiBufferSource buffer, int light, ModelPart part, ResourceLocation texture, float[] rgb) {
        if (textureExists(texture)) {
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(texture));
            part.render(stack, vc, light, OverlayTexture.NO_OVERLAY, rgb[0], rgb[1], rgb[2], 1.0F);
        }
    }

    private ResourceLocation loc(String path) {
        return new ResourceLocation(Reference.MOD_ID, path);
    }

    private boolean textureExists(ResourceLocation location) {
        return TEXTURE_CACHE.computeIfAbsent(location, loc -> Minecraft.getInstance().getResourceManager().getResource(loc).isPresent());
    }

    private void setModelProperties(AbstractClientPlayer pClientPlayer) {
        PlayerModel<AbstractClientPlayer> playermodel = (PlayerModel)this.getModel();
        if (pClientPlayer.isSpectator()) {
            playermodel.setAllVisible(false);
            playermodel.head.visible = true;
            playermodel.hat.visible = true;
        } else {
            playermodel.setAllVisible(true);
            playermodel.hat.visible = pClientPlayer.isModelPartShown(PlayerModelPart.HAT);
            playermodel.jacket.visible = pClientPlayer.isModelPartShown(PlayerModelPart.JACKET);
            playermodel.leftPants.visible = pClientPlayer.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
            playermodel.rightPants.visible = pClientPlayer.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
            playermodel.leftSleeve.visible = pClientPlayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
            playermodel.rightSleeve.visible = pClientPlayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
            playermodel.crouching = pClientPlayer.isCrouching();
            HumanoidModel.ArmPose humanoidmodel$armpose = getArmPose(pClientPlayer, InteractionHand.MAIN_HAND);
            HumanoidModel.ArmPose humanoidmodel$armpose1 = getArmPose(pClientPlayer, InteractionHand.OFF_HAND);
            if (humanoidmodel$armpose.isTwoHanded()) {
                humanoidmodel$armpose1 = pClientPlayer.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            }

            if (pClientPlayer.getMainArm() == HumanoidArm.RIGHT) {
                playermodel.rightArmPose = humanoidmodel$armpose;
                playermodel.leftArmPose = humanoidmodel$armpose1;
            } else {
                playermodel.rightArmPose = humanoidmodel$armpose1;
                playermodel.leftArmPose = humanoidmodel$armpose;
            }
        }

    }

    private static HumanoidModel.ArmPose getArmPose(AbstractClientPlayer pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (itemstack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        } else {
            if (pPlayer.getUsedItemHand() == pHand && pPlayer.getUseItemRemainingTicks() > 0) {
                UseAnim useanim = itemstack.getUseAnimation();
                if (useanim == UseAnim.BLOCK) {
                    return HumanoidModel.ArmPose.BLOCK;
                }

                if (useanim == UseAnim.BOW) {
                    return HumanoidModel.ArmPose.BOW_AND_ARROW;
                }

                if (useanim == UseAnim.SPEAR) {
                    return HumanoidModel.ArmPose.THROW_SPEAR;
                }

                if (useanim == UseAnim.CROSSBOW && pHand == pPlayer.getUsedItemHand()) {
                    return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (useanim == UseAnim.SPYGLASS) {
                    return HumanoidModel.ArmPose.SPYGLASS;
                }

                if (useanim == UseAnim.TOOT_HORN) {
                    return HumanoidModel.ArmPose.TOOT_HORN;
                }

                if (useanim == UseAnim.BRUSH) {
                    return HumanoidModel.ArmPose.BRUSH;
                }
            } else if (!pPlayer.swinging && itemstack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack)) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }

            HumanoidModel.ArmPose forgeArmPose = IClientItemExtensions.of(itemstack).getArmPose(pPlayer, pHand, itemstack);
            return forgeArmPose != null ? forgeArmPose : HumanoidModel.ArmPose.ITEM;
        }
    }

    protected void setupRotations(AbstractClientPlayer pEntityLiving, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        float f = pEntityLiving.getSwimAmount(pPartialTicks);
        if (pEntityLiving.isFallFlying()) {
            super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
            float f1 = (float)pEntityLiving.getFallFlyingTicks() + pPartialTicks;
            float f2 = Mth.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
            if (!pEntityLiving.isAutoSpinAttack()) {
                pPoseStack.mulPose(Axis.XP.rotationDegrees(f2 * (-90.0F - pEntityLiving.getXRot())));
            }

            Vec3 vec3 = pEntityLiving.getViewVector(pPartialTicks);
            Vec3 vec31 = pEntityLiving.getDeltaMovementLerped(pPartialTicks);
            double d0 = vec31.horizontalDistanceSqr();
            double d1 = vec3.horizontalDistanceSqr();
            if (d0 > 0.0D && d1 > 0.0D) {
                double d2 = (vec31.x * vec3.x + vec31.z * vec3.z) / Math.sqrt(d0 * d1);
                double d3 = vec31.x * vec3.z - vec31.z * vec3.x;
                pPoseStack.mulPose(Axis.YP.rotation((float)(Math.signum(d3) * Math.acos(d2))));
            }
        } else if (f > 0.0F) {
            super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
            float f3 = pEntityLiving.isInWater() || pEntityLiving.isInFluidType((fluidType, height) -> pEntityLiving.canSwimInFluidType(fluidType)) ? -90.0F - pEntityLiving.getXRot() : -90.0F;
            float f4 = Mth.lerp(f, 0.0F, f3);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(f4));
            if (pEntityLiving.isVisuallySwimming()) {
                pPoseStack.translate(0.0F, -1.0F, 0.3F);
            }
        } else {
            super.setupRotations(pEntityLiving, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTicks);
        }

    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer pEntity) {
        return pEntity.getSkinTextureLocation();
    }

    private void renderKiPart(PoseStack ps, MultiBufferSource buffer, int light, ModelPart part, float[] color) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(KI_WEAPON_TEX));
        part.render(ps, vc, light, OverlayTexture.NO_OVERLAY, color[0], color[1], color[2], 0.85F);
    }

    private float[] getKiColor(StatsData stats) {
        var character = stats.getCharacter();
        String kiHex = character.getAuraColor();
        if (character.hasActiveForm() && character.getActiveFormData() != null) {
            String formColor = character.getActiveFormData().getAuraColor();
            if (formColor != null && !formColor.isEmpty()) kiHex = formColor;
        }
        return ColorUtils.hexToRgb(kiHex);
    }

}
