package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.StatsData;
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

import java.util.Set;

public class DMZCustomArmorLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {

    private static final ResourceLocation MAJIN_ARMOR_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/armor/armormajinfat.geo.json");
    private static final ResourceLocation MAJIN_SLIM_ARMOR_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/armor/armormajinslim.geo.json");
    private static final ResourceLocation OOZARU_ARMOR_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/armor/armoroozaru.geo.json");

    private static final Set<String> SLIM_SUPPORTED_MODELS = Set.of(
            "majin_evil", "majin_kid", "majin_super", "majin_ultra", "majin", "saiyan", "human", "ssj4gt", "ssj4d"
    );

    public DMZCustomArmorLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone playerBone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isSpectator() || !"body".equals(playerBone.getName())) return;

        ItemStack stack = resolveChestArmorStack(animatable);
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) return;

        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(new StatsData(animatable));
        if (stats.getCharacter().getArmored()) return;

        ArmorRenderContext ctx = resolveArmorContext(stats, stack);
        if (!ctx.shouldRender()) return;

        if (ctx.isDbzArmor()) {
            ResourceLocation texture = getDbzArmorTexture((DbzArmorItem) stack.getItem(), stack);
            float translateY = resolveCustomArmorTranslateY(ctx);
            float inflation = ctx.isOozaruTarget() ? 1.021f : 1.035f;

            poseStack.pushPose();
            poseStack.translate(0, translateY, 0);

            GeoBone armorBody = getChild(playerBone, "armorBody");
            GeoBone armorLeggingsBody = getChild(playerBone, "armorLeggingsBody");
            GeoBone bodyLayer = getChild(playerBone, "body_layer");

            boolean ob1 = armorBody != null && armorBody.isHidden();
            boolean ob2 = armorLeggingsBody != null && armorLeggingsBody.isHidden();
            boolean ob3 = bodyLayer != null && bodyLayer.isHidden();

            if (armorBody != null) armorBody.setHidden(true);
            if (armorLeggingsBody != null) armorLeggingsBody.setHidden(true);
            if (bodyLayer != null) bodyLayer.setHidden(true);

            renderRootBoneInflated(playerBone, poseStack, bufferSource, animatable, texture, partialTick, packedLight, inflation);

            if (armorBody != null) armorBody.setHidden(ob1);
            if (armorLeggingsBody != null) armorLeggingsBody.setHidden(ob2);
            if (bodyLayer != null) bodyLayer.setHidden(ob3);

            poseStack.popPose();
            return;
        }

        ResourceLocation targetModelLoc = ctx.isOozaruTarget() ? OOZARU_ARMOR_MODEL : (ctx.isSlimTarget() ? MAJIN_SLIM_ARMOR_MODEL : MAJIN_ARMOR_MODEL);
        BakedGeoModel vanillaArmorModel = getGeoModel().getBakedModel(targetModelLoc);
        if (vanillaArmorModel == null) return;

        GeoBone armorBodyBone = vanillaArmorModel.getBone("body").orElse(null);
        if (armorBodyBone == null && !vanillaArmorModel.topLevelBones().isEmpty()) {
            armorBodyBone = vanillaArmorModel.topLevelBones().get(0);
        }
        if (armorBodyBone == null) return;

        ResourceLocation texture = getVanillaArmorTexture(animatable, stack, EquipmentSlot.CHEST, null);

        float translateY = resolveCustomArmorTranslateY(ctx);
        poseStack.pushPose();
        poseStack.translate(0, translateY, 0);
        renderRootBoneInflated(armorBodyBone, poseStack, bufferSource, animatable, texture, partialTick, packedLight, 1.05f);
        poseStack.popPose();

        if (stack.getItem() instanceof DyeableArmorItem) {
            ResourceLocation overlayTex = getVanillaArmorTexture(animatable, stack, EquipmentSlot.CHEST, "overlay");

            poseStack.pushPose();
            poseStack.translate(0, translateY, 0);
            renderRootBoneInflated(armorBodyBone, poseStack, bufferSource, animatable, overlayTex, partialTick, packedLight, 1.05f);
            poseStack.popPose();
        }
    }

    private ItemStack resolveChestArmorStack(T animatable) {
        ItemStack stack = animatable.getItemBySlot(EquipmentSlot.CHEST);
        if (CosmeticArmorCompat.isLoaded()) {
            ItemStack cosmeticStack = CosmeticArmorCompat.getCosmeticStack(animatable, EquipmentSlot.CHEST);
            if (cosmeticStack != null) {
                if (cosmeticStack.isEmpty()) return ItemStack.EMPTY;
                stack = cosmeticStack;
            }
        }
        return stack;
    }

    private ArmorRenderContext resolveArmorContext(StatsData stats, ItemStack stack) {
        var character = stats.getCharacter();
        String raceName = character.getRaceName().toLowerCase();
        String gender = character.getGender().toLowerCase();

        var raceConfig = ConfigManager.getRaceCharacter(raceName);
        String raceCustomModel = (raceConfig != null && raceConfig.getCustomModel() != null) ? raceConfig.getCustomModel().toLowerCase() : "";
        String formCustomModel = (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasCustomModel())
                ? character.getActiveFormData().getCustomModel().toLowerCase() : "";

        String logicKey = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
        if (logicKey.isEmpty()) logicKey = raceName;

        ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        boolean isVanilla = itemKey != null && "minecraft".equals(itemKey.getNamespace());
        boolean isDbzArmor = stack.getItem() instanceof DbzArmorItem;
        boolean isPothala = stack.getItem().getDescriptionId().contains("pothala");
        if (isPothala || (!isVanilla && !isDbzArmor)) {
            return new ArmorRenderContext(false, false, false, false, isDbzArmor);
        }

        boolean shouldRender = false;
        boolean isSlimTarget = false;
        boolean isOozaruTarget = false;
        boolean isMajinGordoTarget = false;

        if (character.isOozaruCached() || logicKey.equals("oozaru")) {
            shouldRender = true;
            isOozaruTarget = true;
        } else if (logicKey.contains("buffed") || logicKey.contains("frostdemon_fp") || logicKey.contains("majin_ultra")
                || logicKey.contains("namekian_orange") || logicKey.contains("bioandroid_ultra") || logicKey.contains("ssj4gt") || logicKey.contains("ssj4d")
                || logicKey.contains("frostdemon_fifth")) {
            if (isDbzArmor) shouldRender = true;
        } else if ((logicKey.equals("majin") && (gender.equals(Character.GENDER_MALE))
                || (raceName.equals("majin") && (gender.equals(Character.GENDER_MALE))) || logicKey.equals("janemba_fat"))) {
            shouldRender = true;
            isMajinGordoTarget = true;
        } else if (gender.equals(Character.GENDER_FEMALE)) {
            boolean isKnownModel = SLIM_SUPPORTED_MODELS.contains(logicKey);
            boolean hasGenderConfig = raceConfig != null && raceConfig.getHasGender();
            if (isKnownModel || hasGenderConfig) {
                shouldRender = true;
                isSlimTarget = true;
            }
        }

        return new ArmorRenderContext(shouldRender, isSlimTarget, isOozaruTarget, isMajinGordoTarget, isDbzArmor);
    }

    private float resolveCustomArmorTranslateY(ArmorRenderContext ctx) {
        if (ctx.isOozaruTarget() || ctx.isMajinGordoTarget()) return 0.03f;
        return 0.001f;
    }

    private void renderBaseArmorBodyFromPlayerBone(GeoBone playerBodyBone, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, ResourceLocation texture, float partialTick, int packedLight) {
        renderBaseArmorPiece(playerBodyBone, "armorBody", poseStack, bufferSource, animatable, texture, partialTick, packedLight);
        renderBaseArmorPiece(playerBodyBone, "armorBody2", poseStack, bufferSource, animatable, texture, partialTick, packedLight);
        renderBaseArmorPiece(playerBodyBone, "armorLeggingsBody", poseStack, bufferSource, animatable, texture, partialTick, packedLight);
    }

    private void renderBaseArmorPiece(GeoBone playerBodyBone, String boneName, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, ResourceLocation texture, float partialTick, int packedLight) {
        GeoBone armorPiece = getChild(playerBodyBone, boneName);
        if (armorPiece == null) return;

        boolean wasHidden = armorPiece.isHidden();
        if (wasHidden) armorPiece.setHidden(false);
        VisibilityState parentVisibility = unhideParentChain(armorPiece);

        renderChildBoneInflated(armorPiece, poseStack, bufferSource, animatable, texture, partialTick, packedLight, 1.1f);

        restoreParentChain(parentVisibility);
        if (wasHidden) armorPiece.setHidden(true);
    }

    private VisibilityState unhideParentChain(GeoBone bone) {
        java.util.List<GeoBone> parents = new java.util.ArrayList<>();
        java.util.List<Boolean> hiddenStates = new java.util.ArrayList<>();
        GeoBone parent = bone.getParent();
        while (parent != null) {
            parents.add(parent);
            hiddenStates.add(parent.isHidden());
            parent.setHidden(false);
            parent = parent.getParent();
        }
        return new VisibilityState(parents, hiddenStates);
    }

    private void restoreParentChain(VisibilityState state) {
        for (int i = 0; i < state.parents().size(); i++) {
            state.parents().get(i).setHidden(state.hiddenStates().get(i));
        }
    }

    private GeoBone getChild(GeoBone parent, String name) {
        for (GeoBone child : parent.getChildBones()) {
            if (child.getName().equals(name)) return child;
        }
        return null;
    }

    private static final Set<String> EXCLUDED_BONES = Set.of("tail1", "tail1m");

    private void renderRootBoneInflated(GeoBone targetBone, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, ResourceLocation texture, float partialTick, int packedLight, float inflation) {
        float rotX = targetBone.getRotX();
        float rotY = targetBone.getRotY();
        float rotZ = targetBone.getRotZ();
        float posX = targetBone.getPosX();
        float posY = targetBone.getPosY();
        float posZ = targetBone.getPosZ();
        float scaleX = targetBone.getScaleX();
        float scaleY = targetBone.getScaleY();
        float scaleZ = targetBone.getScaleZ();

        targetBone.setRotX(0);
        targetBone.setRotY(0);
        targetBone.setRotZ(0);
        targetBone.setPosX(0);
        targetBone.setPosY(0);
        targetBone.setPosZ(0);
        targetBone.setScaleX(inflation);
        targetBone.setScaleY(inflation);
        targetBone.setScaleZ(inflation);

        java.util.List<GeoBone> excludedFound = new java.util.ArrayList<>();
        java.util.List<Boolean> excludedHidden = new java.util.ArrayList<>();
        for (String name : EXCLUDED_BONES) {
            GeoBone bone = findBoneDeep(targetBone, name);
            if (bone != null) {
                excludedFound.add(bone);
                excludedHidden.add(bone.isHidden());
                bone.setHidden(true);
            }
        }

        RenderType armorRenderType = RenderType.armorCutoutNoCull(texture);
        getRenderer().renderRecursively(poseStack, animatable, targetBone, armorRenderType, bufferSource, bufferSource.getBuffer(armorRenderType), true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < excludedFound.size(); i++) {
            excludedFound.get(i).setHidden(excludedHidden.get(i));
        }

        targetBone.setRotX(rotX);
        targetBone.setRotY(rotY);
        targetBone.setRotZ(rotZ);
        targetBone.setPosX(posX);
        targetBone.setPosY(posY);
        targetBone.setPosZ(posZ);
        targetBone.setScaleX(scaleX);
        targetBone.setScaleY(scaleY);
        targetBone.setScaleZ(scaleZ);
    }

    private GeoBone findBoneDeep(GeoBone parent, String name) {
        for (GeoBone child : parent.getChildBones()) {
            if (child.getName().equals(name)) return child;
            GeoBone found = findBoneDeep(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private void renderChildBoneInflated(GeoBone targetBone, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, ResourceLocation texture, float partialTick, int packedLight, float inflation) {
        float scaleX = targetBone.getScaleX();
        float scaleY = targetBone.getScaleY();
        float scaleZ = targetBone.getScaleZ();

        targetBone.setScaleX(scaleX * inflation);
        targetBone.setScaleY(scaleY * inflation);
        targetBone.setScaleZ(scaleZ * inflation);

        RenderType armorRenderType = RenderType.armorCutoutNoCull(texture);
        getRenderer().renderRecursively(poseStack, animatable, targetBone, armorRenderType, bufferSource, bufferSource.getBuffer(armorRenderType), true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        targetBone.setScaleX(scaleX);
        targetBone.setScaleY(scaleY);
        targetBone.setScaleZ(scaleZ);
    }

    private ResourceLocation getDbzArmorTexture(DbzArmorItem item, ItemStack stack) {
        String itemId = item.getItemId();
        boolean isDamaged = item.isDamageOn() && stack.getDamageValue() > stack.getMaxDamage() / 2;
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

    private record ArmorRenderContext(boolean shouldRender, boolean isSlimTarget, boolean isOozaruTarget,
                                      boolean isMajinGordoTarget, boolean isDbzArmor) {
    }

    private record VisibilityState(java.util.List<GeoBone> parents, java.util.List<Boolean> hiddenStates) {
    }
}