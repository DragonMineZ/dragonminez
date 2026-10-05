package com.dragonminez.client.gui.dialogue;

import com.dragonminez.client.gui.MastersSkillsScreen;
import com.dragonminez.client.gui.character.minigames.BaseMinigameScreen;
import com.dragonminez.client.systems.worldboss.ClientWorldBossState;
import com.dragonminez.client.util.LocalizationUtil;
import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TournamentDefinition;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.network.C2S.DialogueChoiceC2S;
import com.dragonminez.common.network.C2S.NPCActionC2S;
import com.dragonminez.common.network.C2S.QuestActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.DialogueResultS2C;
import com.dragonminez.common.network.S2C.OpenDialogueNodeS2C;
import com.dragonminez.common.network.TournamentPackets;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestAvailabilityChecker;
import com.dragonminez.common.quest.QuestPrerequisites;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.training.MinigameOrigin;
import com.dragonminez.common.util.DMZTextPlaceholders;
import com.dragonminez.server.util.BabaReviveService;
import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
final class NpcDialogueContent {
	private static final Set<String> TEXT_MASTERS = Set.of("karin", "guru", "dende", "enma", "baba", "popo", "gero",
			"toribot", "babidi", "grandkai", "otherworld_announcer");
	private static final String HELL_OGRE = "hell_ogre";
	private static final String LINES = "gui.dragonminez.lines.";
	private static final String STAGE = "dialogue.dragonminez.story.sidequest.";
	private static final String[] MINIGAMES = {"rhythm", "control", "memory", "precision", "gravity"};

	private final NpcDialogueScreen host;
	private final String npcId;
	private final boolean masterNpc;
	private final List<DialogueQuest> quests;
	private final MasterServices services;
	private final Map<String, Runnable> pending = new HashMap<>();
	private final Set<String> used = new HashSet<>();
	private final Map<String, String> picks = new HashMap<>();

	NpcDialogueContent(NpcDialogueScreen host, String npcId, boolean masterNpc, List<DialogueQuest> quests) {
		this.host = host;
		this.npcId = npcId;
		this.masterNpc = masterNpc;
		this.quests = quests;
		this.services = new MasterServices(this, host, npcId);
	}

	String npcId() {
		return npcId;
	}

	DialoguePage root() {
		return DialoguePage.speech("root", this::rootLine, this::rootOptions).asRoot();
	}

	DialoguePage questsAsRoot() {
		return questList().asRoot();
	}

	DialoguePage tree(OpenDialogueNodeS2C node) {
		return DialoguePage.speech("tree:" + node.getNodeId(),
				() -> ph(LocalizationUtil.localizedOrReadable(node.getLine()).copy()),
				() -> {
					List<DialogueOption> options = new ArrayList<>();
					for (int i = 0; i < node.getChoiceTexts().size(); i++) {
						int choiceIndex = node.getChoiceIndices().get(i);
						options.add(DialogueOption.of("choice:" + i, ph(LocalizationUtil.localizedOrReadable(node.getChoiceTexts().get(i)).copy()),
								() -> {
									host.awaitServer();
									NetworkHandler.sendToServer(new DialogueChoiceC2S(npcId, host.entityId(), node.getNodeId(), choiceIndex));
								}).enabled(!host.awaitingServer()));
					}
					return options;
				}).asRoot();
	}

	void perform(int actionId, int value, Runnable onSuccess) {
		String key = DialogueResultS2C.npcAction(actionId);
		if (pending.containsKey(key)) return;
		NetworkHandler.sendToServer(new NPCActionC2S(npcId, actionId, value));
		pending.put(key, onSuccess);
		host.refreshOptions();
	}

	Runnable succeed(String event, boolean popToRoot, boolean markUsed, Object... args) {
		return () -> {
			if (markUsed) used.add(event);
			if (popToRoot) host.popToRoot();
			host.say(DialogueReplies.reply(npcId, event, args));
			playConfirm();
		};
	}

	void sendAndClose(int actionId) {
		NetworkHandler.sendToServer(new NPCActionC2S(npcId, actionId));
		host.close();
	}

