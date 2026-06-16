package com.dragonminez.common.init.item;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class WeightItem extends DMZCuriosItem implements GeoItem {

	public enum WeightType {
		TURTLE_SHELL,
		WORKOUT_WEIGHTS,
		PICCOLO_CAPE
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	@Getter
	private final WeightType weightType;

	public WeightItem(Properties properties, WeightType weightType) {
		super(properties, CurioType.WEIGHTS);
		this.weightType = weightType;
	}

	public static int getWeight(ItemStack stack) {
		if (stack.isEmpty()) return 0;
		CompoundTag tag = stack.getTag();
		return tag != null ? tag.getInt("WeightValue") : 0;
	}

	public static void setWeight(ItemStack stack, int weight) {
		stack.getOrCreateTag().putInt("WeightValue", weight);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		int weight = getWeight(stack);
		if (weight > 0) {
			tooltip.add(Component.translatable("item.dragonminez.weight.tooltip", weight).withStyle(ChatFormatting.GOLD));
		}
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
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
