package com.dragonminez.client.render;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.model.KiBladeModel;
import com.dragonminez.client.model.KiScytheModel;
import com.dragonminez.client.model.KiTridentModel;
import com.dragonminez.client.util.AuraRenderQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.ModRenderTypes;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.BioAndroidForms;
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

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class DMZRenderHand extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final Map<ResourceLocation, Boolean> TEXTURE_CACHE = new HashMap<>();
    public static final ResourceLocation KI_WEAPON_TEX = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/kiweapons.png");

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

        if (stats.getStatus().isAuraActive() && !stats.getStatus().isAndroidUpgraded()) queueFirstPersonAura(pPlayer, pPoseStack, pCombinedLight);
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
        var stats = StatsProvider.get(StatsCapability.INSTANCE, pPlayer).orElse(new StatsData(pPlayer));
        var character = stats.getCharacter();
        int kaiokenPhase = TransformationsHelper.getKaiokenPhase(stats);

        this.model.attackTime = 0.0F;
        this.model.crouching = false;
        this.model.swimAmount = 0.0F;
        this.model.setupAnim(pPlayer, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        pRendererArm.xRot = 0.0F;

        String raceName = character.getRaceName().toLowerCase();
        int bodyType = character.getBodyType();
        String currentForm = character.getActiveForm();
        boolean hasForm = (character.hasActiveForm() && !Objects.equals(currentForm, "base"));

        float[] b1 = ColorUtils.hexToRgb(character.getBodyColor());
        float[] b2 = ColorUtils.hexToRgb(character.getBodyColor2());
        float[] b3 = ColorUtils.hexToRgb(character.getBodyColor3());
        float[] hair = ColorUtils.hexToRgb(character.getHairColor());

        if (hasForm && character.getActiveFormData() != null) {
            var f = character.getActiveFormData();
            if (!f.getBodyColor1().isEmpty()) b1 = ColorUtils.hexToRgb(f.getBodyColor1());
            if (!f.getBodyColor2().isEmpty()) b2 = ColorUtils.hexToRgb(f.getBodyColor2());
            if (!f.getBodyColor3().isEmpty()) b3 = ColorUtils.hexToRgb(f.getBodyColor3());
            if (!f.getHairColor().isEmpty()) hair = ColorUtils.hexToRgb(f.getHairColor());
        }

        if (stats.getStatus().isActionCharging() && stats.getStatus().getSelectedAction() == ActionMode.FORM) {
            var nextForm = TransformationsHelper.getNextAvailableForm(stats);
            if (nextForm != null) {
                float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
                if (!nextForm.getBodyColor1().isEmpty()) {
                    float[] target = ColorUtils.hexToRgb(nextForm.getBodyColor1());
                    b1 = new float[]{Mth.lerp(factor, b1[0], target[0]), Mth.lerp(factor, b1[1], target[1]), Mth.lerp(factor, b1[2], target[2])};
                }
                if (!nextForm.getBodyColor2().isEmpty()) {
                    float[] target = ColorUtils.hexToRgb(nextForm.getBodyColor2());
                    b2 = new float[]{Mth.lerp(factor, b2[0], target[0]), Mth.lerp(factor, b2[1], target[1]), Mth.lerp(factor, b2[2], target[2])};
                }
                if (!nextForm.getBodyColor3().isEmpty()) {
                    float[] target = ColorUtils.hexToRgb(nextForm.getBodyColor3());
                    b3 = new float[]{Mth.lerp(factor, b3[0], target[0]), Mth.lerp(factor, b3[1], target[1]), Mth.lerp(factor, b3[2], target[2])};
                }
                if (!nextForm.getHairColor().isEmpty()) {
                    float[] target = ColorUtils.hexToRgb(nextForm.getHairColor());
                    hair = new float[]{Mth.lerp(factor, hair[0], target[0]), Mth.lerp(factor, hair[1], target[1]), Mth.lerp(factor, hair[2], target[2])};
                }
            }
        }

        b1 = applyKaiokenTint(b1, kaiokenPhase);
        b2 = applyKaiokenTint(b2, kaiokenPhase);
        b3 = applyKaiokenTint(b3, kaiokenPhase);
        hair = applyKaiokenTint(hair, kaiokenPhase);

        String customModelValue = "";
        if (character.hasActiveForm()) {
            var activeFormData = character.getActiveFormData();
            if (activeFormData != null && activeFormData.hasCustomModel()) {
                customModelValue = activeFormData.getCustomModel().toLowerCase();
            }
        }
        if (customModelValue.isEmpty()) {
            RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
            if (raceConfig != null && raceConfig.getCustomModel() != null && !raceConfig.getCustomModel().isEmpty()) {
                customModelValue = raceConfig.getCustomModel().toLowerCase();
            }
        }

        final String logicKey = customModelValue.isEmpty() ? raceName : customModelValue;

        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
        boolean forceVanilla = (raceConfig != null && raceConfig.useVanillaSkin());
        boolean isOozaru = logicKey.equals("oozaru") || (currentForm != null && currentForm.contains("oozaru"));
        boolean isHumanoid = logicKey.equals("human") || logicKey.equals("saiyan") || logicKey.equals("saiyan_ssj4");

        if (forceVanilla || (isHumanoid && bodyType == 0 && !isOozaru)) {
            float[] skinTint = applyKaiokenTint(new float[]{1.0f, 1.0f, 1.0f}, kaiokenPhase);
            renderPart(pPoseStack, pBuffer, pCombinedLight, pRendererArm, pPlayer.getSkinTextureLocation(), skinTint);
        }else if (isOozaru) {
            float[] skin = ColorUtils.hexToRgb("#FFD7CF");
            float[] furColor = ColorUtils.hexToRgb("#572117");

            if (currentForm != null && (currentForm.contains("golden") || !currentForm.equals(SaiyanForms.OOZARU))) {
                furColor = hair;
            }

            skin = applyKaiokenTint(skin, kaiokenPhase);
            if (currentForm != null && currentForm.equals(SaiyanForms.OOZARU)) {
                furColor = applyKaiokenTint(furColor, kaiokenPhase);
            }

            String basePath = "textures/entity/races/humansaiyan/oozaru_";

            renderPart(pPoseStack, pBuffer, pCombinedLight, pRendererArm, loc(basePath + "layer1.png"), furColor);
            renderPart(pPoseStack, pBuffer, pCombinedLight, pRendererArm, loc(basePath + "layer2.png"), skin);
            renderPart(pPoseStack, pBuffer, pCombinedLight, pRendererArm, loc(basePath + "layer3.png"), applyKaiokenTint(new float[]{1f, 1f, 1f}, kaiokenPhase));
        } else {
            renderRaceLayers(pPoseStack, pBuffer, pCombinedLight, pRendererArm, logicKey, currentForm, character.getGender().toLowerCase(), bodyType, b1, b2, b3, hair);
        }

        renderTattoos(pPoseStack, pBuffer, pCombinedLight, pRendererArm, stats);
        renderDbzArmor(pPoseStack, pBuffer, pCombinedLight, pPlayer, pRendererArm);
    }

    private void renderKiWeapon(PoseStack ps, MultiBufferSource buffer, int light, AbstractClientPlayer player, StatsData stats, HumanoidArm arm) {
        if (!stats.getSkills().isSkillActive("kimanipulation")) return;

        String type = stats.getStatus().getKiWeaponType();
        if (type == null || type.equalsIgnoreCase("none")) return;

        float[] color = getKiColor(stats);
        boolean isRight = arm == HumanoidArm.RIGHT;

        ps.pushPose();

        switch (type.toLowerCase()) {
            case "blade" -> {
                KI_BLADE_MODEL.rightArm.copyFrom(isRight ? this.model.rightArm : this.model.leftArm);

                ps.translate(isRight ? -0.02D : 0.15D, 0.1D, -0.1D);
                ps.mulPose(Axis.XP.rotationDegrees(5.0F));

                renderKiPart(ps, buffer, light,KI_BLADE_MODEL.right_arm, color);
            }
            case "scythe" -> {
                KI_SCYTHE_MODEL.rightArm.copyFrom(isRight ? this.model.rightArm : this.model.leftArm);

                ps.translate(isRight ? -0.06D : 0.65D, isRight ? 0.1D : 0.1d, isRight ? -0.2D : 0.5D);
                ps.mulPose(Axis.YP.rotationDegrees(isRight ? 15.0F : -15.0F));

                renderKiPart(ps, buffer, light,KI_SCYTHE_MODEL.scythe_right, color);
            }
            case "clawlance" -> {
                KI_TRIDENT_MODEL.rightArm.copyFrom(isRight ? this.model.rightArm : this.model.leftArm);

                ps.translate(isRight ? -0.05D : 0.8D, isRight ? 0.0D : 0, isRight ? -0.3D : 0.5);
                ps.mulPose(Axis.XP.rotationDegrees(isRight ? 25.0F : -05.0F));
                renderKiPart(ps, buffer, light,KI_TRIDENT_MODEL.trident_right, color);
            }
        }

        ps.popPose();
    }

    private void renderRaceLayers(PoseStack stack, MultiBufferSource buffer, int light, ModelPart arm, String logicKey, String form, String gender, int bodyType, float[] b1, float[] b2, float[] b3, float[] h) {
        String pathPrefix;
        List<String> layeredRaces = Arrays.asList("human", "saiyan", "saiyan_ssj4", "namekian", "namekian_orange", "majin", "majin_super", "majin_ultra", "majin_evil", "majin_kid", "frostdemon", "frostdemon_final", "frostdemon_fifth", "frostdemon_third", "bioandroid", "bioandroid_semi", "bioandroid_perfect");

        if (!layeredRaces.contains(logicKey)) {
            renderPart(stack, buffer, light, arm, loc("textures/entity/races/" + logicKey + ".png"), b1);
            return;
        }

        if (logicKey.startsWith("frostdemon")) {
            boolean isFinal = Objects.equals(form, FrostDemonForms.FINAL_FORM) || Objects.equals(form, FrostDemonForms.FULLPOWER) || Objects.equals(form, FrostDemonForms.FIFTH_FORM) || logicKey.contains("final") || logicKey.contains("fifth");

            if (isFinal) {
                pathPrefix = "textures/entity/races/frostdemon/" + (logicKey.contains("fifth") || (form != null && form.equals(FrostDemonForms.FIFTH_FORM)) ? "fifth" : "finalform") + "_bodytype_" + bodyType + "_";
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer1.png"), b1);
                if (bodyType == 0) renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer2.png"), h);
                else {
                    renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer2.png"), b2);
                    renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer3.png"), (bodyType == 1 ? b3 : h));
                }
            } else {
                pathPrefix = "textures/entity/races/frostdemon/" + (logicKey.contains("third") ? "thirdform_" : "") + "bodytype_" + bodyType + "_";
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer1.png"), b1);
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer2.png"), b2);
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer3.png"), b3);
                renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer4.png"), h);

                if (bodyType == 0) {
                    renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer5.png"), ColorUtils.hexToRgb("#e67d40"));
                }
            }
        } else if (logicKey.startsWith("bioandroid")) {
            String phase = logicKey.contains("semi") ? "semiperfect" : (logicKey.contains("perfect") || (form != null && !form.isEmpty()) ? "perfect" : "base");
            pathPrefix = "textures/entity/races/bioandroid/" + phase + "_" + bodyType + "_";
            renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer1.png"), b1);
            renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer2.png"), phase.equals("perfect") ? new float[]{1,1,1} : b2);
            renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer3.png"), b3);
            renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer4.png"), h);
        } else if (logicKey.startsWith("majin")) {
            String f = logicKey.contains("kid") ? "kid" : (logicKey.contains("evil") ? "evil" : (logicKey.contains("super") ? "super" : "base"));
            pathPrefix = "textures/entity/races/majin/" + f + "_0_" + (gender.contains("female") ? "female" : "male") + "_";
            renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer1.png"), b1);
        } else if (logicKey.startsWith("namekian")) {
            pathPrefix = "textures/entity/races/namekian/bodytype_0_";
            renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer1.png"), b1);
            renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer2.png"), b2);
            renderPart(stack, buffer, light, arm, loc(pathPrefix + "layer3.png"), b3);
        } else {
            pathPrefix = "textures/entity/races/humansaiyan/bodytype_" + (gender.contains("female") ? "female" : "male") + "_";
            renderPart(stack, buffer, light, arm, loc(pathPrefix + (bodyType == 0 ? "1.png" : bodyType + ".png")), b1);
        }
    }

    private void renderDbzArmor(PoseStack ps, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer player, ModelPart pRendererArm) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.isEmpty()) return;

        String itemId = null;

        if (chestStack.getItem() instanceof DbzArmorItem armorItem) {
            itemId = armorItem.getItemId();
        }
        else if (chestStack.getItem() instanceof DbzArmorCapeItem capeItem) {
            itemId = capeItem.getItemId();
        }

        if (itemId != null) {
            String texturePath = "textures/armor/" + itemId + "_layer1.png";
            ResourceLocation armorResource = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, texturePath);

                ps.pushPose();

                boolean isRightArm = (pRendererArm == this.model.rightArm);

                float armorInflation = 1.05F;
                ps.scale(armorInflation, armorInflation, armorInflation);

                ps.translate(isRightArm ? 0.02D : -0.01, 0.02D, 0.0D);

                renderPart(ps, pBuffer, pCombinedLight, pRendererArm, armorResource, new float[]{1.0F, 1.0F, 1.0F});

                ps.popPose();

        }
    }

    private void renderTattoos(PoseStack stack, MultiBufferSource buffer, int light, ModelPart arm, StatsData stats) {
        if (stats.getEffects() != null && stats.getEffects().hasEffect("majin")) {
            renderPart(stack, buffer, light, arm, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/majinm.png"), new float[]{1,1,1});
        }

        int tattooType = stats.getCharacter().getTattooType();
        if (tattooType != 0) {
            ResourceLocation tattooLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/tattoos/tattoo_" + tattooType + ".png");
            renderPart(stack, buffer, light, arm, tattooLoc, new float[]{1,1,1});
        }
    }

    private void renderPart(PoseStack stack, MultiBufferSource buffer, int light, ModelPart part, ResourceLocation texture, float[] rgb) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(texture));
        part.render(stack, vc, light, OverlayTexture.NO_OVERLAY, rgb[0], rgb[1], rgb[2], 1.0F);
    }

    private ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
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
        VertexConsumer vc = buffer.getBuffer(ModRenderTypes.kiblast(KI_WEAPON_TEX));
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

    private float[] applyKaiokenTint(float[] rgb, int phase) {
        if (phase <= 0) return rgb;

        float intensity = Math.min(0.6f, phase * 0.1f);

        float newR = rgb[0] * (1.0f - intensity) + (1.0f * intensity);
        float newG = rgb[1] * (1.0f - intensity);
        float newB = rgb[2] * (1.0f - intensity);

        return new float[]{newR, newG, newB};
    }

    private void queueFirstPersonAura(AbstractClientPlayer player, PoseStack poseStack, int packedLight) {
        float partialTick = Minecraft.getInstance().getFrameTime();
        AuraRenderQueue.addFirstPersonAura(player, poseStack, partialTick, packedLight);
    }
}
