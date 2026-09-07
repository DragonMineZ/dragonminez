package com.dragonminez.common.racial.capture;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.racial.impl.MajinAbsorption;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.server.events.QuestEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RacialCapture {

	private RacialCapture() {
	}

	public static LivingEntity findTarget(ServerPlayer player, double range) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = start.add(look.scale(range));
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);

		List<Entity> entities = player.level().getEntities(player, searchBox,
				e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable());

		for (Entity entity : entities) {
			AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
			if (entityBox.contains(start) || entityBox.clip(start, end).isPresent()) {
				return (LivingEntity) entity;
			}
		}
		return null;
	}

	public static boolean isStrongerThan(ServerPlayer player, StatsData playerData, LivingEntity target) {
		double maxDmg = Math.max(playerData.getMaxMeleeDamage(), Math.max(playerData.getMaxStrikeDamage(), playerData.getMaxKiDamage()));
		if (target.getHealth() > maxDmg) return false;

		if (target instanceof ServerPlayer targetPlayer) {
			return StatsProvider.get(StatsCapability.INSTANCE, targetPlayer)
					.map(targetData -> targetData.getLevel() < playerData.getLevel())
					.orElse(false);
		}

		return true;
	}

	public static int cappedAbsorptionBonus(double sourceValue, double ratio, int cap) {
		if (!Double.isFinite(sourceValue) || sourceValue <= 0) return 1;
		double bonus = Math.min(sourceValue, cap) * ratio;
		if (!Double.isFinite(bonus) || bonus < 1) return 1;
		return (int) Math.min(bonus, cap);
	}

	public static void finalizeKill(ServerPlayer user, LivingEntity target, double healRatio) {
		if (healRatio > 0) {
			float heal = (float) (user.getMaxHealth() * healRatio);
			user.heal(heal);
		}
		QuestEvents.creditQuestKill(user, target);
		target.kill();
	}

	public static boolean forcesKnockdown(Entity attacker) {
		if (!(attacker instanceof ServerPlayer player)) return false;

		return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
			GeneralServerConfig.RacialSkillsConfig racial = ConfigManager.getServerConfig().getRacialSkills();
			if (!racial.getEnableRacialSkills()) return false;
			if (data.getStatus().getSelectedAction() != ActionMode.RACIAL) return false;

			RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(data.getCharacter().getRace());
			String skill = raceConfig == null ? null : raceConfig.getRacialSkill();
			if (skill == null) return false;

			return switch (skill) {
				case "namekian" -> racial.getNamekian().getEnabled()
						&& !data.getCooldowns().hasCooldown(Cooldowns.ASSIMILATION)
						&& data.getRacialData().getAssimilations().size() < racial.getNamekian().getAssimilationAmount();
				case "majin" -> racial.getMajin().getEnabled()
						&& !data.getCooldowns().hasCooldown(Cooldowns.ABSORPTION)
						&& MajinAbsorption.effectiveSlotsUsed(data) < racial.getMajin().getAbsorptionAmount();
				default -> false;
			};
		}).orElse(false);
	}
}
