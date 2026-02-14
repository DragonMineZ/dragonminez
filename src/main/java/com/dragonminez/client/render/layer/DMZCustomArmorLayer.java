package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.DbzArmorItem;
    import com.dragonminez.common.stats.StatsCapability;
    import com.dragonminez.common.stats.StatsProvider;
    import com.mojang.blaze3d.vertex.PoseStack;
    import com.mojang.blaze3d.vertex.VertexConsumer;
    import net.minecraft.client.player.AbstractClientPlayer;
    import net.minecraft.client.renderer.MultiBufferSource;
    import net.minecraft.client.renderer.RenderType;
    import net.minecraft.client.renderer.texture.OverlayTexture;
    import net.minecraft.resources.ResourceLocation;
    import net.minecraft.world.entity.EquipmentSlot;
    import net.minecraft.world.entity.LivingEntity;
    import net.minecraft.world.item.ArmorItem;
    import net.minecraft.world.item.DyeableArmorItem;
    import net.minecraft.world.item.ItemStack;
    import net.minecraftforge.client.ForgeHooksClient;
    import net.minecraftforge.registries.ForgeRegistries;
    import software.bernie.geckolib.cache.object.BakedGeoModel;
    import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
    import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

    import java.util.HashMap;
    import java.util.Map;

public class DMZCustomArmorLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {

