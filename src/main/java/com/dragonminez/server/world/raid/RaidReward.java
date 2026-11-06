package com.dragonminez.server.world.raid;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.RaidDefinition;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class RaidReward {

	private RaidReward() {}

	public static List<Component> grant(ServerLevel level, List<ServerPlayer> participants,
										RaidDefinition.Rewards rewards) {
		if (rewards == null || participants.isEmpty()) return List.of();

		List<Component> summary = new ArrayList<>();
		boolean first = true;

		for (ServerPlayer player : participants) {
			grantTrainingPoints(player, rewards, first ? summary : null);
			grantEffects(player, rewards, first ? summary : null);
			grantItems(player, rewards, first ? summary : null);
			first = false;
		}
		return summary;
	}

	private static void line(List<Component> summary, Component text) {
		if (summary != null) summary.add(Component.translatable("raid.dragonminez.rewards.entry", text));
	}

	private static void grantTrainingPoints(ServerPlayer player, RaidDefinition.Rewards rewards, List<Component> summary) {
		float points = rewards.trainingPointsOr(0.0F);
		if (points <= 0.0F) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(
				data -> data.getResources().addTrainingPoints(points, false));
		line(summary, Component.translatable("raid.dragonminez.rewards.tp", (int) points));
	}

	private static void grantEffects(ServerPlayer player, RaidDefinition.Rewards rewards, List<Component> summary) {
		if (rewards.getEffects() == null) return;

		for (RaidDefinition.EffectReward reward : rewards.getEffects()) {
			MobEffect effect = resolve(BuiltInRegistries.MOB_EFFECT, reward.getId(), "effect");
			if (effect == null) continue;

			int ticks = reward.durationTicksOr(20 * 600);
			player.addEffect(new MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), ticks, reward.amplifierOr(0), false, true, true));
			line(summary, Component.translatable("raid.dragonminez.rewards.effect",
					Component.translatable(effect.getDescriptionId()), ticks / 20 / 60));
		}
	}

	private static void grantItems(ServerPlayer player, RaidDefinition.Rewards rewards, List<Component> summary) {
		if (rewards.getItems() == null) return;

		for (RaidDefinition.ItemReward reward : rewards.getItems()) {
			Item item = resolve(BuiltInRegistries.ITEM, reward.getId(), "item");
			if (item == null) continue;

			ItemStack stack = new ItemStack(item, reward.countOr(1));
			line(summary, Component.translatable("raid.dragonminez.rewards.item",
					stack.getCount(), stack.getHoverName()));
			if (!player.getInventory().add(stack)) player.drop(stack, false);
		}
	}

	private static <T> T resolve(Registry<T> registry, String id, String what) {
		if (id == null || id.isBlank()) return null;

		ResourceLocation location = ResourceLocation.tryParse(id);
		if (location == null) {
			LogUtil.warn(Env.SERVER, "Raid reward has a malformed {} id '{}'; skipping it", what, id);
			return null;
		}

		T value = registry.get(location);
		if (value == null) {
			LogUtil.warn(Env.SERVER, "Raid reward points at unknown {} '{}'; skipping it", what, id);
		}
		return value;
	}
}
