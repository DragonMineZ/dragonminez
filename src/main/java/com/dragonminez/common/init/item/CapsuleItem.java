package com.dragonminez.common.init.item;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig.CapsuleValues;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CapsuleItem extends Item {
	private final CapsuleType capsuleType;

	public CapsuleItem(CapsuleType capsuleType) {
		super(new Properties());
		this.capsuleType = capsuleType;
	}

	@Override
	public @NotNull Component getName(@NotNull ItemStack pStack) {
		CapsuleValues values = ConfigManager.getServerConfig().getGameplay().getCapsules().getCapsuleValues(capsuleType);
		return Component.translatable("item.dragonminez.capsule", values.getStats()).withStyle(ChatFormatting.GREEN);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
		CapsuleValues values = ConfigManager.getServerConfig().getGameplay().getCapsules().getCapsuleValues(capsuleType);
		pTooltipComponents.add(Component.translatable("item.dragonminez.capsule.tooltip", values.getPoints(), values.getStats()).withStyle(ChatFormatting.GREEN));
		pTooltipComponents.add(Component.translatable("item.dragonminez.capsule.tooltip2", values.getStats()).withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, @NotNull InteractionHand pUsedHand) {
		ItemStack capsule = pPlayer.getItemInHand(pUsedHand);
		pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.5F, 1.0F);
		if (!pLevel.isClientSide) {
			StatsProvider.get(StatsCapability.INSTANCE, pPlayer).ifPresent(data -> {
				if (data.getStatus().isHasCreatedCharacter()) {
					String separator = ConfigManager.getServerConfig().getGameplay().getCapsules().getStatSeparator();
					CapsuleValues values = ConfigManager.getServerConfig().getGameplay().getCapsules().getCapsuleValues(capsuleType);

					var capsuleComponent = Component.empty();
					String statSeparator = ConfigManager.getServerConfig().getGameplay().getCapsules().getStatSeparator();
                    String[] split = values.getStats().split(separator);
                    for (int i = 0; i < split.length; i++) {
                        String statName = split[i];
                        capsuleComponent.append(applyCapsuleStats(capsule, data, statName));
						if (i < split.length - 1) {
                            capsuleComponent.append(Component.literal(statSeparator).withStyle(ChatFormatting.GRAY));
						}
                    }
					pPlayer.displayClientMessage(capsuleComponent, true);
				} else {
					pPlayer.displayClientMessage(Component.translatable("error.dmz.createcharacter").withStyle(ChatFormatting.RED), true);
				}
			});
			return InteractionResultHolder.sidedSuccess(capsule, pLevel.isClientSide());
		} else {
			return InteractionResultHolder.fail(capsule);
		}
	}

	private Component applyCapsuleStats(ItemStack capsule, StatsData data, String statName) {
		int maxStat = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
		int currentStat = getCurrentStat(data, statName);

		if (currentStat < maxStat) {
			CapsuleValues values = ConfigManager.getServerConfig().getGameplay().getCapsules().getCapsuleValues(capsuleType);
			int increment = Math.min(values.getPoints(), maxStat - currentStat);
			addToStat(data, statName, increment);
			capsule.shrink(1);

			return Component.translatable("item.dragonminez.capsule.use", increment, statName)
							.withStyle(ChatFormatting.GREEN);
		} else {
			return Component.translatable("item.dragonminez.capsule.full", statName)
							.withStyle(ChatFormatting.RED);
		}
	}

	private int getCurrentStat(StatsData data, String statName) {
		return switch (statName) {
			case "STR" -> data.getStats().getStrength();
			case "SKP" -> data.getStats().getStrikePower();
			case "RES" -> data.getStats().getResistance();
			case "VIT" -> data.getStats().getVitality();
			case "PWR" -> data.getStats().getKiPower();
			case "ENE" -> data.getStats().getEnergy();
			default -> 0;
		};
	}

	private void addToStat(StatsData data, String statName, int amount) {
		switch (statName) {
			case "STR" -> data.getStats().setStrength(data.getStats().getStrength() + amount);
			case "SKP" -> data.getStats().setStrikePower(data.getStats().getStrikePower() + amount);
			case "RES" -> data.getStats().setResistance(data.getStats().getResistance() + amount);
			case "VIT" -> data.getStats().setVitality(data.getStats().getVitality() + amount);
			case "PWR" -> data.getStats().setKiPower(data.getStats().getKiPower() + amount);
			case "ENE" -> data.getStats().setEnergy(data.getStats().getEnergy() + amount);
		}
	}
}


