package com.dragonminez.client.gui.dialogue;

import com.dragonminez.client.gui.character.minigames.UltimateChallenge;
import com.dragonminez.client.gui.hair.HairEditorScreen;
import com.dragonminez.common.alignment.AlignmentBand;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.server.util.BabaReviveService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class MasterServices {
	private static final int POTENTIAL_LEVEL = 10;
	private static final int GURU_MIN_ALIGNMENT = 50;
	private static final int OLDKAI_MIN_ALIGNMENT = 62;
	private static final int BABIDI_MAX_ALIGNMENT = 38;
	private static final int BLESS_MIN_ALIGNMENT = 61;
	private static final float FULL_RESOURCE_SLACK = 0.5f;

	private final NpcDialogueContent content;
	private final NpcDialogueScreen host;
	private final String npcId;

	MasterServices(NpcDialogueContent content, NpcDialogueScreen host, String npcId) {
		this.content = content;
		this.host = host;
		this.npcId = npcId;
	}

	static boolean dislikes(String npcId, StatsData stats) {
		int alignment = stats.getResources().getAlignment();
		return switch (npcId) {
			case "guru" -> alignment < GURU_MIN_ALIGNMENT;
			case "babidi" -> alignment > BABIDI_MAX_ALIGNMENT;
			default -> false;
		};
	}

	void add(List<DialogueOption> options, StatsData stats, LocalPlayer player) {
		switch (npcId) {
			case "karin" -> karin(options, player);
			case "guru" -> guru(options, stats);
			case "dende" -> dende(options, stats, player);
			case "enma" -> {
				DialogueOption earth = closing("earth", "gui.dragonminez.button.enma.earth", 1);
				if (!BabaReviveService.canEnmaReturn(stats)) lock(earth, reason("gui.dragonminez.dialogue.locked.need_baba"));
				options.add(earth);
			}
			case "baba" -> baba(options, stats);
			case "popo" -> {
				if (!NpcDialogueContent.inTimeChamber() && HairManager.canUseHair(stats.getCharacter())) {
					options.add(DialogueOption.of("haircut", NpcDialogueContent.tr("gui.dragonminez.button.popo.haircut"), () -> {
						StatsData current = NpcDialogueContent.stats();
						if (current != null) host.openScreen(new HairEditorScreen(host.screen(), current.getCharacter()));
					}));
				}
			}
			case "gero" -> gero(options, stats);
			case "piccolo" -> {
				options.add(heal("gui.dragonminez.button.piccolo.heal", stats, player));
				options.add(weightOption("gui.dragonminez.button.piccolo.weight", "gui.dragonminez.button.piccolo.confirm"));
			}
			case "roshi", "kingkai" -> options.add(weightOption("gui.dragonminez.button.weight", "gui.dragonminez.button.weight.confirm"));
			case "oldkai" -> oldKai(options, stats);
			case "babidi" -> babidi(options, stats);
			case "grandkai" -> options.add(closing("grounds", "gui.dragonminez.button.grandkai.tournament", 1));
			case "otherworld_announcer" -> options.add(closing("return", "gui.dragonminez.button.otherworld_announcer.return", 1));
			default -> {
			}
		}
	}

	private void karin(List<DialogueOption> options, LocalPlayer player) {
		DialogueOption nimbus = simple("nimbus", "gui.dragonminez.button.karin.nimbus", 1, "nimbus", true);
		boolean owned = player.getInventory().contains(new ItemStack(MainItems.NUBE_ITEM.get()))
				|| player.getInventory().contains(new ItemStack(MainItems.NUBE_NEGRA_ITEM.get()));
		if (owned || content.isUsed("nimbus")) lock(nimbus, reason("gui.dragonminez.dialogue.locked.nimbus_owned"));
		options.add(nimbus);
		options.add(simple("senzu", "gui.dragonminez.button.karin.senzu", 2, "senzu", false)
				.cooldown(() -> NpcDialogueContent.cooldown(Cooldowns.SENZU_KARIN)));
	}

	private void guru(List<DialogueOption> options, StatsData stats) {
		int potential = stats.getBaseSkills().getSkillLevel("potentialunlock");
		DialogueOption unlock = simple("unlock_potential", "gui.dragonminez.button.guru.unlock_potential", 1, "unlock_potential", false);
		if (potential > POTENTIAL_LEVEL) {
			lock(unlock, reason("gui.dragonminez.dialogue.locked.potential_unlocked"));
		} else if (stats.getResources().getAlignment() < GURU_MIN_ALIGNMENT) {
			lock(unlock, reason("message.dragonminez.npc.master_alignment_too_low", GURU_MIN_ALIGNMENT))
					.onLocked(() -> host.say(NpcDialogueContent.line("gui.dragonminez.lines.guru.evil")));
		} else if (potential < POTENTIAL_LEVEL) {
			lock(unlock, reason("gui.dragonminez.dialogue.locked.potential_level", POTENTIAL_LEVEL))
					.onLocked(() -> host.say(NpcDialogueContent.line("gui.dragonminez.lines.guru.level")));
		}
		options.add(unlock);
	}

	private void dende(List<DialogueOption> options, StatsData stats, LocalPlayer player) {
		options.add(heal("gui.dragonminez.button.dende.heal", stats, player));
		if (ConfigManager.getRaceCharacter(stats.getCharacter().getRace()).getHasSaiyanTail()) {
			boolean hasTail = stats.getCharacter().isHasSaiyanTail();
			options.add(simple("tail", hasTail ? "gui.dragonminez.button.dende.remove_tail" : "gui.dragonminez.button.dende.grow_tail",
					3, hasTail ? "tail_remove" : "tail_grow", false));
		}
		DialogueOption bless = simple("bless", "gui.dragonminez.button.dende.bless", 4, "bless", false)
				.cooldown(() -> NpcDialogueContent.cooldown(Cooldowns.KAMI_BLESS));
		if (AlignmentBand.fromValue(stats.getResources().getAlignment()) != AlignmentBand.GOOD) {
			lock(bless, reason("message.dragonminez.npc.master_alignment_too_low", BLESS_MIN_ALIGNMENT));
		}
		options.add(bless);
		options.add(DialogueOption.of("reset", NpcDialogueContent.tr("gui.dragonminez.button.dende.reset"), () -> host.push(dendeReset())).submenu());
	}

	private void baba(List<DialogueOption> options, StatsData stats) {
		boolean temporary = NpcDialogueContent.temporaryRevive();
		DialogueOption revive = simple("revive", temporary ? "gui.dragonminez.button.baba.revive_temporary" : "gui.dragonminez.button.baba.revive",
				1, "revive", false).cooldown(() -> NpcDialogueContent.cooldown(Cooldowns.REVIVE_BABA));
		if (stats.getStatus().isAlive()) lock(revive, reason("gui.dragonminez.dialogue.locked.alive"));
		else if (temporary && BabaReviveService.isTempReviveActive(stats)) lock(revive, reason("gui.dragonminez.dialogue.locked.time_borrowed"));
		options.add(revive);
	}

	private void gero(List<DialogueOption> options, StatsData stats) {
		if (!NpcDialogueContent.geroEligible(stats)) {
			options.add(DialogueOption.of("gero_leave", NpcDialogueContent.tr("gui.dragonminez.button.gero.not_interested"), host::close));
			return;
		}
		DialogueOption offer = DialogueOption.of("gero_offer", NpcDialogueContent.tr("gui.dragonminez.button.gero.accept"), () -> host.push(geroOffer())).submenu();
		if (stats.getStatus().isAndroidUpgraded()) lock(offer, reason("gui.dragonminez.dialogue.locked.android"));
		options.add(offer);
	}

	private void oldKai(List<DialogueOption> options, StatsData stats) {
		DialogueOption ultimate = DialogueOption.of("ultimate", NpcDialogueContent.tr("gui.dragonminez.button.oldkai.unlock_ultimate"),
				() -> new UltimateChallenge().start());
		if (stats.getResources().getAlignment() < OLDKAI_MIN_ALIGNMENT) {
			lock(ultimate, reason("message.dragonminez.npc.master_alignment_too_low", OLDKAI_MIN_ALIGNMENT))
					.onLocked(() -> host.say(NpcDialogueContent.line("gui.dragonminez.lines.oldkai.evil")));
		} else if (stats.getBaseSkills().getSkillLevel("potentialunlock") < POTENTIAL_LEVEL) {
			lock(ultimate, reason("gui.dragonminez.dialogue.locked.potential_level", POTENTIAL_LEVEL))
					.onLocked(() -> host.say(NpcDialogueContent.line("gui.dragonminez.lines.oldkai.level")));
		}
		options.add(ultimate);
	}

	private void babidi(List<DialogueOption> options, StatsData stats) {
		DialogueOption mark = simple("mark", "gui.dragonminez.button.babidi.mark", 1, "mark", false);
		if (stats.getEffects().hasEffect("majin")) {
			lock(mark, reason("gui.dragonminez.dialogue.locked.majin"))
					.onLocked(() -> host.say(NpcDialogueContent.line("gui.dragonminez.lines.babidi.already")));
		} else if (stats.getResources().getAlignment() > BABIDI_MAX_ALIGNMENT) {
			lock(mark, reason("message.dragonminez.npc.master_alignment_too_high", BABIDI_MAX_ALIGNMENT))
					.onLocked(() -> host.say(NpcDialogueContent.line("gui.dragonminez.lines.babidi.too_good")));
		}
		options.add(mark);
	}

	private DialogueOption heal(String labelKey, StatsData stats, LocalPlayer player) {
		DialogueOption heal = simple("heal", labelKey, 1, "heal", false);
		boolean healthy = player.getHealth() >= player.getMaxHealth() - FULL_RESOURCE_SLACK
				&& stats.getResources().getCurrentEnergy() >= stats.getMaxEnergy() - FULL_RESOURCE_SLACK
				&& stats.getResources().getCurrentStamina() >= stats.getMaxStamina() - FULL_RESOURCE_SLACK;
		if (healthy) lock(heal, reason("gui.dragonminez.dialogue.locked.full_health"));
		return heal;
	}

	private DialogueOption weightOption(String labelKey, String confirmKey) {
		DialogueOption weight = DialogueOption.of("weight", NpcDialogueContent.tr(labelKey), () -> host.push(weightPage(confirmKey))).submenu();
		if (content.isUsed("weight")) lock(weight, reason("gui.dragonminez.dialogue.locked.weight_given"));
		return weight;
	}

	private DialoguePage weightPage(String confirmKey) {
		int maxWeight = Math.max(1, ConfigManager.getServerConfig().getGravity().getMaxWeightRequestable().intValue());
		return DialoguePage.speech("weight", () -> NpcDialogueContent.line("gui.dragonminez.lines." + npcId + ".weight_prompt",
				NpcDialogueContent.playerName()), () -> List.of(
				DialogueOption.of("weight_confirm", NpcDialogueContent.tr(confirmKey), () -> {
					int weight = Math.min(host.inputValue(), maxWeight);
					if (weight <= 0) return;
					content.perform(2, weight, content.succeed("weight", true, true, weight));
				}).tone(DialogueOption.Tone.PRIMARY).enabled(host.inputValue() > 0 && !content.isPending(2))))
				.withNumberInput(String.valueOf(maxWeight).length(), NpcDialogueContent.tr("gui.dragonminez.dialogue.weight.hint", maxWeight));
	}

	private DialoguePage dendeReset() {
		return DialoguePage.speech("dende_reset", () -> NpcDialogueContent.line("gui.dragonminez.lines.dende.reset_warning",
				NpcDialogueContent.playerName()), () -> List.of(
				closing("reset_confirm", "gui.dragonminez.button.dende.reset_confirm", 2).tone(DialogueOption.Tone.DANGER),
				DialogueOption.of("reset_cancel", NpcDialogueContent.tr("gui.dragonminez.button.dende.reset_cancel"), host::pop)));
	}

	private DialoguePage geroOffer() {
		return DialoguePage.speech("gero_offer", () -> NpcDialogueContent.line("gui.dragonminez.lines.gero.offer",
				NpcDialogueContent.playerName()), () -> List.of(
				DialogueOption.of("gero_interest", NpcDialogueContent.tr("gui.dragonminez.button.gero.interest"), () -> host.push(geroConfirm())).submenu(),
				DialogueOption.of("gero_decline", NpcDialogueContent.tr("gui.dragonminez.button.gero.not_interested"), host::popToRoot)));
	}

	private DialoguePage geroConfirm() {
		return DialoguePage.speech("gero_confirm", () -> NpcDialogueContent.line("gui.dragonminez.lines.gero.confirm",
				NpcDialogueContent.playerName()), () -> List.of(
				DialogueOption.of("gero_convert", NpcDialogueContent.tr("gui.dragonminez.button.gero.confirm"),
						() -> content.perform(1, 0, content.succeed("convert", true, false)))
						.tone(DialogueOption.Tone.DANGER).enabled(!content.isPending(1)),
				DialogueOption.of("gero_cancel", NpcDialogueContent.tr("gui.dragonminez.button.gero.cancel"), host::popToRoot)));
	}

	private DialogueOption simple(String id, String labelKey, int actionId, String event, boolean markUsed) {
		return DialogueOption.of(id, NpcDialogueContent.tr(labelKey), () -> content.perform(actionId, 0, content.succeed(event, false, markUsed)))
				.enabled(!content.isPending(actionId));
	}

	private DialogueOption closing(String id, String labelKey, int actionId) {
		return DialogueOption.of(id, NpcDialogueContent.tr(labelKey), () -> content.sendAndClose(actionId));
	}

	private static DialogueOption lock(DialogueOption option, Component reason) {
		return option.locked(NpcDialogueContent.tr("gui.dragonminez.dialogue.locked"), List.of(reason));
	}

	private static Component reason(String key, Object... args) {
		return NpcDialogueContent.tr(key, args);
	}
}
