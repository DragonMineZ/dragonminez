package com.dragonminez.client.events;

import com.dragonminez.client.gui.dialogue.DialogueOption;
import com.dragonminez.client.gui.dialogue.NpcDialogueScreen;
import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class DMZClientEvent {

	/**
	 * Fired when player starts the upswing (aka windup).
	 */
	public static class PlayerAttackStart extends Event {
		private final LocalPlayer player;
		private final AttackHand attackHand;

		public PlayerAttackStart(LocalPlayer player, AttackHand attackHand) {
			this.player = player;
			this.attackHand = attackHand;
		}

		public LocalPlayer getPlayer() {
			return this.player;
		}

		public AttackHand getAttackHand() {
			return this.attackHand;
		}
	}


	/**
	 * Fired when player hits some targets (can be zero or more targets).
	 */
	public static class PlayerAttackHit extends Event {
		private final LocalPlayer player;
		private final AttackHand attackHand;
		private final List<Entity> targets;
		@Nullable
		private final Entity cursorTarget;

		public PlayerAttackHit(LocalPlayer player, AttackHand attackHand, List<Entity> targets, @Nullable Entity cursorTarget) {
			this.player = player;
			this.attackHand = attackHand;
			this.targets = targets;
			this.cursorTarget = cursorTarget;
		}

		@Nullable
		public Entity getCursorTarget() {
			return cursorTarget;
		}

		public LocalPlayer getPlayer() {
			return this.player;
		}

		public AttackHand getAttackHand() {
			return this.attackHand;
		}

		public List<Entity> getTargets() {
			return this.targets;
		}
	}


	/**
	 * Fired client-side when the local player begins charging (casts) a Ki attack from a slot.
	 */
	public static class KiAttackCast extends Event {
		private final LocalPlayer player;
		private final int slot;

		public KiAttackCast(LocalPlayer player, int slot) {
			this.player = player;
			this.slot = slot;
		}

		public LocalPlayer getPlayer() {
			return this.player;
		}

		public int getSlot() {
			return this.slot;
		}
	}


	/**
	 * Fired client-side when the local player releases a charged Ki attack.
	 */
	public static class KiAttackRelease extends Event {
		private final LocalPlayer player;

		public KiAttackRelease(LocalPlayer player) {
			this.player = player;
		}

		public LocalPlayer getPlayer() {
			return this.player;
		}
	}


	/**
	 * Fired client-side when the local player initiates a strike attack.
	 */
	public static class StrikeAttack extends Event {
		private final LocalPlayer player;
		private final int targetId;

		public StrikeAttack(LocalPlayer player, int targetId) {
			this.player = player;
			this.targetId = targetId;
		}

		public LocalPlayer getPlayer() {
			return this.player;
		}

		public int getTargetId() {
			return this.targetId;
		}
	}

	/**
	 * Fired client-side after the built-in options of an NPC dialogue are built.
	 * Listeners may add to {@link #getOptions()}.
	 */
	public static class NpcDialogueOptionsEvent extends Event {
		private final String npcId;
		private final StatsData stats;
		private final LocalPlayer player;
		private final NpcDialogueScreen screen;
		private final List<DialogueOption> options;

		public NpcDialogueOptionsEvent(String npcId, StatsData stats, LocalPlayer player, NpcDialogueScreen screen, List<DialogueOption> options) {
			this.npcId = npcId;
			this.stats = stats;
			this.player = player;
			this.screen = screen;
			this.options = options;
		}

		public String getNpcId() {
			return this.npcId;
		}

		public StatsData getStats() {
			return this.stats;
		}

		public LocalPlayer getPlayer() {
			return this.player;
		}

		public NpcDialogueScreen getScreen() {
			return this.screen;
		}

		public List<DialogueOption> getOptions() {
			return this.options;
		}
	}
}
