package com.dragonminez.common.init.item.farming;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Supplier;


public class StatPlantItem extends Item {

	public enum StatType {
		STR, SKP, RES, VIT, PWR, ENE, MASTERY
	}

	private static final int BUFF_DURATION = 20 * 15;
	private static final int KI_REGEN_DURATION = 20 * 5;
	private static final int REGEN_AMPLIFIER = 2;

	private final StatType statType;
	private final Supplier<? extends Block> cropBlock;

	public StatPlantItem(StatType statType, Supplier<? extends Block> cropBlock) {
		super(new Properties().food(new FoodProperties.Builder()
				.nutrition(1)
				.saturationMod(0.1f)
				.alwaysEat()
				.build()));
		this.statType = statType;
		this.cropBlock = cropBlock;
	}

	@Override
	public @NotNull InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		BlockState clickedState = level.getBlockState(clicked);
		BlockPos plantPos = clicked.above();

		// Solo planta si se hace clic sobre tierra de cultivo con espacio libre encima.
		if (clickedState.is(Blocks.FARMLAND) && level.isEmptyBlock(plantPos)) {
			if (!level.isClientSide) {
				level.setBlock(plantPos, cropBlock.get().defaultBlockState(), 3);
			}
			Player player = context.getPlayer();
			if (player == null || !player.getAbilities().instabuild) {
				context.getItemInHand().shrink(1);
			}
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		// PASS deja que el item se coma normalmente (comportamiento de comida).
		return InteractionResult.PASS;
	}

	@Override
	public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, Level level, @NotNull LivingEntity entity) {
		if (!level.isClientSide && entity instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) {
					player.displayClientMessage(Component.translatable("error.dmz.createcharacter")
							.withStyle(ChatFormatting.RED), true);
					return;
				}

				if (statType == StatType.MASTERY) {
					cleanse(player, data);
					consume(player, stack);
					player.displayClientMessage(Component.translatable("item.dragonminez.zenkai_lotus.use")
							.withStyle(ChatFormatting.LIGHT_PURPLE), true);
					return;
				}

				MobEffect effect = effectFor(statType);
				if (effect == null) return;

				// Anti-spam: si ya tiene el efecto, no se aplica de nuevo ni se consume la planta.
				if (player.hasEffect(effect)) {
					player.displayClientMessage(Component.translatable("item.dragonminez.stat_plant.active")
							.withStyle(ChatFormatting.RED), true);
					return;
				}

				int duration = statType == StatType.ENE ? KI_REGEN_DURATION : BUFF_DURATION;
				player.addEffect(new MobEffectInstance(effect, duration, 0, false, false, true));
				if (statType == StatType.VIT) {
					player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, BUFF_DURATION, REGEN_AMPLIFIER, false, false, true));
				}

				consume(player, stack);
				if (statType == StatType.ENE) {
					player.displayClientMessage(Component.translatable("item.dragonminez.kaioshin_fruit.use")
							.withStyle(ChatFormatting.AQUA), true);
				} else {
					player.displayClientMessage(Component.translatable("item.dragonminez.stat_plant.use", statType.name())
							.withStyle(ChatFormatting.GREEN), true);
				}
			});
		}
		return stack;
	}

	private void consume(ServerPlayer player, ItemStack stack) {
		player.getFoodData().eat(1, 0.1f);
		if (!player.isCreative()) {
			stack.shrink(1);
		}
	}

	private MobEffect effectFor(StatType type) {
		return switch (type) {
			case STR -> MainEffects.OOZARU_ROOT.get();
			case RES -> MainEffects.KATCHIN_SPROUT.get();
			case SKP -> MainEffects.METEOR_FLOWER.get();
			case PWR -> MainEffects.AURA_LILY.get();
			case VIT -> MainEffects.HERMIT_FERN.get();
			case ENE -> MainEffects.KAIOSHIN_FRUIT.get();
			default -> null;
		};
	}

	private void cleanse(ServerPlayer player, StatsData data) {
		data.getCooldowns().clearCooldowns();
		// Efectos vanilla dañinos.
		for (MobEffectInstance instance : new ArrayList<>(player.getActiveEffects())) {
			if (instance.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
				player.removeEffect(instance.getEffect());
			}
		}
		player.removeEffect(MainEffects.STUN.get());
		player.removeEffect(MainEffects.STAGGER.get());
		player.removeEffect(MainEffects.FUSION_CD.get());
		player.removeEffect(MainEffects.DASH_CD.get());
		player.removeEffect(MainEffects.DOUBLEDASH_CD.get());
		player.removeEffect(MainEffects.TELEPORT_CD.get());
		player.removeEffect(MainEffects.KI_BLAST_CD.get());
		player.removeEffect(MainEffects.POISE_CD.get());
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull java.util.List<Component> tooltip, @NotNull TooltipFlag flag) {
		String key = switch (statType) {
			case MASTERY -> "item.dragonminez.zenkai_lotus.tooltip";
			case ENE -> "item.dragonminez.kaioshin_fruit.tooltip";
			default -> "item.dragonminez.stat_plant.tooltip";
		};
		if (statType == StatType.MASTERY || statType == StatType.ENE) {
			tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
		} else {
			tooltip.add(Component.translatable(key, statType.name()).withStyle(ChatFormatting.GRAY));
		}
	}
}