    private static final ResourceLocation MAJIN_ARMOR_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "geo/armor/armormajinfat.geo.json");
    private static final ResourceLocation MAJIN_SLIM_ARMOR_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "geo/armor/armormajinslim.geo.json");
    private static final ResourceLocation OOZARU_ARMOR_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
            "geo/armor/armoroozaru.geo.json");

    public DMZCustomArmorLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel playerModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        ItemStack stack = animatable.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) return;

        var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
        if (stats == null) return;

        if (stats.getCharacter().getArmored()) {
            return;
        }

        String race = stats.getCharacter().getRaceName().toLowerCase();
        String gender = stats.getCharacter().getGender().toLowerCase();
        String currentForm = stats.getCharacter().getActiveForm();

        boolean isVanilla = ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace().equals("minecraft");
        boolean isDbzArmor = stack.getItem() instanceof DbzArmorItem;
		boolean isPothala = stack.getItem().getDescriptionId().contains("pothala");

        if (isPothala || (!isVanilla && !isDbzArmor)) return;

        boolean shouldRender = false;
        boolean isSlimTarget = false;
        boolean isOozaruTarget = false;

        if (race.equals("saiyan") && "oozaru".equalsIgnoreCase(currentForm)) {
            shouldRender = true;
            isOozaruTarget = true;
        } else if (race.equals("majin") && gender.equals("male")) {
            shouldRender = true;
            isSlimTarget = false;
        } else if (gender.equals("female")) {
            if (race.equals("majin") || race.equals("human") || race.equals("saiyan")) {
                shouldRender = true;
                isSlimTarget = true;
            }
        }

        if (!shouldRender) return;

        if (isDbzArmor) {
            ResourceLocation texture = getDbzArmorTexture((DbzArmorItem) stack.getItem(), stack);
            Map<String, float[]> originalScales = new HashMap<>();
            Map<String, Boolean> originalVisibility = new HashMap<>();

            saveStateRecursively(playerModel, originalScales, originalVisibility);

            poseStack.pushPose();

            float translateY = isOozaruTarget ? -0.087f : -0.025f;
            float inflation = isOozaruTarget ? 1.021f : 1.02f;

            poseStack.translate(0, translateY, 0);
            inflateRecursively(playerModel, inflation);

            applyBodyOnlyVisibility(playerModel);

            renderModel(playerModel, poseStack, bufferSource, animatable, texture, 1.0F, 1.0F, 1.0F, partialTick, packedLight);

            poseStack.popPose();
            restoreStateRecursively(playerModel, originalScales, originalVisibility);
        } else {
            ResourceLocation targetModelLoc;
            if (isOozaruTarget) {
                targetModelLoc = OOZARU_ARMOR_MODEL;
            } else {
                targetModelLoc = isSlimTarget ? MAJIN_SLIM_ARMOR_MODEL : MAJIN_ARMOR_MODEL;
            }

            BakedGeoModel vanillaArmorModel = getGeoModel().getBakedModel(targetModelLoc);

            if (vanillaArmorModel != null) {
                ResourceLocation texture = getVanillaArmorTexture(animatable, stack, EquipmentSlot.CHEST, null);

                for (GeoBone bone : vanillaArmorModel.topLevelBones()) {
                    syncBoneRecursively(bone, playerModel);
                }

				poseStack.pushPose();
				poseStack.translate(0, -0.05f, 0);
                renderModel(vanillaArmorModel, poseStack, bufferSource, animatable, texture, 1.0F, 1.0F, 1.0F, partialTick, packedLight);
				poseStack.popPose();

                if (armorItem instanceof DyeableArmorItem) {
                    ResourceLocation overlayTex = getVanillaArmorTexture(animatable, stack, EquipmentSlot.CHEST, "overlay");
                    renderModel(vanillaArmorModel, poseStack, bufferSource, animatable, overlayTex, 1f, 1f, 1f, partialTick, packedLight);
                }
            }
        }
    }

    private void applyBodyOnlyVisibility(BakedGeoModel model) {
        for (GeoBone bone : model.topLevelBones()) {
            setRecursiveVisible(bone, false);
        }

        model.getBone("body").ifPresent(body -> {
            body.setHidden(false);

            GeoBone parent = body.getParent();
            while (parent != null) {
                parent.setHidden(false);
                parent = parent.getParent();
            }

            for (GeoBone child : body.getChildBones()) {
                child.setHidden(true);
                setRecursiveVisible(child, false);
            }
        });
    }

    private void setRecursiveVisible(GeoBone bone, boolean visible) {
        bone.setHidden(!visible);
        for (GeoBone child : bone.getChildBones()) {
            setRecursiveVisible(child, visible);
        }
    }

    private void saveStateRecursively(BakedGeoModel model, Map<String, float[]> scales, Map<String, Boolean> visibility) {
        for (GeoBone bone : model.topLevelBones()) {
            saveBoneState(bone, scales, visibility);
        }
    }

    private void saveBoneState(GeoBone bone, Map<String, float[]> scales, Map<String, Boolean> visibility) {
        scales.put(bone.getName(), new float[]{bone.getScaleX(), bone.getScaleY(), bone.getScaleZ()});
        visibility.put(bone.getName(), bone.isHidden());

        for (GeoBone child : bone.getChildBones()) {
            saveBoneState(child, scales, visibility);
        }
    }

    private void inflateRecursively(BakedGeoModel model, float inflation) {
        for (GeoBone bone : model.topLevelBones()) {
            inflateBone(bone, inflation);
        }
    }

    private void inflateBone(GeoBone bone, float inflation) {
        bone.setScaleX(bone.getScaleX() * inflation);
        bone.setScaleY(bone.getScaleY() * inflation);
        bone.setScaleZ(bone.getScaleZ() * inflation);

        for (GeoBone child : bone.getChildBones()) {
            inflateBone(child, inflation);
        }
    }

    private void restoreStateRecursively(BakedGeoModel model, Map<String, float[]> scales, Map<String, Boolean> visibility) {
        for (GeoBone bone : model.topLevelBones()) {
            restoreBoneState(bone, scales, visibility);
        }
    }

    private void restoreBoneState(GeoBone bone, Map<String, float[]> scales, Map<String, Boolean> visibility) {
        if (scales.containsKey(bone.getName())) {
            float[] s = scales.get(bone.getName());
            bone.setScaleX(s[0]);
            bone.setScaleY(s[1]);
            bone.setScaleZ(s[2]);
        }
        if (visibility.containsKey(bone.getName())) {
            bone.setHidden(visibility.get(bone.getName()));
        }

        for (GeoBone child : bone.getChildBones()) {
            restoreBoneState(child, scales, visibility);
        }
    }

    private void syncBoneRecursively(GeoBone destBone, BakedGeoModel sourceModel) {
        sourceModel.getBone(destBone.getName()).ifPresent(sourceBone -> {
            destBone.setRotX(sourceBone.getRotX());
            destBone.setRotY(sourceBone.getRotY());
            destBone.setRotZ(sourceBone.getRotZ());
            destBone.setPosX(sourceBone.getPosX());
            destBone.setPosY(sourceBone.getPosY());
            destBone.setPosZ(sourceBone.getPosZ());
            destBone.setPivotX(sourceBone.getPivotX());
            destBone.setPivotY(sourceBone.getPivotY());
            destBone.setPivotZ(sourceBone.getPivotZ());

            float inflation = 1.05f;
            destBone.setScaleX(sourceBone.getScaleX() * inflation);
            destBone.setScaleY(sourceBone.getScaleY() * inflation);
            destBone.setScaleZ(sourceBone.getScaleZ() * inflation);
        });

        for (GeoBone child : destBone.getChildBones()) {
            syncBoneRecursively(child, sourceModel);
        }
    }

    private ResourceLocation getDbzArmorTexture(DbzArmorItem item, ItemStack stack) {
        String itemId = item.getItemId();
        boolean isDamaged = false;
        if (item.isDamageOn()) {
            isDamaged = stack.getDamageValue() > stack.getMaxDamage() / 2;
        }
        String suffix = isDamaged ? "_damaged_layer1.png" : "_layer1.png";
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/" + itemId + suffix);
    }

    private ResourceLocation getVanillaArmorTexture(LivingEntity entity, ItemStack stack, EquipmentSlot slot, String type) {
        ArmorItem item = (ArmorItem) stack.getItem();
        String materialName = item.getMaterial().getName();
        String domain = "minecraft";
        if (materialName.contains(":")) {
            String[] split = materialName.split(":", 2);
            domain = split[0];
            materialName = split[1];
        } else {
            ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(item);
            if (itemRegistryName != null) domain = itemRegistryName.getNamespace();
        }
        String typeSuffix = (type == null || type.isEmpty()) ? "" : "_" + type;
        String textureLocation = String.format("%s:textures/models/armor/%s_layer_1%s.png", domain, materialName, typeSuffix);
        return ResourceLocation.parse(ForgeHooksClient.getArmorTexture(entity, stack, textureLocation, slot, type));
    }

    private void renderModel(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, ResourceLocation texture, float r, float g, float b, float partialTick, int packedLight) {
        RenderType armorRenderType = RenderType.armorCutoutNoCull(texture);
        getRenderer().reRender(model, poseStack, bufferSource, animatable, armorRenderType,
                bufferSource.getBuffer(armorRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                r, g, b, 1.0f);
    }
}