	boolean isPending(int actionId) {
		return pending.containsKey(DialogueResultS2C.npcAction(actionId));
	}

	boolean isUsed(String event) {
		return used.contains(event);
	}

	void onResult(DialogueResultS2C result) {
		Runnable onSuccess = pending.remove(result.getAction());
		if (onSuccess == null) return;
		if (result.isSuccess()) onSuccess.run();
		else host.say(DialogueReplies.reply(npcId, result.getAction().startsWith("quest_") ? "quest_failure" : "failure"));
		host.refreshOptions();
	}

	void accept(DialogueQuest quest) {
		String key = DialogueResultS2C.questStart(quest.id);
		if (pending.containsKey(key)) return;
		NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.START, quest.id, npcId));
		pending.put(key, () -> {
			quest.setStatus(DialogueQuest.Status.IN_PROGRESS);
			host.popToRoot();
			host.say(DialogueReplies.reply(npcId, "quest_accept"));
			playConfirm();
		});
	}

	void turnIn(DialogueQuest quest) {
		String key = DialogueResultS2C.questTurnIn(quest.id);
		if (pending.containsKey(key)) return;
		NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.TURN_IN, quest.id, npcId));
		pending.put(key, () -> {
			quests.remove(quest);
			host.popToRoot();
			host.say(DialogueReplies.reply(npcId, "quest_turn_in"));
			playConfirm();
		});
	}

	private MutableComponent rootLine() {
		LocalPlayer player = Minecraft.getInstance().player;
		StatsData stats = stats();
		if (HELL_OGRE.equals(npcId)) return hellOgreLine();
		if (player != null && disliked(stats)) return line(dislikedKey(), player.getName());
		if (stats != null && player != null) {
			switch (npcId) {
				case "enma" -> {
					return enmaLine(stats);
				}
				case "baba" -> {
					return babaLine(stats);
				}
				case "gero" -> {
					if (!geroEligible(stats)) return line(pick("gero_rejects", LINES + "gero.not_eligible"), player.getName());
				}
				default -> {
				}
			}
		}
		if (hasQuest(DialogueQuest.Status.TURN_IN)) return stageLine("complete");
		String greeting = masterNpc ? pick("greeting", LINES + npcId + ".main", LINES + npcId + ".idle", STAGE + npcId + ".idle") : null;
		if (greeting != null && player != null) return line(greeting, player.getName());
		if (hasQuest(DialogueQuest.Status.OFFER)) return stageLine("offer");
		if (hasQuest(DialogueQuest.Status.IN_PROGRESS)) return stageLine("in_progress");
		return stageLine("idle");
	}

	private List<DialogueOption> rootOptions() {
		List<DialogueOption> options = new ArrayList<>();
		Component alignmentLock = alignmentLock();
		if (!quests.isEmpty()) options.add(questsOption(alignmentLock));

		if (masterNpc) {
			boolean skillMaster = !TEXT_MASTERS.contains(npcId);
			if (skillMaster) {
				options.add(lockedBy(DialogueOption.of("skills", tr("gui.dragonminez.npc.skills"),
						() -> host.openScreen(new MastersSkillsScreen(npcId, host.npcEntity()))), alignmentLock));
				options.add(lockedBy(DialogueOption.of("train", tr("gui.dragonminez.npc.train"), () -> host.push(training())).submenu(), alignmentLock));
			} else if ("popo".equals(npcId) && inTimeChamber()) {
				options.add(lockedBy(DialogueOption.of("train", tr("gui.dragonminez.npc.train"), () -> host.push(training())).submenu(), alignmentLock));
			}

			StatsData stats = stats();
			LocalPlayer player = Minecraft.getInstance().player;
			if (stats != null && player != null) services.add(options, stats, player);

			if (TournamentDefinition.HOST_NPCS.containsKey(npcId)) {
				options.add(DialogueOption.of("tournament", tr("gui.dragonminez.npc.tournament"),
						() -> NetworkHandler.sendToServer(new TournamentPackets.ActionC2S(
								TournamentPackets.ActionC2S.Action.OPEN_BRACKET, host.entityId()))));
			}
		}

		if (HELL_OGRE.equals(npcId) && ClientWorldBossState.isLairKnown(WorldBossEntity.JANEMBA)) {
			options.add(DialogueOption.of("location", tr("gui.dragonminez.npc.show_location"), () -> {
				BlockPos lair = ClientWorldBossState.getLair(WorldBossEntity.JANEMBA);
				host.say(line("dialogue.dragonminez.story.sidequest." + npcId + ".location", lair.getX(), lair.getZ()));
			}));
		}
		return options;
	}

	private DialoguePage training() {
		return DialoguePage.speech("train", () -> {
			String own = "gui.dragonminez.lines." + npcId + ".training";
			LocalPlayer player = Minecraft.getInstance().player;
			if (I18n.exists(own) && player != null) return line(own, player.getName());
			return line("gui.dragonminez.dialogue.train.prompt");
		}, () -> {
			List<DialogueOption> options = new ArrayList<>();
			options.add(DialogueOption.of("shadow", tr("gui.dragonminez.button.popo.shadow"), () -> {
				NetworkHandler.sendToServer(new NPCActionC2S("popo", 1));
				host.close();
			}));
			String minigame = minigameFor(npcId);
			if (minigame != null) {
				options.add(DialogueOption.of("minigame", tr("gui.dragonminez.minigame." + minigame),
						() -> host.openScreen(BaseMinigameScreen.create(minigame, MinigameOrigin.MASTER))));
			}
			return options;
		});
	}

	private DialogueOption questsOption(Component alignmentLock) {
		DialogueSkin.Icon icon = DialogueSkin.Icon.LOCK;
		int color = DialogueSkin.MUTED;
		if (hasQuest(DialogueQuest.Status.TURN_IN)) {
			icon = DialogueSkin.Icon.TURN_IN;
			color = DialogueSkin.TURN_IN;
		} else if (hasQuest(DialogueQuest.Status.OFFER) && alignmentLock == null) {
			icon = DialogueSkin.Icon.OFFER;
			color = DialogueSkin.OFFER;
		} else if (hasQuest(DialogueQuest.Status.IN_PROGRESS)) {
			icon = DialogueSkin.Icon.PROGRESS;
			color = DialogueSkin.PROGRESS;
		}
		DialogueOption option = DialogueOption.of("quests", tr("gui.dragonminez.dialogue.quests"), () -> host.push(questList()))
				.icon(icon, color).submenu();
		boolean ongoing = hasQuest(DialogueQuest.Status.TURN_IN) || hasQuest(DialogueQuest.Status.IN_PROGRESS);
		return ongoing ? option : lockedBy(option, alignmentLock);
	}

	private DialoguePage questList() {
		return DialoguePage.speech("quests", this::questPrompt, this::questOptions);
	}

	private MutableComponent questPrompt() {
		if (quests.isEmpty()) return line("gui.dragonminez.sidequest.no_quests");
		if (masterNpc) {
			if (hasQuest(DialogueQuest.Status.TURN_IN) && I18n.exists(STAGE + npcId + ".complete")) return line(STAGE + npcId + ".complete");
			if (hasQuest(DialogueQuest.Status.OFFER) && alignmentLock() == null && I18n.exists(STAGE + npcId + ".offer")) return line(STAGE + npcId + ".offer");
			if (hasQuest(DialogueQuest.Status.IN_PROGRESS) && I18n.exists(STAGE + npcId + ".in_progress")) return line(STAGE + npcId + ".in_progress");
		}
		return line("gui.dragonminez.dialogue.quests.prompt");
	}

	List<DialogueOption> questOptions() {
		List<DialogueOption> options = new ArrayList<>();
		long now = Util.getMillis();
		Component alignmentLock = alignmentLock();
		for (DialogueQuest quest : quests) {
			DialogueQuest.Status status = quest.status(now);
			DialogueOption option = DialogueOption.of(DialogueQuest.optionId(quest.id), ph(tr(quest.quest.getTitle())),
					() -> host.showQuest(DialoguePage.quest(quest, this::questOptions)));
			switch (status) {
				case TURN_IN -> option.icon(DialogueSkin.Icon.TURN_IN, DialogueSkin.TURN_IN);
				case OFFER -> {
					option.icon(DialogueSkin.Icon.OFFER, DialogueSkin.OFFER);
					if (alignmentLock != null) {
						option.icon(DialogueSkin.Icon.LOCK, DialogueSkin.MUTED).locked(tr("gui.dragonminez.dialogue.quest.locked"), List.of(alignmentLock));
					}
				}
				case IN_PROGRESS -> option.icon(DialogueSkin.Icon.PROGRESS, DialogueSkin.PROGRESS);
				case COOLDOWN -> option.icon(DialogueSkin.Icon.CLOCK, DialogueSkin.MUTED)
						.cooldownUntil(quest.cooldownEndMs)
						.locked(tr("gui.dragonminez.dialogue.quest.locked"), quest.lockReason != null ? List.of(quest.lockReason) : List.of());
				case LOCKED -> option.icon(DialogueSkin.Icon.LOCK, DialogueSkin.MUTED)
						.locked(tr("gui.dragonminez.dialogue.quest.locked"), lockLines(quest));
			}
			if (status == DialogueQuest.Status.COOLDOWN) option.enabled(true);
			options.add(option);
		}
		return options;
	}

	private List<Component> lockLines(DialogueQuest entry) {
		StatsData stats = stats();
		QuestPrerequisites.Condition condition = stats == null ? null : QuestAvailabilityChecker.firstFailingCondition(entry.quest, stats);
		if (condition != null && condition.getType() == QuestPrerequisites.ConditionType.SAGA_QUEST && condition.getQuestId() != null) {
			Saga saga = QuestRegistry.getClientSaga(condition.getSagaId());
			Quest required = saga != null ? saga.getQuestById(condition.getQuestId()) : null;
			if (required != null) {
				return List.of(tr("gui.dragonminez.quests.requirement.complete_saga", highlight(required.getTitle())),
						tr("gui.dragonminez.dialogue.quest.saga_step", tr(saga.getName()), condition.getQuestId()));
			}
		}
		if (condition != null && condition.getType() == QuestPrerequisites.ConditionType.QUEST) {
			Quest required = QuestRegistry.getClientQuest(condition.getRequiredQuestId());
			if (required != null) {
				List<Component> lines = new ArrayList<>();
				lines.add(tr("gui.dragonminez.quests.requirement.complete_quest", highlight(required.getTitle())));
				String giver = required.getQuestGiver();
				if (giver != null && !giver.isBlank()) lines.add(tr("gui.dragonminez.dialogue.quest.sidequest_from", npcDisplayName(giver)));
				return lines;
			}
		}
		return entry.lockReason != null ? List.of(entry.lockReason) : List.of();
	}

	private Component alignmentLock() {
		StatsData stats = stats();
		return masterNpc && stats != null ? NpcDispositionService.masterAlignmentBlocker(stats, npcId) : null;
	}

	private boolean disliked(StatsData stats) {
		return alignmentLock() != null || masterNpc && stats != null && MasterServices.dislikes(npcId, stats);
	}

	private String dislikedKey() {
		String own = pick("disliked", LINES + npcId + ".disliked");
		return own != null ? own : pick("disliked_generic", "gui.dragonminez.dialogue.disliked");
	}

	private String pick(String slot, String... bases) {
		return picks.computeIfAbsent(slot, key -> DialogueReplies.pickKey(bases));
	}

	private static DialogueOption lockedBy(DialogueOption option, Component reason) {
		if (reason == null) return option;
		return option.locked(tr("gui.dragonminez.dialogue.locked"), List.of(reason));
	}

	private static MutableComponent highlight(String titleKey) {
		return ph(tr(titleKey)).withStyle(ChatFormatting.WHITE);
	}

	static MutableComponent npcDisplayName(String npcId) {
		for (String key : new String[]{"entity.dragonminez.master_" + npcId, "entity.dragonminez.questnpc." + npcId,
				"gui.dragonminez.lines." + npcId + ".name"}) {
			if (I18n.exists(key)) return tr(key);
		}
		StringBuilder builder = new StringBuilder();
		for (String word : npcId.split("[_ ]+")) {
			if (word.isEmpty()) continue;
			if (!builder.isEmpty()) builder.append(' ');
			builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return Component.literal(builder.toString()).withStyle(Style.EMPTY.withFont(NpcDialogueScreen.FONT));
	}

	private MutableComponent enmaLine(StatsData stats) {
		Component name = playerName();
		if (stats.getStatus().isAlive()) return line(pick("enma_alive", LINES + "enma.main"), name);
		if (BabaReviveService.canEnmaReturn(stats)) return line("gui.dragonminez.lines.enma.main_temporary", name);
		if (temporaryRevive()) return line("gui.dragonminez.lines.enma.need_baba_temporary", name);
		return line(pick("enma_dead", LINES + "enma.need_baba"), name);
	}

	private MutableComponent babaLine(StatsData stats) {
		Component name = playerName();
		boolean temporary = temporaryRevive();
		if (stats.getStatus().isAlive()) return line(pick("baba_alive", LINES + "baba.already_alive", LINES + "baba.idle"), name);
		if (temporary && BabaReviveService.isTempReviveActive(stats)) return line("gui.dragonminez.lines.baba.temporary_active", name);
		if (stats.getCooldowns().hasCooldown(Cooldowns.REVIVE_BABA)) {
			return line("gui.dragonminez.lines.baba.wait", stats.getCooldowns().getCooldown(Cooldowns.REVIVE_BABA) / 20);
		}
		if (temporary) {
			return line("gui.dragonminez.lines.baba.main_temporary", name,
					ConfigManager.getServerConfig().getGameplay().getBabaTempReturnSeconds() / 60);
		}
		return line(pick("baba_dead", LINES + "baba.main"), name);
	}

	private MutableComponent hellOgreLine() {
		if (ClientWorldBossState.getRespawnTicksRemaining(WorldBossEntity.JANEMBA) > 0L) {
			long seconds = Math.max(0L, ClientWorldBossState.getRespawnTicksRemaining(WorldBossEntity.JANEMBA) / 20L);
			return line("dialogue.dragonminez.story.sidequest." + npcId + ".boss_respawning", CooldownClock.format(seconds * 1000L));
		}
		return line("dialogue.dragonminez.story.sidequest." + npcId + ".boss_alive");
	}

	private MutableComponent stageLine(String stage) {
		String own = STAGE + npcId + "." + stage;
		return line(I18n.exists(own) ? own : STAGE + "generic_npc." + stage);
	}

	private boolean hasQuest(DialogueQuest.Status status) {
		long now = Util.getMillis();
		return quests.stream().anyMatch(quest -> quest.status(now) == status);
	}

	static boolean geroEligible(StatsData stats) {
		return ConfigManager.getRaceCharacter(stats.getCharacter().getRaceName()).getFormSkillTpCosts("androidforms").length > 0;
	}

	static boolean temporaryRevive() {
		return ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()
				&& ConfigManager.getServerConfig().getGameplay().getBabaTemporaryRevive();
	}

	static boolean inTimeChamber() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && player.level().dimension().equals(HTCDimension.HTC_KEY);
	}

	private static String minigameFor(String master) {
		for (String gameId : MINIGAMES) {
			if (ConfigManager.getTrainingConfig().getSettings(gameId).getMasterName().equalsIgnoreCase(master)) return gameId;
		}
		return null;
	}

	static int cooldown(String key) {
		StatsData stats = stats();
		return stats == null ? 0 : stats.getCooldowns().getCooldown(key);
	}

	static StatsData stats() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null ? null : StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
	}

	static Component playerName() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null ? Component.empty() : player.getName();
	}

	private static void playConfirm() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) player.playSound(MainSounds.CONFIRM_MENU.get(), 0.6f, 1.0f);
	}

	static MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(NpcDialogueScreen.FONT));
	}

	static MutableComponent line(String key, Object... args) {
		return ph(tr(key, args));
	}

	static MutableComponent ph(MutableComponent component) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return component;
		return DMZTextPlaceholders.apply(component, player).copy().withStyle(Style.EMPTY.withFont(NpcDialogueScreen.FONT));
	}
}
