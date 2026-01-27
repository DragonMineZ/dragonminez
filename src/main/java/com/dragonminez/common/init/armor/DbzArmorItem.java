package com.dragonminez.common.init.armor;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.client.model.ArmorBaseModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class DbzArmorItem extends ArmorItem {

    private final String itemId;
    private final boolean isDamageOn;

    public DbzArmorItem(ArmorMaterial pMaterial, Type pType, Properties pProperties, String itemId, boolean isDamageOn) {
        super(pMaterial, pType, pProperties);
        this.itemId = itemId; // ID del item
        this.isDamageOn = isDamageOn;
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {

        String texturePath = Reference.MOD_ID + ":textures/armor/" + itemId;
		if (texturePath.contains("pothala")) return Reference.MOD_ID + ":textures/armor/blank.png";

        if(isDamageOn()){
            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getDamageValue();

            // Determinamos si la armadura está dañada (menos de la mitad de durabilidad)
            boolean isDamaged = currentDamage > maxDamage / 2;

            switch (slot) {
                case HEAD:
                    return texturePath + (isDamaged ? "_damaged_layer1.png" : "_layer1.png");
                case LEGS:
                    return texturePath + (isDamaged ? "_damaged_layer2.png" : "_layer2.png");
                case FEET:
                    return texturePath + (isDamaged ? "_damaged_layer1.png" : "_layer1.png");
                default:
                    return texturePath + (isDamaged ? "_damaged_layer1.png" : "_layer1.png");
            }
        } else {
            switch (slot) {
                case HEAD:
                    return texturePath + "_layer1.png";
                case LEGS:
                    return texturePath + "_layer2.png";
                case FEET:
                    return texturePath + "_layer1.png";
                default:
                    return texturePath + "_layer1.png";
            }

        }

    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private ArmorBaseModel model;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {

                if(model == null){
                    model = new ArmorBaseModel(Minecraft.getInstance().getEntityModels().bakeLayer(ArmorBaseModel.LAYER_LOCATION));
                }
                return model;

            }
        });
    }

    public String getItemId() {
        return itemId;
    }

    public boolean isDamageOn() {
        return isDamageOn;
    }
}