package com.dragonminez.common.init.item.consumables;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig.CapsuleValues;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CapsuleItem extends Item implements CapsuleUseTracker.SummaryItem {
	private final CapsuleType type;
	private ChatFormatting tierColor = ChatFormatting.WHITE;
	private String tierLabel = null;
	private Integer tierMultiplier = 1;

	public CapsuleItem(CapsuleType type) {
		super(new Properties());
		this.type = type;
	}

	public CapsuleItem(CapsuleType type, ChatFormatting tierColor, String tierLabel, Integer tierMultiplier, Rarity rarity) {
		super(new Properties().rarity(rarity));
		this.type = type;
		this.tierColor = tierColor;
		this.tierLabel = tierLabel;
		this.tierMultiplier = tierMultiplier;
	}

	@Override
	public @NotNull Component getName(@NotNull ItemStack pStack) {
		CapsuleValues values = ConfigManager.getServerConfig().getGameplay().getCapsules().getCapsuleValues(type);
		return Component.translatable("item.dragonminez.capsule")
				.append(Component.literal(" "))
				.append(tierLabel != null
						? Component.translatable("item.dragonminez.capsule.tier", tierLabel).withStyle(tierColor).append(Component.literal(" "))
						: Component.empty())
				.append(Component.translatable("item.dragonminez.capsule.stat", values.getStats()).withStyle(ChatFormatting.GREEN))
				;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
		CapsuleValues values = ConfigManager.getServerConfig().getGameplay().getCapsules().getCapsuleValues(type);
		pTooltipComponents.add(Component.translatable("item.dragonminez.capsule.tooltip", values.getPoints() * tierMultiplier, values.getStats()).withStyle(ChatFormatting.GREEN));
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
					CapsuleValues values = ConfigManager.getServerConfig().getGameplay().getCapsules().getCapsuleValues(type);

					var capsuleComponent = Component.empty();
					String statSeparator = ConfigManager.getServerConfig().getGameplay().getCapsules().getStatSeparator();
					String[] split = values.getStats().split(separator);
					boolean[] anyApplied = {false};
					Map<String, Integer> streak = CapsuleUseTracker.getStreak(pPlayer, this);
					for (int i = 0; i < split.length; i++) {
						String statName = split[i];
						capsuleComponent.append(applyCapsuleStats(data, statName, anyApplied, streak));
						if (i < split.length - 1) {
							capsuleComponent.append(Component.literal(statSeparator).withStyle(ChatFormatting.GRAY));
						}
					}
					if (anyApplied[0]) capsule.shrink(1);
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

	@Override
	public Component getStreakSummary(ServerPlayer player, Map<String, Integer> totals) {
		return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return null;
			String separator = ConfigManager.getServerConfig().getGameplay().getCapsules().getStatSeparator();
			CapsuleValues values = ConfigManager.getServerConfig().getGameplay().getCapsules().getCapsuleValues(type);

			var summary = Component.empty();
			String[] split = values.getStats().split(separator);
			for (int i = 0; i < split.length; i++) {
				String statName = split[i];
				summary.append(Component.translatable("item.dragonminez.capsule.current", statName, data.getCurrentStatValue(statName))
						.withStyle(ChatFormatting.RED));
				if (i < split.length - 1) {
					summary.append(Component.literal(separator).withStyle(ChatFormatting.GRAY));
				}
			}
			return (Component) summary;
		}).orElse(null);
	}

	private Component applyCapsuleStats(StatsData data, String statName, boolean[] anyApplied, Map<String, Integer> streak) {
		CapsuleValues values = ConfigManager.getServerConfig().getGameplay().getCapsules().getCapsuleValues(type);
		int requested = values.getPoints() * tierMultiplier;
		int increment = data.getMaxAllowedIncreaseForStat(statName, requested);

		if (increment > 0) {
			addToStat(data, statName, increment);
			anyApplied[0] = true;
			int total = CapsuleUseTracker.add(streak, statName, increment);

			return Component.translatable("item.dragonminez.capsule.use", total, statName)
					.withStyle(ChatFormatting.GREEN);
		}

		Component full = Component.translatable("item.dragonminez.capsule.full", statName).withStyle(ChatFormatting.RED);
		int total = streak.getOrDefault(statName, 0);
		if (total <= 0) return full;
		// Keep showing what this hold already gave, plus why it stopped growing
		return Component.translatable("item.dragonminez.capsule.use", total, statName).withStyle(ChatFormatting.GREEN)
				.append(Component.literal(" (").withStyle(ChatFormatting.RED))
				.append(full)
				.append(Component.literal(")").withStyle(ChatFormatting.RED));
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


