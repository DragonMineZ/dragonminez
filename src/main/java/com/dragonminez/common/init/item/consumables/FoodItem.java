package com.dragonminez.common.init.item.consumables;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class FoodItem extends Item {
    @Nullable
    private final String tooltipKey;

    public FoodItem(int hunger, float saturation, int maxStack) {
        this(hunger, saturation, maxStack, null);
    }

    public FoodItem(int hunger, float saturation, int maxStack, @Nullable String tooltipKey) {
        this(hunger, saturation, maxStack, tooltipKey, null);
    }

    public FoodItem(int hunger, float saturation, int maxStack, @Nullable String tooltipKey, @Nullable Supplier<MobEffectInstance> effect) {
        super(new Properties().stacksTo(maxStack).food(food(hunger, saturation, effect)));
        this.tooltipKey = tooltipKey;
    }

    private static FoodProperties food(int hunger, float saturation, @Nullable Supplier<MobEffectInstance> effect) {
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(hunger)
                .saturationMod(saturation)
                .meat()
                .alwaysEat();
        if (effect != null) builder.effect(effect, 1.0F);
        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (this.tooltipKey != null) tooltip.add(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
