package com.dragonminez.common.init.item.consumables;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.statuseffect.RelicItemsStatusHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class SacredWaterItem extends Item {
	private static final int DRINK_TICKS = 32;

	public SacredWaterItem() {
		super(new Properties().stacksTo(16).rarity(Rarity.EPIC));
	}

	@Override
	public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public int getUseDuration(@NotNull ItemStack stack) {
		return DRINK_TICKS;
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
		if (level.isClientSide || !(entity instanceof ServerPlayer player)) return stack;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) {
				player.displayClientMessage(Component.translatable("error.dmz.createcharacter").withStyle(ChatFormatting.RED), true);
				return;
			}
			if (RelicItemsStatusHandler.hasStatMultiplier(data, RelicItemsStatusHandler.SACRED_WATER_BONUS)) {
				player.displayClientMessage(Component.translatable("item.dragonminez.sacred_water.already").withStyle(ChatFormatting.RED), true);
				return;
			}

			double multiplier = ConfigManager.getServerConfig().getGameplay().getSacredWaterMultiplier();
			RelicItemsStatusHandler.setStatMultiplier(data, RelicItemsStatusHandler.SACRED_WATER_BONUS, multiplier);
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);

			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.4F);
			player.displayClientMessage(Component.translatable("item.dragonminez.sacred_water.use", formatMultiplier(multiplier))
					.withStyle(ChatFormatting.AQUA), true);
			if (!player.getAbilities().instabuild) stack.shrink(1);
		});
		return stack;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		double multiplier = ConfigManager.getServerConfig().getGameplay().getSacredWaterMultiplier();
		tooltip.add(Component.translatable("item.dragonminez.sacred_water.tooltip", formatMultiplier(multiplier)).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.dragonminez.sacred_water.tooltip2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}

	private static String formatMultiplier(double multiplier) {
		return String.format(Locale.US, "%.2f", multiplier);
	}
}
