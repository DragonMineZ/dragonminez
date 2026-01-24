package com.dragonminez.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.render.layer.*;
import com.dragonminez.client.util.BoneVisibilityHandler;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.BioAndroidForms;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PlayerDMZRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoEntityRenderer<T> {

    private static final Map<ResourceLocation, Boolean> TEXTURE_CACHE = new HashMap<>();

    public PlayerDMZRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);

        this.addRenderLayer(new DMZPlayerItemInHandLayer(this));
        this.addRenderLayer(new DMZPlayerArmorLayer<>(this));
        this.addRenderLayer(new DMZCustomArmorLayer(this));
        this.addRenderLayer(new DMZSkinLayer<>(this));
        this.addRenderLayer(new DMZHairLayer<>(this));
        this.addRenderLayer(new DMZRacePartsLayer(this));

    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        BoneVisibilityHandler.updateVisibility(model, animatable);

    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity == null) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, entity);
        var stats = statsCap.orElse(new StatsData(entity));
        var character = stats.getCharacter();
        var activeForm = character.getActiveFormData();

        String race = character.getRaceName().toLowerCase();
        String currentForm = character.getActiveForm();

        float scaling;

        if (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU)) ||
                (Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) {
            scaling = 1.0f;
        } else {
            if (activeForm != null) {
                scaling = activeForm.getModelScaling();
            } else {
                scaling = (float) character.getModelScaling();
            }
        }

        poseStack.pushPose();

        boolean isFlying = false;
        Skill flySkill = stats.getSkills().getSkill("fly");
        if (flySkill != null && flySkill.isActive()) {
            isFlying = true;
        }

		if (isFlying || FlightRollHandler.hasActiveRoll()) {
			float roll = FlightRollHandler.getRoll(partialTick);
			if (Math.abs(roll) > 0.1F) {
				poseStack.translate(0.0F, 0.9F * scaling, 0.0F);
				poseStack.mulPose(Axis.ZN.rotationDegrees(roll));
				poseStack.translate(0.0F, -0.9F * scaling, 0.0F);
			}
		}

        poseStack.scale(scaling, scaling, scaling);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        this.shadowRadius = 0.4f * scaling;

        poseStack.popPose();
    }

    public void renderHand(PoseStack ps, MultiBufferSource bs, int pl, T player, HumanoidArm arm) {
        PlayerRenderer playerRenderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().get("default");
        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();

        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(new StatsData(player));
        var character = stats.getCharacter();

        ps.pushPose();

        model.attackTime = player.getAttackAnim(Minecraft.getInstance().getFrameTime());
        model.setupAnim(player, 0, 0, 0, 0, 0);

        renderBodyHand(ps, bs, pl, player, model, character, arm);
        renderTattoosHand(ps, bs, pl, model, stats, arm);
        renderArmorHand(ps, bs, pl, player, model, arm);

        ps.popPose();
    }

    private void renderArmorHand(PoseStack ps, MultiBufferSource bs, int pl, T player, PlayerModel<AbstractClientPlayer> model, HumanoidArm arm) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);

        if (!chestStack.isEmpty() && chestStack.getItem() instanceof DbzArmorItem armorItem) {
            String texturePath = "textures/armor/" + armorItem.getItemId() + "_layer1.png";
            ResourceLocation armorResource = new ResourceLocation(Reference.MOD_ID, texturePath);

            ps.pushPose();

            float armorInflation = 1.01F;
            ps.scale(armorInflation, armorInflation, armorInflation);
            ps.translate(arm == HumanoidArm.RIGHT ? -0.00D : 0.00D, 0.02D, 0.0D);

            dibujarBrazo(ps, bs, pl, model, armorResource, new float[]{1, 1, 1}, arm);

            ps.popPose();
        }
    }

    private void renderBodyHand(PoseStack ps, MultiBufferSource bs, int pl, T player, PlayerModel<AbstractClientPlayer> model, com.dragonminez.common.stats.Character character, HumanoidArm arm) {
        String race = character.getRace().toLowerCase();
        String gender = character.getGender().toLowerCase();
        int bodyType = character.getBodyType();
        String form = character.getActiveForm();
        boolean hasForm = (form != null && !form.isEmpty() && !form.equals("base"));

        float[] b1 = ColorUtils.hexToRgb(character.getBodyColor());
        float[] b2 = ColorUtils.hexToRgb(character.getBodyColor2());
        float[] b3 = ColorUtils.hexToRgb(character.getBodyColor3());
        float[] h = ColorUtils.hexToRgb(character.getHairColor());

        if (hasForm && character.getActiveFormData() != null) {
            var f = character.getActiveFormData();
            if (!f.getBodyColor1().isEmpty()) b1 = ColorUtils.hexToRgb(f.getBodyColor1());
            if (!f.getBodyColor2().isEmpty()) b2 = ColorUtils.hexToRgb(f.getBodyColor2());
            if (!f.getBodyColor3().isEmpty()) b3 = ColorUtils.hexToRgb(f.getBodyColor3());
            if (!f.getHairColor().isEmpty()) h = ColorUtils.hexToRgb(f.getHairColor());
        }

        if (race.equals("bioandroid") && (Objects.equals(form, BioAndroidForms.PERFECT) || Objects.equals(form, BioAndroidForms.SUPER_PERFECT))) {
            b2 = new float[]{1.0f, 1.0f, 1.0f};
        }

        if (race.equals("saiyan") && (Objects.equals(form, SaiyanForms.OOZARU) || Objects.equals(form, SaiyanForms.GOLDEN_OOZARU))) {
            float[] fur = Objects.equals(form, SaiyanForms.GOLDEN_OOZARU) ? ColorUtils.hexToRgb("#FFD700") : ColorUtils.hexToRgb("#6B1E0E");
            float[] skin = ColorUtils.hexToRgb("#CC978D");
            dibujarBrazo(ps, bs, pl, model, "textures/entity/races/humansaiyan/oozaru_layer1.png", fur, arm);
            dibujarBrazo(ps, bs, pl, model, "textures/entity/races/humansaiyan/oozaru_layer2.png", skin, arm);
            return;
        }

        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
        boolean isStandard = race.equals("human") || race.equals("saiyan");

        if ((raceConfig != null && raceConfig.useVanillaSkin()) || (isStandard && bodyType == 0)) {
            dibujarBrazo(ps, bs, pl, model, player.getSkinTextureLocation(), new float[]{1,1,1}, arm);
            return;
        }

        if (Arrays.asList("namekian", "frostdemon", "bioandroid", "majin").contains(race)) {
            renderSpecializedHand(ps, bs, pl, model, race, form, bodyType, gender, b1, b2, b3, h, arm);
        } else {
            String textureBase = isStandard ? "humansaiyan" : race;
            String genderPart = (raceConfig != null && raceConfig.hasGender()) ? "_" + gender : "";
            String path = "textures/entity/races/" + textureBase + "/bodytype" + genderPart + "_" + bodyType + ".png";
            dibujarBrazo(ps, bs, pl, model, path, b1, arm);
        }
    }

    private void renderSpecializedHand(PoseStack ps, MultiBufferSource bs, int pl, PlayerModel<AbstractClientPlayer> model, String race, String form, int type, String gender, float[] b1, float[] b2, float[] b3, float[] h, HumanoidArm arm) {
        String prefix;
        boolean isFrost = race.equals("frostdemon");
        boolean isBio = race.equals("bioandroid");

        if (isFrost && (Objects.equals(form, FrostDemonForms.FINAL_FORM) || Objects.equals(form, FrostDemonForms.FULLPOWER))) {
            prefix = "textures/entity/races/frostdemon/finalform_bodytype_" + type + "_";
            dibujarBrazo(ps, bs, pl, model, prefix + "layer1.png", b1, arm);
            dibujarBrazo(ps, bs, pl, model, prefix + "layer2.png", (type == 1 ? b2 : h), arm);
            if (type == 1) {
                dibujarBrazo(ps, bs, pl, model, prefix + "layer3.png", b3, arm);
                dibujarBrazo(ps, bs, pl, model, prefix + "layer4.png", h, arm);
            }
        } else {
            String fName = (form == null || form.isEmpty()) ? "base" : form.toLowerCase();
            if (isBio) prefix = "textures/entity/races/bioandroid/" + fName + "_" + type + "_";
            else if (race.equals("majin")) prefix = "textures/entity/races/majin/" + fName + "_" + type + "_" + gender + "_";
            else prefix = "textures/entity/races/" + race + "/bodytype_" + type + "_";

            dibujarBrazo(ps, bs, pl, model, prefix + "layer1.png", b1, arm);
            dibujarBrazo(ps, bs, pl, model, prefix + "layer2.png", b2, arm);
            dibujarBrazo(ps, bs, pl, model, prefix + "layer3.png", b3, arm);
            if (isFrost || isBio) dibujarBrazo(ps, bs, pl, model, prefix + "layer4.png", h, arm);
            if (isBio || (isFrost && type == 0)) dibujarBrazo(ps, bs, pl, model, prefix + "layer5.png", ColorUtils.hexToRgb("#e67d40"), arm);
        }
    }

    private void renderTattoosHand(PoseStack ps, MultiBufferSource bs, int pl, PlayerModel<AbstractClientPlayer> model, StatsData stats, HumanoidArm arm) {
        if (stats.getEffects() != null && stats.getEffects().hasEffect("majin")) {
            dibujarBrazo(ps, bs, pl, model, "textures/entity/races/majinm.png", new float[]{1,1,1}, arm);
        }
        int tattoo = stats.getCharacter().getTattooType();
        if (tattoo != 0) {
            dibujarBrazo(ps, bs, pl, model, "textures/entity/races/tattoos/tattoo_" + tattoo + ".png", new float[]{1,1,1}, arm);
        }
    }

    private void dibujarBrazo(PoseStack ps, MultiBufferSource bs, int pl, PlayerModel<AbstractClientPlayer> model, Object texture, float[] rgb, HumanoidArm arm) {
        ResourceLocation loc = (texture instanceof ResourceLocation rl) ? rl : new ResourceLocation(Reference.MOD_ID, texture.toString());
        if (textureExists(loc)) {
            VertexConsumer builder = bs.getBuffer(RenderType.entityTranslucent(loc));
            if (arm == HumanoidArm.RIGHT) {
                model.rightArm.xRot = 0;
                model.rightArm.render(ps, builder, pl, OverlayTexture.NO_OVERLAY, rgb[0], rgb[1], rgb[2], 1.0F);
            } else {
                model.leftArm.xRot = 0;
                model.leftArm.render(ps, builder, pl, OverlayTexture.NO_OVERLAY, rgb[0], rgb[1], rgb[2], 1.0F);
            }
        }
    }

    private boolean textureExists(ResourceLocation location) {
        return TEXTURE_CACHE.computeIfAbsent(location, loc -> Minecraft.getInstance().getResourceManager().getResource(loc).isPresent());
    }
}
