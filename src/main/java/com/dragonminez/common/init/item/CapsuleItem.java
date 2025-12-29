package com.dragonminez.common.init.item;

import com.dragonminez.common.config.ConfigManager;
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
		return Component.translatable("item.dragonminez." + capsuleType.getTranslationKey());
	}

	@Override
	public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
		pTooltipComponents.add(Component.translatable("item.dragonminez." + capsuleType.getTranslationKey() + ".tooltip").withStyle(ChatFormatting.GRAY));
		pTooltipComponents.add(Component.translatable("item.dragonminez." + capsuleType.getTranslationKey() + ".tooltip2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, @NotNull InteractionHand pUsedHand) {
		ItemStack capsula = pPlayer.getItemInHand(pUsedHand);
		pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.5F, 1.0F);

		if (!pLevel.isClientSide) {
			StatsProvider.get(StatsCapability.INSTANCE, pPlayer).ifPresent(data -> {
				if (data.getStatus().hasCreatedCharacter()) {
					String statName = capsuleType.getStatName();
					int maxStat = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
					int currentStat = getCurrentStat(data, statName);

					if (currentStat < maxStat) {
						int increment = Math.min(5, maxStat - currentStat);
						addToStat(data, statName, increment);

						pPlayer.displayClientMessage(
								Component.literal("+")
										.append(Component.literal(increment + " "))
										.append(Component.translatable("item.dragonminez." + capsuleType.getTranslationKey() + "." + statName.toLowerCase() + ".use"))
										.withStyle(ChatFormatting.GREEN),
								true
						);
						capsula.shrink(1);
					} else {
						pPlayer.displayClientMessage(
								Component.translatable("item.dragonminez." + capsuleType.getTranslationKey() + "." + statName.toLowerCase() + ".full")
										.withStyle(ChatFormatting.RED),
								true
						);
					}
				} else {
					pPlayer.displayClientMessage(Component.translatable("error.dmz.createcharacter").withStyle(ChatFormatting.RED), true);
				}
			});
			return InteractionResultHolder.sidedSuccess(capsula, pLevel.isClientSide());
		} else {
			return InteractionResultHolder.fail(capsula);
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


