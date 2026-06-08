package com.dragonminez.common.init.armor;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.common.init.armor.client.model.ArmorBaseModel;
import lombok.Getter;
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

@Getter
public class DbzArmorItem extends ArmorItem implements DbzArmorTextured {

    private final String modId;
    private final String itemId;

    public DbzArmorItem(ArmorMaterial pMaterial, Type pType, Properties pProperties, String itemId) {
        this(pMaterial, pType, pProperties, Reference.MOD_ID, itemId);
    }

    public DbzArmorItem(ArmorMaterial pMaterial, Type pType, Properties pProperties, String modId, String itemId) {
        super(pMaterial, pType, pProperties);
        this.modId = modId;
        this.itemId = itemId;
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return ArmorTextureResolver.resolve(modId, itemId, slot, stack).toString();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ArmorBaseModel model;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if(model == null)model = new ArmorBaseModel(Minecraft.getInstance().getEntityModels().bakeLayer(ArmorBaseModel.LAYER_LOCATION));
                return model;
            }
        });
    }
}
