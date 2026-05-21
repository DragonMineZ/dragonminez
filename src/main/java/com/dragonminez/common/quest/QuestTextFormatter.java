package com.dragonminez.common.quest;

import com.dragonminez.common.quest.objectives.BiomeObjective;
import com.dragonminez.common.quest.objectives.CoordsObjective;
import com.dragonminez.common.quest.objectives.DimensionObjective;
import com.dragonminez.common.quest.objectives.DragonSummonObjective;
import com.dragonminez.common.quest.objectives.InteractObjective;
import com.dragonminez.common.quest.objectives.ItemObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.quest.objectives.SkillObjective;
import com.dragonminez.common.quest.objectives.StructureObjective;
import com.dragonminez.common.quest.objectives.TalkToObjective;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuestTextFormatter {

	private static final String OBJECTIVE_DEFEAT = "dmz.quest.defeat.obj";
	private static final String OBJECTIVE_OBTAIN = "dmz.quest.obtain.obj";
	private static final String OBJECTIVE_TALK_TO = "dmz.quest.talk_to.obj";
	private static final String OBJECTIVE_GO_TO = "dmz.quest.go_to.obj";
	private static final String OBJECTIVE_INTERACT = "dmz.quest.interact.obj";
	private static final String OBJECTIVE_SUMMON_DRAGON = "dmz.quest.summon_dragon.obj";
	private static final String OBJECTIVE_SKILL = "dmz.quest.skill.obj";

	private static final Pattern NPC_VARIANT_SUFFIX = Pattern.compile("^(.+?)_\\d+$");

	private static final Map<String, String> STRUCTURE_NAMES = Map.ofEntries(
			Map.entry("dragonminez:goku_house", "Goku's House"),
			Map.entry("dragonminez:roshi_house", "Kame House"),
			Map.entry("dragonminez:elder_guru", "Guru's House"),
			Map.entry("dragonminez:timechamber", "Hyperbolic Time Chamber"),
			Map.entry("dragonminez:kamilookout", "Kami's Lookout"),
			Map.entry("dragonminez:gero_lab", "Dr. Gero's Laboratory"),
			Map.entry("dragonminez:village_ajissa", "Ajissa Village"),
			Map.entry("dragonminez:village_sacred", "Sacred Village")
	);

	private static final Map<String, String> DIMENSION_NAMES = Map.ofEntries(
			Map.entry("minecraft:overworld", "Earth"),
			Map.entry("minecraft:the_nether", "Nether"),
			Map.entry("minecraft:the_end", "The End"),
			Map.entry("dragonminez:namek", "Namek"),
			Map.entry("dragonminez:time_chamber", "Hyperbolic Time Chamber")
	);

	private QuestTextFormatter() {
	}

	public static Component describeObjective(QuestObjective objective) {
		if (objective == null) {
			return Component.empty();
		}

		if (objective instanceof KillObjective kill) {
			return Component.translatable(OBJECTIVE_DEFEAT, resolveEntityName(kill.getEntityId()));
		}
		if (objective instanceof ItemObjective item) {
			return Component.translatable(OBJECTIVE_OBTAIN, resolveItemName(item.getItemId()));
		}
		if (objective instanceof TalkToObjective talkTo) {
			return Component.translatable(OBJECTIVE_TALK_TO, resolveNpcName(talkTo.getNpcId()));
		}
		if (objective instanceof InteractObjective interact) {
			return Component.translatable(OBJECTIVE_INTERACT, resolveInteractTarget(interact));
		}
		if (objective instanceof DragonSummonObjective dragonSummon) {
			return Component.translatable(OBJECTIVE_SUMMON_DRAGON, resolveDragonName(dragonSummon.getDragonId()));
		}
		if (objective instanceof SkillObjective skill) {
			return Component.translatable(OBJECTIVE_SKILL, resolveSkillName(skill.getSkill()), skill.getLevel());
		}
		if (objective instanceof StructureObjective structure) {
			return Component.translatable(OBJECTIVE_GO_TO, resolveStructureName(structure.getStructureId()));
		}
		if (objective instanceof BiomeObjective biome) {
			return Component.translatable(OBJECTIVE_GO_TO, resolveBiomeName(biome.getBiomeId()));
		}
		if (objective instanceof DimensionObjective dimension) {
			return Component.translatable(OBJECTIVE_GO_TO, resolveDimensionName(dimension.getDimensionId()));
		}
		if (objective instanceof CoordsObjective coords) {
			return Component.translatable(OBJECTIVE_GO_TO, Component.literal(
					coords.getTargetPos().getX() + ", " + coords.getTargetPos().getY() + ", " + coords.getTargetPos().getZ()
			));
		}
		return Component.literal(objective.getType() != null ? humanizeIdentifier(objective.getType().name()) : "?");
	}

	public static Component describeRequirement(QuestPrerequisites.Condition condition, RequirementContext context) {
		if (condition == null || condition.getType() == null) {
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}

		return switch (condition.getType()) {
			case SAGA_QUEST -> Component.translatable(
					"gui.dragonminez.quests.requirement.complete_saga",
					resolveSagaQuestName(condition.getSagaId(), condition.getQuestId())
			);
			case QUEST -> Component.translatable(
					"gui.dragonminez.quests.requirement.complete_quest",
					resolveQuestName(condition.getRequiredQuestId())
			);
			case STAT -> Component.translatable(
					"gui.dragonminez.quests.requirement.stat",
					condition.getMinValue(),
					humanizeIdentifier(condition.getStat())
			);
			case LEVEL -> Component.translatable(
					"gui.dragonminez.quests.requirement.level",
					condition.getMinLevel()
			);
			case BIOME -> Component.translatable(
					"gui.dragonminez.quests.requirement.biome",
					resolveBiomeName(condition.getBiomeId())
			);
			case STRUCTURE -> buildStructureRequirement(condition);
			case DIMENSION -> Component.translatable(
					"gui.dragonminez.quests.requirement.dimension",
					resolveDimensionName(condition.getDimensionId())
			);
			case TIME -> buildTimeRequirement(condition, context);
			case ALIGNMENT -> buildAlignmentRequirement(condition);
			case SKILL -> Component.translatable(
					"gui.dragonminez.quests.requirement.skill",
					resolveSkillName(condition.getSkill()),
					condition.getSkillLevel()
			);
		};
	}

	public static Component displayText(String raw) {
		if (raw == null || raw.isBlank()) {
			return Component.literal("?");
		}
		if (!raw.contains(" ") && raw.contains(".")) {
			return Component.translatable(raw);
		}
		return Component.literal(raw);
	}

	public static String humanizeResourceIdentifier(String raw) {
		if (raw == null || raw.isBlank()) return "?";

		String value = raw.startsWith("#") ? raw.substring(1) : raw;
		int colon = value.indexOf(':');
		String token = colon >= 0 ? value.substring(colon + 1) : value;
		if (token.startsWith("is_")) {
			token = token.substring(3);
		}
		return humanizeIdentifier(token);
	}

	public static String humanizeIdentifier(String raw) {
		if (raw == null || raw.isBlank()) return "?";
		String normalized = raw.replace('_', ' ').replace('-', ' ');
		String[] parts = normalized.split("\\s+");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part.isBlank()) continue;
			if (!builder.isEmpty()) builder.append(' ');
			builder.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				builder.append(part.substring(1).toLowerCase());
			}
		}
		return builder.toString();
	}

	public static String formatGameTimeDuration(long ticks) {
		if (ticks <= 0L) return "0s";
		long totalSeconds = Math.max(1L, ticks / 20L);
		return formatSeconds(totalSeconds);
	}

	public static String formatRealTimeDuration(long millis) {
		if (millis <= 0L) return "0s";
		long totalSeconds = Math.max(1L, millis / 1000L);
		return formatSeconds(totalSeconds);
	}

	private static Component resolveInteractTarget(InteractObjective interact) {
		if (interact.getEntityName() != null && !interact.getEntityName().isBlank()) {
			return Component.literal(interact.getEntityName());
		}
		if (interact.getEntityTypeId() != null && !interact.getEntityTypeId().isBlank()) {
			return resolveEntityName(interact.getEntityTypeId());
		}
		return Component.literal("?");
	}

	private static Component resolveDragonName(String dragonId) {
		if (dragonId == null || dragonId.isBlank()) {
			return Component.literal("the dragon");
		}
		String entityId = dragonId.contains(":") ? dragonId : "dragonminez:" + dragonId;
		return resolveEntityName(entityId);
	}

	private static Component resolveEntityName(String entityId) {
		ResourceLocation id = safeParse(entityId);
		if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
			return Component.literal(humanizeResourceIdentifier(entityId));
		}

		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
		return type != null ? type.getDescription() : Component.literal(humanizeResourceIdentifier(entityId));
	}

	private static Component resolveItemName(String itemId) {
		ResourceLocation id = safeParse(itemId);
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			return Component.literal(humanizeResourceIdentifier(itemId));
		}

		Item item = BuiltInRegistries.ITEM.get(id);
		if (item == null || item == Items.AIR) {
			return Component.literal(humanizeResourceIdentifier(itemId));
		}
		return new ItemStack(item).getHoverName();
	}

	private static Component resolveNpcName(String npcId) {
		if (npcId == null || npcId.isBlank()) {
			return Component.literal("?");
		}

		String normalized = npcId.contains(":") ? npcId.substring(npcId.indexOf(':') + 1) : npcId;
		Component exact = resolveNpcTranslation(normalized);
		if (exact != null) {
			return exact;
		}

		Matcher variantMatcher = NPC_VARIANT_SUFFIX.matcher(normalized);
		if (variantMatcher.matches()) {
			Component variant = resolveNpcTranslation(variantMatcher.group(1));
			if (variant != null) {
				return variant;
			}
		}

		return Component.literal(humanizeIdentifier(normalized));
	}

	private static Component resolveNpcTranslation(String npcId) {
		String questNpcKey = "entity.dragonminez.questnpc." + npcId;
		String masterKey = "gui.dragonminez.lines." + npcId + ".name";

		if (!npcId.contains(" ")) {
			return Component.translatable(questNpcKey).getString().equals(questNpcKey)
					? (!Component.translatable(masterKey).getString().equals(masterKey) ? Component.translatable(masterKey) : null)
					: Component.translatable(questNpcKey);
		}
		return null;
	}

	private static Component resolveSkillName(String skill) {
		if (skill == null || skill.isBlank()) {
			return Component.literal("?");
		}
		String normalized = skill.contains(":") ? skill.substring(skill.indexOf(':') + 1) : skill;
		String key = normalized.contains(".") ? normalized : "skill.dragonminez." + normalized.toLowerCase();
		Component translated = Component.translatable(key);
		return translated.getString().equals(key)
				? Component.literal(humanizeIdentifier(normalized))
				: translated;
	}

	private static Component resolveStructureName(String structureId) {
		String display = STRUCTURE_NAMES.get(structureId);
		return display != null
				? Component.literal(display)
				: Component.literal(humanizeResourceIdentifier(structureId));
	}

	private static Component resolveBiomeName(String biomeId) {
		ResourceLocation id = safeParse(biomeId != null && biomeId.startsWith("#") ? biomeId.substring(1) : biomeId);
		if (biomeId != null && biomeId.startsWith("#")) {
			return Component.literal(humanizeResourceIdentifier(biomeId));
		}
		if (id != null && "minecraft".equals(id.getNamespace())) {
			return Component.translatable("biome.minecraft." + id.getPath());
		}
		return Component.literal(humanizeResourceIdentifier(biomeId));
	}

	private static Component resolveDimensionName(String dimensionId) {
		String display = DIMENSION_NAMES.get(dimensionId);
		return display != null
				? Component.literal(display)
				: Component.literal(humanizeResourceIdentifier(dimensionId));
	}

	private static Component buildStructureRequirement(QuestPrerequisites.Condition condition) {
		MutableComponent base = Component.translatable(
				"gui.dragonminez.quests.requirement.structure",
				resolveStructureName(condition.getStructureId())
		);

		QuestPrerequisites.StructureHint hint = condition.getStructureHint();
		if (hint == null || (!hint.hasCoordinates() && hint.dimensionId() == null)) {
			return base;
		}

		if (hint.dimensionId() != null) {
			base.append(Component.literal(" ("))
					.append(Component.translatable("gui.dragonminez.quests.requirement.dimension", resolveDimensionName(hint.dimensionId())))
					.append(Component.literal(")"));
		}

		if (hint.hasCoordinates()) {
			base.append(Component.literal(" - "))
					.append(Component.translatable("gui.dragonminez.quests.requirement.coords", hint.x(), hint.y(), hint.z()));
		}

		return base;
	}

	private static Component buildTimeRequirement(QuestPrerequisites.Condition condition, RequirementContext context) {
		long duration = condition.getDuration() != null ? condition.getDuration() : 0L;
		MutableComponent base = condition.getTimeMode() == QuestPrerequisites.TimeMode.REAL_TIME
				? Component.translatable("gui.dragonminez.quests.requirement.time_real", formatRealTimeDuration(duration))
				: Component.translatable("gui.dragonminez.quests.requirement.time_game", formatGameTimeDuration(duration));

		if (context == null || context.questKey() == null || context.questKey().isBlank() || duration <= 0L || context.data() == null) {
			return base;
		}

		PlayerQuestData.QuestStartRequirementTiming timing = context.data().getPlayerQuestData().getStartRequirementTiming(context.questKey());
		if (timing == null) {
			return base;
		}

		long remaining;
		if (condition.getTimeMode() == QuestPrerequisites.TimeMode.REAL_TIME) {
			long elapsed = Math.max(0L, context.realTimeMs() - timing.getRealTimeStartedMs());
			remaining = Math.max(0L, duration - elapsed);
			if (remaining > 0L) {
				return base.append(Component.literal(" ("))
						.append(Component.translatable("gui.dragonminez.quests.requirement.remaining", formatRealTimeDuration(remaining)))
						.append(Component.literal(")"));
			}
			return base.append(Component.literal(" ("))
					.append(Component.translatable("gui.dragonminez.quests.requirement.ready"))
					.append(Component.literal(")"));
		}

		long elapsed = Math.max(0L, context.gameTime() - timing.getGameTimeStarted());
		remaining = Math.max(0L, duration - elapsed);
		if (remaining > 0L) {
			return base.append(Component.literal(" ("))
					.append(Component.translatable("gui.dragonminez.quests.requirement.remaining", formatGameTimeDuration(remaining)))
					.append(Component.literal(")"));
		}
		return base.append(Component.literal(" ("))
				.append(Component.translatable("gui.dragonminez.quests.requirement.ready"))
				.append(Component.literal(")"));
	}

	private static Component buildAlignmentRequirement(QuestPrerequisites.Condition condition) {
		Integer min = condition.getMinAlignment();
		Integer max = condition.getMaxAlignment();
		if (min != null && max != null) {
			return Component.translatable("gui.dragonminez.quests.requirement.alignment_range", min, max);
		}
		if (min != null) {
			return Component.translatable("gui.dragonminez.quests.requirement.alignment_min", min);
		}
		if (max != null) {
			return Component.translatable("gui.dragonminez.quests.requirement.alignment_max", max);
		}
		return Component.translatable("gui.dragonminez.quests.requirement.alignment");
	}

	private static Component resolveSagaQuestName(String sagaId, Integer questId) {
		if (sagaId == null || questId == null) {
			return Component.literal("?");
		}

		Saga saga = QuestRegistry.getSaga(sagaId);
		if (saga == null) {
			saga = QuestRegistry.getClientSaga(sagaId);
		}
		if (saga != null) {
			Quest quest = saga.getQuestById(questId);
			if (quest != null) {
				return displayText(quest.getTitle());
			}
		}
		return Component.literal(humanizeIdentifier(sagaId) + " " + questId);
	}

	private static Component resolveQuestName(String questId) {
		if (questId == null || questId.isBlank()) {
			return Component.literal("?");
		}

		Quest quest = QuestRegistry.getQuest(questId);
		if (quest == null) {
			quest = QuestRegistry.getClientQuest(questId);
		}
		if (quest != null) {
			return displayText(quest.getTitle());
		}
		return Component.literal(humanizeResourceIdentifier(questId));
	}

	private static String formatSeconds(long totalSeconds) {
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;

		if (hours > 0L) {
			return minutes > 0L ? hours + "h " + minutes + "m" : hours + "h";
		}
		if (minutes > 0L) {
			return seconds > 0L ? minutes + "m " + seconds + "s" : minutes + "m";
		}
		return seconds + "s";
	}

	private static ResourceLocation safeParse(String id) {
		if (id == null || id.isBlank() || !id.contains(":")) {
			return null;
		}

		try {
			return ResourceLocation.parse(id);
		} catch (Exception ignored) {
			return null;
		}
	}

	public record RequirementContext(StatsData data, Player player, String questKey) {
		public Level level() {
			return player != null ? player.level() : null;
		}

		public long gameTime() {
			return level() != null ? level().getGameTime() : 0L;
		}

		public long realTimeMs() {
			return System.currentTimeMillis();
		}
	}
}
