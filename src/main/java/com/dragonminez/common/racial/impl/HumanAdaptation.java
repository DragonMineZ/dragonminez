package com.dragonminez.common.racial.impl;

import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class HumanAdaptation implements RacialAbility {
	private static final UUID ADRENALINE_ATTACK_SPEED_UUID = UUID.fromString("a1e1a4a0-3b3e-4a1e-9b1d-adde5a1177ac");
	private static final UUID ADRENALINE_MOVE_SPEED_UUID = UUID.fromString("a1e1a4a0-3b3e-4a1e-9b1d-adde5a1177ad");

	@Override
	public String id() {
		return "human";
	}

	@Override
	public boolean hasActiveAction() {
		return false;
	}

	@Override
	public boolean hasActiveAction(StatsData data) {
		return data.getStatus().isAndroidUpgraded();
	}

	@Override
	public boolean isSustainedAction(RacialContext ctx) {
		return ctx.data().getStatus().isAndroidUpgraded();
	}

	@Override
	public boolean canActivate(RacialContext ctx) {
		return AndroidBarrier.canHold(ctx);
	}

	@Override
	public double modifyDamageTaken(RacialContext ctx, double postMitigationDamage, double rawDamage, DamageSource source) {
		Double absorbed = AndroidBarrier.tryAbsorb(ctx, rawDamage, source);
		if (absorbed != null) return absorbed;
		return modifyDamageTaken(ctx, postMitigationDamage, source);
	}

	@Override
	public double modifyTechniqueXpGain(RacialContext ctx, double amount) {
		return amount * (1.0 + ctx.config().getHuman().getTechniqueXpBonus());
	}

	@Override
	public double modifyDamageTaken(RacialContext ctx, double postMitigationDamage, DamageSource source) {
		if (!ctx.data().getCooldowns().hasCooldown(Cooldowns.ADRENALINE_ACTIVE)) return postMitigationDamage;
		double reduction = ctx.config().getHuman().getAdrenalineDamageReduction();
		return postMitigationDamage * (1.0 - reduction);
	}

	@Override
	public void onTick(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		GeneralServerConfig.HumanRacialConfig config = ctx.config().getHuman();
		RacialData racialData = data.getRacialData();

		float ratio = player.getHealth() / player.getMaxHealth();
		boolean belowThreshold = ratio <= config.getAdrenalineThreshold();

		if (!belowThreshold) {
			racialData.setAdrenalineArmed(true);
		} else if (racialData.isAdrenalineArmed()) {
			racialData.setAdrenalineArmed(false);
			if (config.getEnabled() && !data.getCooldowns().hasCooldown(Cooldowns.ADRENALINE)) {
				triggerAdrenaline(player, data, config);
			}
		}

		if (!data.getCooldowns().hasCooldown(Cooldowns.ADRENALINE_ACTIVE)) {
			removeModifiers(player);
		}

		AndroidBarrier.tick(ctx);
	}

	@Override
	public void onDeath(RacialContext ctx) {
		removeModifiers(ctx.player());
		AndroidBarrier.forceEnd(ctx, false);
	}

	@Override
	public void onRespawn(RacialContext ctx) {
		removeModifiers(ctx.player());
		AndroidBarrier.forceEnd(ctx, false);
	}

	@Override
	public void onDimensionChange(RacialContext ctx) {
		removeModifiers(ctx.player());
		AndroidBarrier.forceEnd(ctx, false);
	}

	@Override
	public void onLogin(RacialContext ctx) {
		removeModifiers(ctx.player());
		AndroidBarrier.forceEnd(ctx, false);
	}

	private static void triggerAdrenaline(ServerPlayer player, StatsData data, GeneralServerConfig.HumanRacialConfig config) {
		data.getCooldowns().setCooldown(Cooldowns.ADRENALINE, config.getAdrenalineCooldownSeconds() * 20);
		data.getCooldowns().setCooldown(Cooldowns.ADRENALINE_ACTIVE, config.getAdrenalineSeconds() * 20);

		applyModifier(player, Attributes.ATTACK_SPEED, ADRENALINE_ATTACK_SPEED_UUID, "Adrenaline Attack Speed", config.getAdrenalineAttackSpeed());
		applyModifier(player, Attributes.MOVEMENT_SPEED, ADRENALINE_MOVE_SPEED_UUID, "Adrenaline Move Speed", config.getAdrenalineMoveSpeed());

		player.addEffect(new MobEffectInstance(
				MainEffects.ADRENALINE.get(), config.getAdrenalineSeconds() * 20, 0, false, false, true));

		player.displayClientMessage(Component.translatable("message.dragonminez.racial.human.adrenaline_triggered"), true);
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	private static void applyModifier(ServerPlayer player, Attribute attribute, UUID uuid, String name, double amount) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) return;
		if (instance.getModifier(uuid) != null) instance.removeModifier(uuid);
		instance.addTransientModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	private static void removeModifiers(ServerPlayer player) {
		AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
		if (attackSpeed != null && attackSpeed.getModifier(ADRENALINE_ATTACK_SPEED_UUID) != null) attackSpeed.removeModifier(ADRENALINE_ATTACK_SPEED_UUID);

		AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (moveSpeed != null && moveSpeed.getModifier(ADRENALINE_MOVE_SPEED_UUID) != null) moveSpeed.removeModifier(ADRENALINE_MOVE_SPEED_UUID);
	}
}
