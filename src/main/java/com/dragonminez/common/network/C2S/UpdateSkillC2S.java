package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateSkillC2S {

	public enum SkillAction {
		TOGGLE, UPGRADE, PURCHASE
	}

	private final String skillName;
	private final SkillAction action;
	private final int cost;

	public UpdateSkillC2S(SkillAction action, String skillName, int cost) {
		this.skillName = skillName;
		this.action = action;
		this.cost = cost;
	}

	public UpdateSkillC2S(FriendlyByteBuf buf) {
		this.skillName = buf.readUtf();
		this.action = buf.readEnum(SkillAction.class);
		this.cost = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(this.skillName);
		buf.writeEnum(this.action);
		buf.writeInt(this.cost);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					Skill skill = data.getSkills().getSkill(skillName);
					boolean raceAllowed = isSkillAllowedForPlayerRace(data, skillName);
					switch (action) {
						case TOGGLE:
							if (skill != null && skill.getLevel() > 0) skill.setActive(!skill.isActive());
							break;
						case UPGRADE:
							if (skill == null) break;
							if (skill.getLevel() <= 0 && !raceAllowed) break;
							refreshRuntimeMaxLevel(data, skillName, skill);
							if (!skill.isMaxLevel() && data.getResources().getTrainingPoints() >= cost && cost != -1 && !(skillName.equals("potentialunlock") && skill.getLevel() == 10)) {
								data.getResources().removeTrainingPoints(cost);
								boolean wasLevelZero = skill.getLevel() == 0;
								skill.addLevel(1);

								if (wasLevelZero) unlockTechniqueIfPresent(data, skillName);
							}
							break;

						case PURCHASE:
							if (!raceAllowed) break;
							if (!data.getSkills().hasSkill(skillName) && data.getResources().getTrainingPoints() >= cost && cost != -1) {
								data.getResources().removeTrainingPoints(cost);
								data.getSkills().setSkillLevel(skillName, 1);
								unlockTechniqueIfPresent(data, skillName);
							}
							break;
					}
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				});
			}
		});
		ctx.get().setPacketHandled(true);
	}

	private static boolean isSkillAllowedForPlayerRace(StatsData data, String skillName) {
		if (data == null || skillName == null || skillName.isEmpty()) return false;

		String raceName = data.getCharacter() != null ? data.getCharacter().getRaceName() : "";
		return ConfigManager.getSkillsConfig().isSkillAllowedForRace(skillName, raceName);
	}

	private void unlockTechniqueIfPresent(com.dragonminez.common.stats.StatsData data, String techId) {
		if (PredefinedTechniques.REGISTRY.containsKey(techId)) {
			KiAttackData template = PredefinedTechniques.REGISTRY.get(techId);
			KiAttackData clone = new KiAttackData();
			clone.load(template.save());
			data.getTechniques().unlockTechnique(clone);
		} else if (PredefinedTechniques.STRIKE_REGISTRY.containsKey(techId)) {
			StrikeAttackData template = PredefinedTechniques.STRIKE_REGISTRY.get(techId);
			StrikeAttackData clone = new StrikeAttackData();
			clone.load(template.save());
			data.getTechniques().unlockTechnique(clone);
		}
	}

	private static void refreshRuntimeMaxLevel(StatsData data, String skillName, Skill skill) {
		String normalizedSkill = skillName.toLowerCase();
		var skillsConfig = ConfigManager.getSkillsConfig();

		if (skillsConfig.getFormSkills().contains(normalizedSkill)) {
			String raceName = data.getCharacter().getRaceName();
			if (raceName == null || raceName.isEmpty()) return;

			var charConfig = ConfigManager.getRaceCharacter(raceName);
			int maxLevel = charConfig.getFormSkillTpCosts(normalizedSkill).length;
			skill.setMaxLevel(maxLevel);
			return;
		}

		int maxLevel = 0;
		var skillCosts = skillsConfig.getSkillCosts(normalizedSkill);
		if (skillCosts != null && skillCosts.getCosts() != null) maxLevel = skillCosts.getCosts().size();

		if ("potentialunlock".equalsIgnoreCase(normalizedSkill)) maxLevel = Math.min(maxLevel, 30);
		else maxLevel = Math.min(maxLevel, 50);

		skill.setMaxLevel(maxLevel);
	}
}