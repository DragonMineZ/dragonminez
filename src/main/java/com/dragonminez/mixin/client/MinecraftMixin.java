package com.dragonminez.mixin.client;

import com.dragonminez.client.collision.TargetFinder;
import com.dragonminez.client.events.DMZClientEvent;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.PlayerAttackProperties;
import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements Minecraft_DMZ {

	@Shadow public LocalPlayer player;
	@Shadow public Screen screen;
	@Shadow public HitResult hitResult;
	@Shadow protected abstract boolean startAttack();

	@Unique private AttackHand upswingStack = null;
	@Unique private int upswingTicks = 0;
	@Unique private int lastAttacked = 0;
	@Unique private int lastSwingDuration = 0;
	@Unique private List<Entity> targetsInReach = null;
	@Unique private int itemUseCooldown = 0;
	@Unique private boolean isAttacking = false;
	@Unique private boolean isAwaitingUpswing = false;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void dragonminez$startAttack(CallbackInfoReturnable<Boolean> cir) {
		if (player == null || screen != null) return;
		if (Screen.hasAltDown() && player.getMainHandItem().isEmpty()) return;

		var mcDMZ = (Minecraft_DMZ) this;
		var comboCount = mcDMZ.getComboCount();
		var hand = PlayerAttackHelper.getCurrentAttack(player, comboCount);

		if (hand == null || !PlayerAttackHelper.canAttack(player)) return;
		if (!shouldUseCombatAttack(hand)) return;


		cir.cancel();
		cir.setReturnValue(false);

		if (itemUseCooldown > 0 || isAttacking || isAwaitingUpswing) return;

		float cooldownProgress = player.getAttackStrengthScale(0.5F);
		if (ConfigManager.getCombatConfig().getRespectAttackCooldown() && cooldownProgress < 1.0F) return;

		isAttacking = true;
		isAwaitingUpswing = true;
		upswingStack = hand;

		int attackCooldownTicks = (int) Math.round(PlayerAttackHelper.getAttackCooldownTicksCapped(player));
		upswingTicks = (int) Math.round(attackCooldownTicks * hand.upswingRate());
		lastSwingDuration = attackCooldownTicks;
		lastAttacked = 0;

		((MinecraftAccessor) this).setAttackCooldown(10000);

		var event = new DMZClientEvent.PlayerAttackStart(player, hand);
		MinecraftForge.EVENT_BUS.post(event);
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void dragonminez$continueAttack(boolean leftClick, CallbackInfo ci) {
		if (!leftClick || player == null) return;

		var mcDMZ = (Minecraft_DMZ) this;
		var comboCount = mcDMZ.getComboCount();
		var hand = PlayerAttackHelper.getCurrentAttack(player, comboCount);
		boolean canUseCombat = hand != null && PlayerAttackHelper.canAttack(player) && shouldUseCombatAttack(hand);

		System.out.println("[DMZ_DEBUG] continueAttack called | combo=" + comboCount + " | hit=" + (hitResult != null ? hitResult.getType() : "null") + " | canUseCombat=" + canUseCombat + " | isAttacking=" + isAttacking + " | awaiting=" + isAwaitingUpswing);

		if (!canUseCombat) {
			return;
		}

		// In combat context, fully suppress vanilla continueAttack to avoid mining swing spam.
		ci.cancel();

		float cooldownProgress = player.getAttackStrengthScale(0.5F);
		if (cooldownProgress >= 1.0F && !isAttacking && !isAwaitingUpswing) {
			System.out.println("[DMZ_DEBUG] continueAttack triggers startAttack | cooldown=" + cooldownProgress);
			this.startAttack();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void dragonminez$tick(CallbackInfo info) {
		if (itemUseCooldown > 0) itemUseCooldown--;
		lastAttacked++;

		if (player == null) return;

		resetComboIfNeeded();

		if (upswingStack != null) {
			if (upswingTicks > 0) upswingTicks--;
			else {
				executeAttack();
				upswingStack = null;
				isAwaitingUpswing = false;
			}
		} else isAttacking = false;

		if (player.tickCount % 2 == 0) evaluateTargetsInReach();
	}

	@Unique
	private void resetComboIfNeeded() {
		int comboCount = getComboCount();
		if (comboCount <= 0) return;

		if (isAttacking || isAwaitingUpswing) return;

		int cooldownTicks = (int) Math.ceil(PlayerAttackHelper.getAttackCooldownTicksCapped(player));
		int comboResetWindow = cooldownTicks + 30;
		if (lastAttacked > comboResetWindow) ((PlayerAttackProperties) player).setComboCount(0);
	}

	@Unique
	private void executeAttack() {
		var mcDMZ = (Minecraft_DMZ) this;
		var cursorTarget = mcDMZ.getCursorTarget();
		var attackRange = upswingStack.attributes().attackRange();

		TargetFinder.TargetResult targetResult = TargetFinder.findAttackTargetResult(player, cursorTarget, upswingStack.attack(), attackRange);

		var event = new DMZClientEvent.PlayerAttackHit(player, upswingStack, targetResult.entities, cursorTarget);
		MinecraftForge.EVENT_BUS.post(event);

		int[] entityIds = targetResult.entities.stream().mapToInt(Entity::getId).toArray();

		int comboCount = mcDMZ.getComboCount();
		boolean sneaking = player.isShiftKeyDown();
		int slot = player.getInventory().selected;

		NetworkHandler.sendToServer(new CombatAttackRequestC2S(comboCount, sneaking, slot, entityIds));

		int nextComboCount = comboCount + 1;
		((PlayerAttackProperties) player).setComboCount(nextComboCount);

		player.resetAttackStrengthTicker();
		setMiningCooldown((int) Math.round(PlayerAttackHelper.getAttackCooldownTicksCapped(player)));
	}

	@Unique
	private void evaluateTargetsInReach() {
		var mcDMZ = (Minecraft_DMZ) this;
		var comboCount = mcDMZ.getComboCount();
		var hand = PlayerAttackHelper.getCurrentAttack(player, comboCount);
		if (hand == null) {
			targetsInReach = null;
			return;
		}
		var cursorTarget = mcDMZ.getCursorTarget();
		var attackRange = hand.attributes().attackRange();
		TargetFinder.TargetResult targetResult = TargetFinder.findAttackTargetResult(player, cursorTarget, hand.attack(), attackRange);
		targetsInReach = targetResult.entities;
	}

	@Unique
	private void setMiningCooldown(int ticks) {
		((MinecraftAccessor) this).setAttackCooldown(ticks);
	}

	@Unique
	private boolean hasTargetsForAttack(AttackHand hand) {
		if (hasTargetsInReach()) return true;
		var mcDMZ = (Minecraft_DMZ) this;
		var cursorTarget = mcDMZ.getCursorTarget();
		var attackRange = hand.attributes().attackRange();
		TargetFinder.TargetResult targetResult = TargetFinder.findAttackTargetResult(player, cursorTarget, hand.attack(), attackRange);
		return !targetResult.entities.isEmpty();
	}

	@Unique
	private boolean shouldUseCombatAttack(AttackHand hand) {
		return hitResult == null || hitResult.getType() != HitResult.Type.BLOCK || hasTargetsForAttack(hand);
	}

	@Override
	public int getComboCount() {
		return player != null ? ((PlayerAttackProperties) player).getComboCount() : 0;
	}

	@Override
	public boolean hasTargetsInReach() {
		return targetsInReach != null && !targetsInReach.isEmpty();
	}

	@Override
	public float getSwingProgress() {
		if (lastAttacked > lastSwingDuration || lastSwingDuration <= 0) return 1F;
		return (float) lastAttacked / lastSwingDuration;
	}

	@Override
	public int getUpswingTicks() {
		return upswingTicks;
	}

	@Override
	public void cancelUpswing() {
		upswingStack = null;
		itemUseCooldown = 0;
		setMiningCooldown(0);
		isAwaitingUpswing = false;
		isAttacking = false;
	}
}
