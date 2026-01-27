package com.dragonminez.common.init.armor.client.render;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.client.model.ArmorCapeModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ArmorCapeRenderer extends GeoArmorRenderer<DbzArmorCapeItem> {
    public ArmorCapeRenderer() {
        super(new ArmorCapeModel());
    }

    @Override
    public ResourceLocation getTextureLocation(DbzArmorCapeItem animatable) {
        ItemStack stack = this.getCurrentStack();

        String itemId = animatable.getItemId();
        boolean isDamageOn = animatable.isDamageOn();

        if (itemId.contains("pothala")) {
            return new ResourceLocation(Reference.MOD_ID, "textures/armor/blank.png");
        }

        String basePath = "textures/armor/" + itemId;
        String layer;

        EquipmentSlot slot = this.getCurrentSlot();
        boolean isLegs = slot == EquipmentSlot.LEGS;

        String suffix = isLegs ? "_layer2.png" : "_layer1.png";

        if (isDamageOn) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getDamageValue();
            boolean isDamaged = currentDamage > maxDamage / 2;

            if (isDamaged) {
                suffix = isLegs ? "_damaged_layer2.png" : "_damaged_layer1.png";
            }
        }

        return new ResourceLocation(Reference.MOD_ID, basePath + suffix);
    }
}
