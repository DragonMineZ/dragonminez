package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.FormMasteries;
import com.dragonminez.common.util.TransformationsHelper;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Getter
public class TransformationReward extends QuestReward {
	private final String formGroup;
	private final String formName;
	private final double mastery;
	private final boolean stack;

	public TransformationReward(String formGroup, String formName, double mastery, boolean stack) {
		super(RewardType.TRANSFORMATION);
		this.formGroup = formGroup;
		this.formName = formName;
		this.mastery = mastery;
		this.stack = stack;
	}

	@Override
	public void giveReward(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			Character character = data.getCharacter();

			grantFormSkill(data, character.getRaceName());

			FormMasteries masteries = stack ? character.getStackFormMasteries() : character.getFormMasteries();
			double current = masteries.getMastery(formGroup, formName);
			if (mastery > current) {
				masteries.setMastery(formGroup, formName, mastery, Double.MAX_VALUE);
			}

			grantRequisiteChain(character, character.getRaceName(), formGroup, formName);
		});
	}

	private void grantRequisiteChain(Character character, String raceName, String group, String form) {
		FormConfig config = ConfigManager.getFormGroup(raceName, group);
		if (config == null) config = ConfigManager.getStackFormGroup(group);
		if (config == null) return;

		FormConfig.FormData formData = config.getForm(form);
		if (formData == null) return;

		String req = formData.getFormRequisite();
		double need = formData.getUnlockOnMastery();
		if (req.isEmpty() || need <= 0.0) return;

		for (String token : req.split(",")) {
			String entry = token.trim();
			int dot = entry.indexOf('.');
			if (dot <= 0 || dot >= entry.length() - 1) continue;
			String reqGroup = entry.substring(0, dot);
			String reqForm = entry.substring(dot + 1);

			FormMasteries reqMasteries = character.getFormMasteries();
			if (reqMasteries.getMastery(reqGroup, reqForm) < need) {
				reqMasteries.setMastery(reqGroup, reqForm, need, Double.MAX_VALUE);
			}

			grantRequisiteChain(character, raceName, reqGroup, reqForm);
		}
	}

	private void grantFormSkill(com.dragonminez.common.stats.StatsData data, String raceName) {
		FormConfig config = stack
				? ConfigManager.getStackFormGroup(formGroup)
				: ConfigManager.getFormGroup(raceName, formGroup);
		if (config == null) return;

		String formType = config.getFormType();
		String skillName = stack ? formType.toLowerCase() : TransformationsHelper.getSkillNameForType(formType);
		if (skillName == null || skillName.isEmpty()) return;

		FormConfig.FormData formData = config.getForm(formName);
		int targetLevel = formData != null && formData.getUnlockOnSkillLevel() != null
				? formData.getUnlockOnSkillLevel() : 0;

		if (!data.getSkills().hasSkill(skillName) || data.getSkills().getSkillLevel(skillName) < targetLevel) {
			data.getSkills().setSkillLevel(skillName, targetLevel);
		}
	}

	@Override
	public Component getDescription() {
		return Component.translatable(
				"gui.dragonminez.quests.rewards.transformation",
				Component.literal(formName.replace('_', ' '))
		);
	}
}
