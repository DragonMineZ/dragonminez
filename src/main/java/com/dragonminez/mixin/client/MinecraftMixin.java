package com.dragonminez.mixin.client;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.collision.CollisionHelper;
import com.dragonminez.client.collision.TargetFinder;
import com.dragonminez.client.events.DMZClientEvent;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.PlayerAttackProperties;
import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import com.dragonminez.common.combat.util.SoundHelper;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
	@Unique private boolean queuedAttack = false;
	@Unique private int queuedAttackTicks = 0;

	@Unique private static final float ATTACK_QUEUE_WINDOW_TICKS = 1.0F;
	@Unique private static final int ATTACK_QUEUE_EXPIRY_TICKS = 4;

	@Unique private static final float UPSWING_IMPACT_BIAS = 0.4F;
	@Unique private static final int BLOCK_MINE_ATTACK_GRACE = 5;
	@Unique private int lastBlockMineTick = -100;

	@Unique private static final double BLOCK_MINE_TARGET_BIAS = 0.25D;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void dragonminez$startAttack(CallbackInfoReturnable<Boolean> cir) {
		if (player == null || screen != null) return;

		boolean[] isDmzBlocking = {false};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> isDmzBlocking[0] = data.getStatus().isBlocking());
		if (player.isBlocking() || isDmzBlocking[0] || PlayerAttackHelper.isChargingTechnique(player)) {
			cir.cancel();
			cir.setReturnValue(false);
			return;
		}

		var mcDMZ = (Minecraft_DMZ) this;
		var comboCount = mcDMZ.getComboCount();
		var hand = PlayerAttackHelper.getCurrentAttack(player, comboCount);

		if (hand == null || !PlayerAttackHelper.canAttack(player)) return;
		if (!shouldUseCombatAttack(hand)) return;

		cir.cancel();
		cir.setReturnValue(false);

		if (itemUseCooldown > 0 || isAttacking || isAwaitingUpswing) return;

		float cooldownProgress = player.getAttackStrengthScale(0.5F);
		if (cooldownProgress < 1.0F) {
			float remainingTicks = (1.0F - cooldownProgress) * player.getCurrentItemAttackStrengthDelay();
			if (remainingTicks <= ATTACK_QUEUE_WINDOW_TICKS) {
				queuedAttack = true;
				queuedAttackTicks = 0;
			}
			return;
		}
		queuedAttack = false;

		isAttacking = true;
		isAwaitingUpswing = true;
		upswingStack = hand;

		float cooldownTicks = PlayerAttackHelper.getAttackCooldownTicksCapped(player);
		int swingAnimTicks = meleeAnimTicks(meleeAnimSpeed(cooldownTicks));
		upswingTicks = Math.max(1, Math.round(swingAnimTicks * (float) hand.upswingRate() * UPSWING_IMPACT_BIAS));
		lastSwingDuration = swingAnimTicks;
		lastAttacked = 0;

		((MinecraftAccessor) this).setAttackCooldown(10000);

		var event = new DMZClientEvent.PlayerAttackStart(player, hand);
		MinecraftForge.EVENT_BUS.post(event);

		playLocalAttackFeedback(hand);
	}

	@Unique
	private float meleeAnimSpeed(float cooldownTicks) {
		float speed = 12.0F / Math.max(cooldownTicks, 0.001F);
		return Math.max(0.55F, Math.min(1.35F, speed));
	}

	@Unique
	private int meleeAnimTicks(float animSpeed) {
		return Math.max(8, Math.round(12.0F / Math.max(animSpeed, 0.1F)));
	}

	@Unique
	private void playLocalAttackFeedback(AttackHand hand) {
		if (hand.attack() == null) return;

		float animSpeedMultiplier = meleeAnimSpeed(PlayerAttackHelper.getAttackCooldownTicksCapped(player));

		((IPlayerAnimatable) player).dragonminez$playMeleeAnimation(hand.attack().animation(), hand.isOffHand(), animSpeedMultiplier);

		var swingSound = hand.attack().swingSound();
		SoundEvent soundEvent = SoundHelper.resolveSoundEvent(swingSound);
		if (soundEvent != null) {
			player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), soundEvent, SoundSource.PLAYERS, swingSound.volume(), SoundHelper.computePitch(swingSound), false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void dragonminez$continueAttack(boolean leftClick, CallbackInfo ci) {
		if (!leftClick || player == null) return;

		if (PlayerAttackHelper.isChargingTechnique(player)) {
			ci.cancel();
			return;
		}

		var mcDMZ = (Minecraft_DMZ) this;
		var comboCount = mcDMZ.getComboCount();
		var hand = PlayerAttackHelper.getCurrentAttack(player, comboCount);
		boolean canUseCombat = hand != null && PlayerAttackHelper.canAttack(player) && shouldUseCombatAttack(hand);

		if (!canUseCombat) {
			if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) lastBlockMineTick = player.tickCount;
			return;
		}
		ci.cancel();

		if (player.tickCount - lastBlockMineTick <= BLOCK_MINE_ATTACK_GRACE) return;

		float cooldownProgress = player.getAttackStrengthScale(0.5F);
		if (cooldownProgress >= 1.0F && !isAttacking && !isAwaitingUpswing) this.startAttack();
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

		fireQueuedAttackIfReady();

		if (player.tickCount % 2 == 0) evaluateTargetsInReach();
	}

	@Unique
	private void fireQueuedAttackIfReady() {
		if (!queuedAttack) return;
		if (isAttacking || isAwaitingUpswing) {
			queuedAttack = false;
			return;
		}
		if (++queuedAttackTicks > ATTACK_QUEUE_EXPIRY_TICKS) {
			queuedAttack = false;
			return;
		}
		if (screen != null || player.getAttackStrengthScale(0.5F) < 1.0F) return;
		queuedAttack = false;
		this.startAttack();
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
		var attackRange = PlayerAttackHelper.getEffectiveAttackRange(player, upswingStack.attributes().attackRange());

		TargetFinder.TargetResult targetResult = TargetFinder.findAttackTargetResult(player, cursorTarget, upswingStack.attack(), attackRange);

		var event = new DMZClientEvent.PlayerAttackHit(player, upswingStack, targetResult.entities, cursorTarget);
		MinecraftForge.EVENT_BUS.post(event);

		int[] entityIds = targetResult.entities.stream().mapToInt(Entity::getId).toArray();

		int comboCount = mcDMZ.getComboCount();
		boolean sneaking = player.hasPose(Pose.CROUCHING);
		int slot = player.getInventory().selected;

		NetworkHandler.sendToServer(new CombatAttackRequestC2S(comboCount, sneaking, slot, entityIds));

		int nextComboCount = comboCount + 1;
		((PlayerAttackProperties) player).setComboCount(nextComboCount);

		player.resetAttackStrengthTicker();
		setMiningCooldown(Math.max(2, Math.round(PlayerAttackHelper.getAttackCooldownTicksCapped(player))));
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
		var attackRange = PlayerAttackHelper.getEffectiveAttackRange(player, hand.attributes().attackRange());
		TargetFinder.TargetResult targetResult = TargetFinder.findAttackTargetResult(player, cursorTarget, hand.attack(), attackRange);
		targetsInReach = targetResult.entities;
	}

	@Unique
	private void setMiningCooldown(int ticks) {
		((MinecraftAccessor) this).setAttackCooldown(ticks);
	}

	@Unique
	private List<Entity> collectAttackTargets(AttackHand hand) {
		if (hasTargetsInReach()) return targetsInReach;
		var mcDMZ = (Minecraft_DMZ) this;
		var cursorTarget = mcDMZ.getCursorTarget();
		var attackRange = PlayerAttackHelper.getEffectiveAttackRange(player, hand.attributes().attackRange());
		return TargetFinder.findAttackTargetResult(player, cursorTarget, hand.attack(), attackRange).entities;
	}

	@Unique
	private boolean hasTargetsForAttack(AttackHand hand) {
		return !collectAttackTargets(hand).isEmpty();
	}

	@Unique
	private boolean hasTargetInFrontOfBlock(AttackHand hand) {
		List<Entity> targets = collectAttackTargets(hand);
		if (targets.isEmpty()) return false;

		Vec3 eye = player.getEyePosition();
		double blockDistance = hitResult.getLocation().distanceTo(eye);
		for (Entity target : targets) {
			if (CollisionHelper.distance(eye, target.getBoundingBox()) + BLOCK_MINE_TARGET_BIAS < blockDistance) return true;
		}
		return false;
	}

	@Unique
	private boolean shouldUseCombatAttack(AttackHand hand) {
		if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return true;
		return hasTargetInFrontOfBlock(hand);
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
		queuedAttack = false;
		itemUseCooldown = 0;
		setMiningCooldown(0);
		isAwaitingUpswing = false;
		isAttacking = false;
	}
}
