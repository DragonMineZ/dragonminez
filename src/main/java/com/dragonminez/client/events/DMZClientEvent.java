package com.dragonminez.client.events;

import com.dragonminez.common.combat.player.AttackHand;
import lombok.Getter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DMZClientEvent {

	/**
	 * Fired when player starts the upswing (aka windup).
	 */
	@Getter
	public static class PlayerAttackStart extends Event {
		private final LocalPlayer player;
		private final AttackHand attackHand;

		public PlayerAttackStart(LocalPlayer player, AttackHand attackHand) {
			this.player = player;
			this.attackHand = attackHand;
		}

	}

	/**
	 * Fired when player hits some targets (can be zero or more targets).
	 */
	@Getter
	public static class PlayerAttackHit extends Event {
		@Getter
		private final LocalPlayer player;
		@Getter
		private final AttackHand attackHand;
		@Getter
		private final List<Entity> targets;
		@Nullable private final Entity cursorTarget;

		public PlayerAttackHit(LocalPlayer player, AttackHand attackHand, List<Entity> targets, @Nullable Entity cursorTarget) {
			this.player = player;
			this.attackHand = attackHand;
			this.targets = targets;
			this.cursorTarget = cursorTarget;
		}

		@Nullable public Entity getCursorTarget() { return cursorTarget; }
	}

	/**
	 * Fired client-side when the local player begins charging (casts) a Ki attack from a slot.
	 */
	@Getter
	public static class KiAttackCast extends Event {
		private final LocalPlayer player;
		private final int slot;

		public KiAttackCast(LocalPlayer player, int slot) {
			this.player = player;
			this.slot = slot;
		}

	}

	/** Fired client-side when the local player releases a charged Ki attack. */
	@Getter
	public static class KiAttackRelease extends Event {
		private final LocalPlayer player;

		public KiAttackRelease(LocalPlayer player) {
			this.player = player;
		}

	}

	/** Fired client-side when the local player initiates a strike attack. */
	@Getter
	public static class StrikeAttack extends Event {
		private final LocalPlayer player;
		private final int targetId;

		public StrikeAttack(LocalPlayer player, int targetId) {
			this.player = player;
			this.targetId = targetId;
		}

	}
}