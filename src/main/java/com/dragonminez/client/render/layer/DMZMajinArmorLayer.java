package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.data.DMZAnimatable;
import com.dragonminez.client.util.BoneVisibilityHandler;
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
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Optional;

public class DMZMajinArmorLayer extends GeoRenderLayer<DMZAnimatable> {

    private static final ResourceLocation MAJIN_ARMOR_MODEL = new ResourceLocation(Reference.MOD_ID,
            "geo/armor/armormajinfat.geo.json");
    private static final ResourceLocation MAJIN_SLIM_ARMOR_MODEL = new ResourceLocation(Reference.MOD_ID,
            "geo/armor/armormajinslim.geo.json");

    private static final ResourceLocation MAJIN_FAT_FULL_MODEL = new ResourceLocation(Reference.MOD_ID,
            "geo/entity/races/majin.geo.json");
    private static final ResourceLocation MAJIN_SLIM_FULL_MODEL = new ResourceLocation(Reference.MOD_ID,
            "geo/entity/races/majin_slim.geo.json");

    public static boolean IS_RENDERING_CUSTOM_ARMOR = false;

    public DMZMajinArmorLayer(GeoRenderer<DMZAnimatable> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, DMZAnimatable animatable, BakedGeoModel playerModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (!(this.getRenderer() instanceof DMZPlayerRenderer playerRenderer)) return;
        AbstractClientPlayer player = playerRenderer.getCurrentEntity();
        if (player == null) return;

        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) return;

        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null || !stats.getCharacter().getRaceName().equals("majin")) return;

        boolean isFemale = stats.getCharacter().getGender().equals("female");
        boolean isVanilla = ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace().equals("minecraft");

        BakedGeoModel modelToRender;
        ResourceLocation texture;

        // Variables para Custom Mode
        float storedScaleX = 1f, storedScaleY = 1f, storedScaleZ = 1f;
        GeoBone modifiedBone = null;

        // ==================================================================
        // CASO 1: VANILLA (Requiere Sync + Inflado)
        // ==================================================================
        if (isVanilla) {
            ResourceLocation targetLoc = isFemale ? MAJIN_SLIM_ARMOR_MODEL : MAJIN_ARMOR_MODEL;
            modelToRender = getGeoModel().getBakedModel(targetLoc);
            texture = getVanillaArmorTexture(player, stack, EquipmentSlot.CHEST, null);

            if (modelToRender != null) {
                // Sincronizar CADA hueso con el jugador e INFLARLO (1.15f)
                // Esto es vital para que la armadura externa siga al jugador.
                for (GeoBone bone : modelToRender.topLevelBones()) {
                    syncBoneRecursively(bone, playerModel, true); // true = inflar
                }

                // Render
                renderModel(modelToRender, poseStack, bufferSource, animatable, texture, 1.0F, 1.0F, 1.0F, partialTick, packedLight);

                // Overlay (Cuero, etc)
                if (armorItem instanceof DyeableArmorItem) {
                    ResourceLocation overlayTex = getVanillaArmorTexture(player, stack, EquipmentSlot.CHEST, "overlay");
                    renderModel(modelToRender, poseStack, bufferSource, animatable, overlayTex, 1f, 1f, 1f, partialTick, packedLight);
                }
            }
        }
        // ==================================================================
        // CASO 2: CUSTOM (Requiere No-Sync + Inflado Local)
        // ==================================================================
        else {
            ResourceLocation targetLoc = isFemale ? MAJIN_SLIM_FULL_MODEL : MAJIN_FAT_FULL_MODEL;
            modelToRender = getGeoModel().getBakedModel(targetLoc);
            texture = getCustomArmorTexture(stack);

            if (modelToRender != null) {

                // 1. Preparar Visibilidad
                prepareCustomModelVisibility(modelToRender);

                // 2. Inflar body (Guardando estado original)
                if (modelToRender.getBone("body").isPresent()) {
                    modifiedBone = modelToRender.getBone("body").get();
                    storedScaleX = modifiedBone.getScaleX();
                    storedScaleY = modifiedBone.getScaleY();
                    storedScaleZ = modifiedBone.getScaleZ();

                    float inflation = 1.08f;
                    modifiedBone.setScaleX(storedScaleX * inflation);
                    modifiedBone.setScaleY(storedScaleY * inflation);
                    modifiedBone.setScaleZ(storedScaleZ * inflation);

                }

                // 3. Render
                float r = 1.0F, g = 1.0F, b = 1.0F;
                if (armorItem instanceof DyeableArmorItem dyeable) {
                    int color = dyeable.getColor(stack);
                    r = (float)(color >> 16 & 255) / 255.0F;
                    g = (float)(color >> 8 & 255) / 255.0F;
                    b = (float)(color & 255) / 255.0F;
                }
                renderModel(modelToRender, poseStack, bufferSource, animatable, texture, r, g, b, partialTick, packedLight);

                if (armorItem instanceof DyeableArmorItem) {
                    ResourceLocation overlayTex = getCustomArmorTexture(stack);
                    renderModel(modelToRender, poseStack, bufferSource, animatable, overlayTex, 1f, 1f, 1f, partialTick, packedLight);
                }

                // 4. Restaurar
                if (modifiedBone != null) {
                    modifiedBone.setScaleX(storedScaleX);
                    modifiedBone.setScaleY(storedScaleY);
                    modifiedBone.setScaleZ(storedScaleZ);
                }
                resetCustomModelVisibility(modelToRender);

                // 5. Re-aplicar visibilidad del jugador
                BoneVisibilityHandler.updateVisibility(modelToRender, player);
            }
        }
    }

    // =====================================================================
    // LÓGICA DE SYNC (Para Vanilla)
    // =====================================================================

    private void syncBoneRecursively(GeoBone destBone, BakedGeoModel sourceModel, boolean inflate) {
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

            // Inflado para Vanilla (1.15x) o Copia exacta (1.0x)
            float scaleMult = inflate ? 1.06f : 1.06f;
            destBone.setScaleX(sourceBone.getScaleX() * scaleMult);
            destBone.setScaleY(sourceBone.getScaleY() * scaleMult);
            destBone.setScaleZ(sourceBone.getScaleZ() * scaleMult);
        });

        for (GeoBone child : destBone.getChildBones()) {
            syncBoneRecursively(child, sourceModel, inflate);
        }
    }

    // =====================================================================
    // LÓGICA DE VISIBILIDAD (Para Custom)
    // =====================================================================

    private void prepareCustomModelVisibility(BakedGeoModel model) {
        for (GeoBone bone : model.topLevelBones()) {
            setRecursiveVisible(bone, false);
        }
        model.getBone("armorBody").ifPresent(armorBody -> {
            unhideParents(armorBody);
            armorBody.setHidden(false);
            for (GeoBone child : armorBody.getChildBones()) {
                child.setHidden(true);
                setRecursiveVisible(child, false);
            }
        });
    }

    private void resetCustomModelVisibility(BakedGeoModel model) {
        for (GeoBone bone : model.topLevelBones()) {
            setRecursiveVisible(bone, true);
        }
    }

    // =====================================================================
    // UTILS
    // =====================================================================

    private ResourceLocation getCustomArmorTexture(ItemStack stack) {
        // Verificamos si es tu item custom
        if (stack.getItem() instanceof DbzArmorItem dbzItem) {
            // Obtenemos el ID exacto (ej: "blackgoku")
            String textureId = dbzItem.getItemId();

            // Opcional: Si quieres soportar la textura de daño igual que tu item original:
            boolean isDamaged = dbzItem.isDamageOn() && (stack.getDamageValue() > stack.getMaxDamage() / 2);
            String suffix = isDamaged ? "_damaged_layer1.png" : "_layer1.png";

            // Construimos la ruta: assets/dragonminez/textures/armor/blackgoku_layer1.png
            return new ResourceLocation(Reference.MOD_ID, "textures/armor/" + textureId + suffix);
        }

        // Fallback por si acaso no es DbzArmorItem pero entró aquí
        return new ResourceLocation(Reference.MOD_ID, "textures/armor/error.png");
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
        return new ResourceLocation(ForgeHooksClient.getArmorTexture(entity, stack, textureLocation, slot, type));
    }

    private void renderModel(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, DMZAnimatable animatable, ResourceLocation texture, float r, float g, float b, float partialTick, int packedLight) {
        RenderType armorRenderType = RenderType.armorCutoutNoCull(texture);
        getRenderer().reRender(model, poseStack, bufferSource, animatable, armorRenderType,
                bufferSource.getBuffer(armorRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                r, g, b, 1.0f);
    }

    private void setRecursiveVisible(GeoBone bone, boolean visible) {
        bone.setHidden(!visible);
        for (GeoBone child : bone.getChildBones()) {
            setRecursiveVisible(child, visible);
        }
    }

    private void unhideParents(GeoBone bone) {
        GeoBone parent = bone.getParent();
        while (parent != null) {
            parent.setHidden(false);
            parent = parent.getParent();
        }
    }
}