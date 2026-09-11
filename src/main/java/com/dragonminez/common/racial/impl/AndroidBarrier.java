package com.dragonminez.common.racial.impl;

import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.server.events.players.KiSurgeService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class AndroidBarrier {
	private static final int BARRIER_COLOR_MAIN = 0x6FE7FF;
	private static final int BARRIER_COLOR_BORDER = 0x1A5FA8;
	private static final int BARRIER_COLOR_OUTLINE = 0xFFFFFF;

	private static final float VISUAL_BARRIER_HP = 1.0F;

	private AndroidBarrier() {}

	public static boolean isActive(StatsData data) {
		return data.getRacialData().isAndroidBarrierActive();
	}

	public static boolean isActive(Player player) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		return data != null && isActive(data);
	}

	public static boolean canHold(RacialContext ctx) {
		StatsData data = ctx.data();
		if (!data.getStatus().isAndroidUpgraded()) return false;
		if (!ctx.config().getHuman().getAndroidBarrierEnabled()) return false;
		if (isActive(data)) return true;
		return !data.getCooldowns().hasCooldown(Cooldowns.ANDROID_BARRIER_CD);
	}

	public static void tick(RacialContext ctx) {
		StatsData data = ctx.data();
		RacialData racial = data.getRacialData();

		boolean holding = data.getStatus().isAndroidUpgraded()
				&& data.getStatus().isActionCharging()
				&& data.getStatus().getSelectedAction() == ActionMode.RACIAL;

		if (!racial.isAndroidBarrierActive()) {
			if (holding && canHold(ctx)) start(ctx);
			return;
		}

		int maxTicks = Math.max(1, ctx.config().getHuman().getAndroidBarrierMaxSeconds() * 20);
		racial.setAndroidBarrierTicks(racial.getAndroidBarrierTicks() + 1);

		if (!holding || racial.getAndroidBarrierTicks() >= maxTicks || data.getStatus().isStunned()) {
			end(ctx, false);
			return;
		}

		keepEntityAlive(ctx);
	}

	public static Double tryAbsorb(RacialContext ctx, double rawDamage, DamageSource source) {
		if (!isActive(ctx.data())) return null;
		if (!isRangedKiAttack(source)) return null;
		if (rawDamage <= 0.0) return 0.0;

		absorb(ctx, rawDamage);
		return 0.0;
	}

	private static boolean isRangedKiAttack(DamageSource source) {
		if (!MainDamageTypes.isKiblastDamage(source)) return false;
		return source.getDirectEntity() instanceof AbstractKiProjectile;
	}

	private static void absorb(RacialContext ctx, double rawDamage) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		GeneralServerConfig.HumanRacialConfig config = ctx.config().getHuman();

		float maxEnergy = data.getMaxEnergy();
		float gained = (float) (rawDamage * config.getAndroidBarrierKiConversion());
		float total = data.getResources().getCurrentEnergy() + gained;

		data.getResources().setCurrentEnergy(Math.min(total, maxEnergy));

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				MainSounds.KI_CHARGE_LOOP.get(), SoundSource.PLAYERS, 0.7F, 1.4F);

		double overflowRatio = maxEnergy > 0 ? (total - maxEnergy) / maxEnergy : 0.0;
		double breakThreshold = config.getAndroidBarrierBreakOverflow();

		if (overflowRatio >= config.getAndroidBarrierSurgeOverflow()) {
			KiSurgeService.forceActivateSurge(player, data);
		}

		if (overflowRatio > breakThreshold) {
			float backlash = (float) ((overflowRatio - breakThreshold) * maxEnergy);
			end(ctx, true);
			player.displayClientMessage(
					Component.translatable("message.dragonminez.racial.human.barrier_overloaded"), true);
			if (backlash > 0.0f) player.hurt(MainDamageTypes.kiblast(player.level(), player, player), backlash);
		}

		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	private static void start(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		RacialData racial = ctx.data().getRacialData();

		racial.setAndroidBarrierActive(true);
		racial.setAndroidBarrierTicks(0);
		spawnEntity(ctx);

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				MainSounds.AURA_START.get(), SoundSource.PLAYERS, 0.9F, 1.5F);
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	public static void forceEnd(RacialContext ctx, boolean broken) {
		if (!isActive(ctx.data())) return;
		end(ctx, broken);
	}

	private static void end(RacialContext ctx, boolean broken) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		RacialData racial = data.getRacialData();
		GeneralServerConfig.HumanRacialConfig config = ctx.config().getHuman();

		racial.setAndroidBarrierActive(false);
		racial.setAndroidBarrierTicks(0);
		discardEntity(ctx);

		data.getStatus().setActionCharging(false);
		data.getResources().setActionCharge(0);

		int seconds = broken ? config.getAndroidBarrierBrokenCooldownSeconds() : config.getAndroidBarrierCooldownSeconds();
		data.getCooldowns().setCooldown(Cooldowns.ANDROID_BARRIER_CD, Math.max(1, seconds * 20));

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				broken ? MainSounds.KI_EXPLOSION_IMPACT.get() : MainSounds.SWITCH_OFF.get(),
				SoundSource.PLAYERS, broken ? 1.6F : 0.7F, broken ? 0.7F : 1.2F);

		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	private static void spawnEntity(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		KiBarrierEntity barrier = new KiBarrierEntity(MainEntities.KI_BARRIER.get(), player.level());
		barrier.setPos(player.getX(), player.getY(), player.getZ());
		barrier.setOwner(player);
		barrier.setupBarrierPlayer(player, VISUAL_BARRIER_HP, ctx.config().getHuman().getAndroidBarrierSize().floatValue(), BARRIER_COLOR_MAIN, BARRIER_COLOR_BORDER, BARRIER_COLOR_OUTLINE);
		barrier.setDecorative(true);
		barrier.fireHability(Math.max(1, ctx.config().getHuman().getAndroidBarrierMaxSeconds() * 20));
		player.level().addFreshEntity(barrier);

		ctx.data().getRacialData().setAndroidBarrierEntityId(barrier.getId());
	}

	private static void keepEntityAlive(RacialContext ctx) {
		KiBarrierEntity barrier = findEntity(ctx);
		if (barrier == null) {
			spawnEntity(ctx);
			return;
		}
		ServerPlayer player = ctx.player();
		barrier.setPos(player.getX(), player.getY(), player.getZ());
	}

	private static void discardEntity(RacialContext ctx) {
		KiBarrierEntity barrier = findEntity(ctx);
		if (barrier != null) barrier.discard();
		ctx.data().getRacialData().setAndroidBarrierEntityId(-1);
	}

	private static KiBarrierEntity findEntity(RacialContext ctx) {
		int id = ctx.data().getRacialData().getAndroidBarrierEntityId();
		if (id < 0) return null;
		Entity entity = ctx.player().level().getEntity(id);
		return entity instanceof KiBarrierEntity barrier && barrier.isAlive() ? barrier : null;
	}
}
