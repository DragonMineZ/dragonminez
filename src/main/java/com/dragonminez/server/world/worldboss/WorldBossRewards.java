package com.dragonminez.server.world.worldboss;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.util.NumberFormattingUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.quest.QuestParser;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.rewards.SkillReward;
import com.dragonminez.common.quest.rewards.TPSReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.worldboss.WorldBossResults;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WorldBossRewards {
	private static final Gson GSON = new GsonBuilder().create();
	public static final String RESULTS_COMMAND = "/dmzworldboss results";

	private record Resolved(GeneralServerConfig.WorldBossRewardEntry entry, QuestReward reward, String json) {}

	private WorldBossRewards() {}

	public static List<QuestReward> previewRewards(String bossKey) {
		List<QuestReward> rewards = new ArrayList<>();
		for (Resolved resolved : resolve(bossKey)) rewards.add(resolved.reward());
		return rewards;
	}

	private static List<Resolved> resolve(String bossKey) {
		List<Resolved> resolved = new ArrayList<>();
		for (GeneralServerConfig.WorldBossRewardEntry entry : ConfigManager.getServerConfig().getWorldBoss().getRewards(bossKey)) {
			if (entry == null) continue;
			JsonObject json;
			QuestReward reward;
			try {
				json = GSON.toJsonTree(entry).getAsJsonObject();
				reward = QuestParser.parseReward(json);
			} catch (RuntimeException e) {
				LogUtil.error(Env.SERVER, "Invalid world boss reward for {} ({}): {}", bossKey, entry.getType(), e.getMessage());
				continue;
			}
			if (reward == null) {
				LogUtil.warn(Env.SERVER, "Skipping unresolvable world boss reward for {}: type={} item={} skill={}",
						bossKey, entry.getType(), entry.getItem(), entry.getSkill());
				continue;
			}
			resolved.add(new Resolved(entry, reward, GSON.toJson(json)));
		}
		return resolved;
	}

	public static WorldBossResults grantAndBuild(ServerLevel level, String bossKey, String bossNameKey, long durationTicks) {
		GeneralServerConfig.WorldBossConfig config = ConfigManager.getServerConfig().getWorldBoss();
		List<Resolved> rewards = resolve(bossKey);
		List<WorldBossContribution.Score> scores = WorldBossContribution.snapshot(bossKey);
		double total = WorldBossContribution.totalPoints(scores);
		double best = scores.isEmpty() ? 0.0 : scores.get(0).points();

		List<WorldBossResults.Reward> rewardDefs = new ArrayList<>();
		for (Resolved resolved : rewards) rewardDefs.add(new WorldBossResults.Reward(resolved.json(), (float) resolved.entry().getChance()));

		List<WorldBossResults.PlayerEntry> entries = new ArrayList<>();
		int rank = 0;
		for (WorldBossContribution.Score score : scores) {
			double points = score.points();
			if (points <= 0.0) continue;
			if (entries.size() >= WorldBossResults.MAX_PLAYERS) break;

			double share = total > 0.0 ? points / total : 0.0;
			boolean eligible = share >= config.getMinContributionShare();
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(score.id());
			double guaranteedRatio = config.getGuaranteedRewardMinRatio()
					+ (1.0 - config.getGuaranteedRewardMinRatio()) * (best > 0.0 ? points / best : 0.0);

			float[] chances = new float[rewards.size()];
			float[] amounts = new float[rewards.size()];
			List<Component> granted = new ArrayList<>();
			for (int j = 0; j < rewards.size(); j++) {
				Resolved resolved = rewards.get(j);
				double effectiveChance = resolved.entry().isGuaranteed()
						? 1.0
						: Math.min(1.0, resolved.entry().getChance() * (1.0 + config.getRankChanceBonus(rank)));
				chances[j] = (float) effectiveChance;
				if (!eligible || player == null) continue;
				boolean won = resolved.entry().isGuaranteed() || level.getRandom().nextDouble() < effectiveChance;
				if (!won) continue;
				double ratio = resolved.entry().isGuaranteed() ? guaranteedRatio : 1.0;
				amounts[j] = grant(resolved, player, ratio);
				granted.add(describe(resolved, amounts[j]));
			}

			entries.add(new WorldBossResults.PlayerEntry(score.id(), score.name(), rank, (float) points, (float) share, score.auraRgb(),
					(float) score.damage(WorldBossContribution.DamageKind.MELEE), (float) score.damage(WorldBossContribution.DamageKind.STRIKE),
					(float) score.damage(WorldBossContribution.DamageKind.KI), (float) score.damage(WorldBossContribution.DamageKind.OTHER),
					(float) score.mitigated(WorldBossContribution.MitigationKind.DEFENSE), (float) score.mitigated(WorldBossContribution.MitigationKind.BLOCK),
					(float) score.mitigated(WorldBossContribution.MitigationKind.SHIELD), receivedList(score.received()),
					(float) score.healed(WorldBossContribution.HealKind.SELF), (float) score.healed(WorldBossContribution.HealKind.ALLY),
					chances, amounts));

			if (player != null) {
				if (!granted.isEmpty()) NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
				player.sendSystemMessage(buildChat(granted, eligible, points, share));
			}
			rank++;
		}

		return new WorldBossResults(bossKey, bossNameKey, durationTicks, true, rewardDefs, entries);
	}

	private static List<WorldBossResults.NamedAmount> receivedList(Map<String, Double> received) {
		List<WorldBossResults.NamedAmount> list = new ArrayList<>();
		received.entrySet().stream()
				.sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
				.limit(WorldBossResults.MAX_RECEIVED_SOURCES)
				.forEach(entry -> list.add(new WorldBossResults.NamedAmount(entry.getKey(), entry.getValue().floatValue())));
		return list;
	}

	private static float grant(Resolved resolved, ServerPlayer player, double ratio) {
		QuestReward reward = resolved.reward();
		reward.giveReward(player, ratio);
		if (reward instanceof SkillReward skill) {
			TechniqueData technique = PredefinedTechniques.copyOf(skill.getSkill());
			if (technique != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					if (!data.getTechniques().getUnlockedTechniques().containsKey(technique.getId())) data.getTechniques().unlockTechnique(technique);
				});
			}
			return skill.getLevel();
		}
		if (reward instanceof TPSReward tps) return tps.scaledAmount(ratio);
		if (reward instanceof ItemReward item) return item.scaledCount(ratio);
		return 1.0f;
	}

	public static MutableComponent describe(QuestReward reward, float amount) {
		if (reward instanceof ItemReward item) {
			return Component.translatable("worldboss.dragonminez.results.item", Math.max(1, Math.round(amount)),
					Component.translatable("item." + ResourceLocation.parse(item.getItemId()).toLanguageKey()));
		}
		if (reward instanceof TPSReward) {
			return Component.translatable("worldboss.dragonminez.results.tps", NumberFormattingUtil.formatLargeNumber(amount));
		}
		if (reward instanceof SkillReward skill) {
			return Component.translatable("skill.dragonminez." + skill.getSkill());
		}
		return reward.getDescription().copy();
	}

	private static MutableComponent describe(Resolved resolved, float amount) {
		return describe(resolved.reward(), amount);
	}

	private static Component buildChat(List<Component> granted, boolean eligible, double points, double share) {
		String compactPoints = NumberFormattingUtil.formatLargeNumber(points);
		String percent = NumberFormattingUtil.formatUpToOneDecimal(share * 100.0);
		MutableComponent moreInfo = Component.translatable("worldboss.dragonminez.results.more_info")
				.withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, RESULTS_COMMAND))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("worldboss.dragonminez.results.more_info_hover"))));

		MutableComponent message;
		if (!eligible) {
			message = Component.translatable("worldboss.dragonminez.results.not_eligible", compactPoints, percent);
		} else if (granted.isEmpty()) {
			message = Component.translatable("worldboss.dragonminez.results.nothing", compactPoints, percent);
		} else {
			MutableComponent list = Component.empty();
			for (int i = 0; i < granted.size(); i++) {
				if (i > 0) list.append(Component.literal(", "));
				list.append(granted.get(i).copy().withStyle(ChatFormatting.YELLOW));
			}
			message = Component.translatable("worldboss.dragonminez.results.received", list, compactPoints, percent);
		}
		return message.append(Component.literal(" ")).append(moreInfo);
	}
}
