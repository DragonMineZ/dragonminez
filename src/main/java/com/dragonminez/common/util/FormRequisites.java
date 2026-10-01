package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class FormRequisites {

	public record Lock(int level, String sagaId, int questId) {
		public static final Lock NONE = new Lock(0, null, -1);

		public boolean levelLocked() {
			return level > 0;
		}

		public boolean sagaLocked() {
			return sagaId != null;
		}

		public boolean isLocked() {
			return levelLocked() || sagaLocked();
		}
	}

	private FormRequisites() {}

	public static Lock check(StatsData data, FormConfig.FormData form, boolean clientSide) {
		if (data == null || form == null) return Lock.NONE;
		var gameplay = ConfigManager.getServerConfig().getGameplay();

		int level = 0;
		int requiredLevel = form.getLevelRequisite();
		if (gameplay.getLevelFormRequisiteEnabled() && requiredLevel > 0 && data.getLevel() < requiredLevel) level = requiredLevel;

		String sagaId = null;
		int questId = -1;
		if (gameplay.getSagaFormRequisiteEnabled() && !Boolean.FALSE.equals(gameplay.getStoryModeEnabled())) {
			String requiredSaga = form.getSagaRequisiteId();
			int requiredQuest = form.getSagaRequisiteQuest();
			if (!requiredSaga.isEmpty() && requiredQuest >= 0 && sagaQuestExists(requiredSaga, requiredQuest, clientSide)
					&& !data.getPlayerQuestData().isQuestCompleted(PlayerQuestData.sagaQuestKey(requiredSaga, requiredQuest))) {
				sagaId = requiredSaga;
				questId = requiredQuest;
			}
		}

		return level == 0 && sagaId == null ? Lock.NONE : new Lock(level, sagaId, questId);
	}

	public static Lock checkSkillLevel(StatsData data, String skillName, int targetLevel, boolean clientSide) {
		if (data == null || skillName == null || skillName.isEmpty()) return Lock.NONE;
		int level = 0;
		String sagaId = null;
		int questId = -1;
		for (FormConfig.FormData form : formsAtSkillLevel(data, skillName, targetLevel)) {
			Lock lock = check(data, form, clientSide);
			level = Math.max(level, lock.level());
			if (sagaId == null && lock.sagaLocked()) {
				sagaId = lock.sagaId();
				questId = lock.questId();
			}
		}
		return level == 0 && sagaId == null ? Lock.NONE : new Lock(level, sagaId, questId);
	}

	public static List<Component> describe(Lock lock, boolean clientSide) {
		List<Component> lines = new ArrayList<>();
		if (lock == null) return lines;
		if (lock.sagaLocked()) lines.add(Component.translatable("gui.dragonminez.skills.locked_until", sagaQuestReference(lock.sagaId(), lock.questId(), clientSide)));
		if (lock.levelLocked()) lines.add(Component.translatable("gui.dragonminez.skills.locked_until", Component.translatable("gui.dragonminez.skills.locked_level", lock.level())));
		return lines;
	}

	public static Component describeInline(Lock lock, boolean clientSide) {
		MutableComponent requirements = Component.empty();
		if (lock.sagaLocked()) requirements.append(sagaQuestReference(lock.sagaId(), lock.questId(), clientSide));
		if (lock.levelLocked()) {
			if (lock.sagaLocked()) requirements.append(" / ");
			requirements.append(Component.translatable("gui.dragonminez.skills.locked_level", lock.level()));
		}
		return Component.translatable("gui.dragonminez.skills.locked_until", requirements);
	}

	private static Component sagaQuestReference(String sagaId, int questId, boolean clientSide) {
		Saga saga = findSaga(sagaId, clientSide);
		Component sagaName = saga != null && saga.getName() != null ? Component.translatable(saga.getName()) : Component.literal(sagaId);
		return Component.translatable("gui.dragonminez.skills.quest_ref", sagaName, questId);
	}

	private static List<FormConfig.FormData> formsAtSkillLevel(StatsData data, String skillName, int targetLevel) {
		List<FormConfig.FormData> result = new ArrayList<>();
		String raceName = data.getCharacter() != null ? data.getCharacter().getRaceName() : null;
		if (raceName != null && !raceName.isEmpty()) collect(ConfigManager.getAllFormsForRace(raceName).values(), skillName, targetLevel, result);
		collect(ConfigManager.getAllStackForms().values(), skillName, targetLevel, result);
		return result;
	}

	private static void collect(Collection<FormConfig> groups, String skillName, int targetLevel, List<FormConfig.FormData> out) {
		String skill = skillName.toLowerCase(Locale.ROOT);
		for (FormConfig group : groups) {
			if (group == null || !group.getFormType().toLowerCase(Locale.ROOT).equals(skill)) continue;
			for (FormConfig.FormData form : group.getForms().values()) {
				if (form != null && form.getUnlockOnSkillLevel() != null && form.getUnlockOnSkillLevel() == targetLevel) out.add(form);
			}
		}
	}

	private static boolean sagaQuestExists(String sagaId, int questId, boolean clientSide) {
		Saga saga = findSaga(sagaId, clientSide);
		return saga != null && saga.getQuestById(questId) != null;
	}

	private static Saga findSaga(String sagaId, boolean clientSide) {
		return clientSide ? QuestRegistry.getClientSaga(sagaId) : QuestRegistry.getSaga(sagaId);
	}
}
