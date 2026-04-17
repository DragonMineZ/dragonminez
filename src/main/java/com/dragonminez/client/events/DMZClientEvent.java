package com.dragonminez.client.events;

import com.dragonminez.common.combat.player.AttackHand;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;
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

		public LocalPlayer getPlayer() { return player; }
		public AttackHand getAttackHand() { return attackHand; }
	}

	/**
	 * Fired when player hits some targets (can be zero or more targets).
	 */
	public static class PlayerAttackHit extends Event {
		private final LocalPlayer player;
		private final AttackHand attackHand;
		private final List<Entity> targets;
		@Nullable private final Entity cursorTarget;

		public PlayerAttackHit(LocalPlayer player, AttackHand attackHand, List<Entity> targets, @Nullable Entity cursorTarget) {
			this.player = player;
			this.attackHand = attackHand;
			this.targets = targets;
			this.cursorTarget = cursorTarget;
		}

		public LocalPlayer getPlayer() { return player; }
		public AttackHand getAttackHand() { return attackHand; }
		public List<Entity> getTargets() { return targets; }
		@Nullable public Entity getCursorTarget() { return cursorTarget; }
	}
}