package com.dragonminez.common.init.armor;

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
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class DbzArmorCapeItem extends ArmorItem implements GeoItem, DbzArmorTextured {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Getter
    private final String itemId;


    public DbzArmorCapeItem(ArmorMaterial pMaterial, Type pType, Properties pProperties, String itemId) {
        super(pMaterial, pType, pProperties);
        this.itemId = itemId;
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return ArmorTextureResolver.resolve(itemId, slot, stack).toString();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
        }));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ArmorBaseModel model;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (model == null)
                    model = new ArmorBaseModel(Minecraft.getInstance().getEntityModels().bakeLayer(ArmorBaseModel.LAYER_LOCATION));
                return model;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
