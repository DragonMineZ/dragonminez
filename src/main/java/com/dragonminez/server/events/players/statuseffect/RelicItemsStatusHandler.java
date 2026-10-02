package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.CuriosUtil;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import top.theillusivec4.curios.api.CuriosApi;

public class RelicItemsStatusHandler implements IStatusEffectHandler {
	public static final String SACRED_WATER_BONUS = "Sacred_Water";
	public static final String DEMON_EYE_BONUS = "Demon_Eye";
	private static final String[] BONUS_STATS = {"STR", "SKP", "DEF", "STM", "VIT", "PWR", "ENE"};
	private static final String BACK_HITS_KEY = "dmz_demon_eye_back_hits";
	private static final String BACK_HIT_TICK_KEY = "dmz_demon_eye_back_hit_tick";
	private static final double BEHIND_DOT = -0.5;

	@Override
	public void handleStatusEffects(ServerPlayer player, StatsData data) {
		var gameplay = ConfigManager.getServerConfig().getGameplay();
		boolean changed = false;

		if (hasStatMultiplier(data, SACRED_WATER_BONUS)) {
			changed |= setStatMultiplier(data, SACRED_WATER_BONUS, gameplay.getSacredWaterMultiplier());
		}

		if (isWearingDemonEye(player)) {
			changed |= setStatMultiplier(data, DEMON_EYE_BONUS, gameplay.getDemonEyeMultiplier());
		} else if (hasStatMultiplier(data, DEMON_EYE_BONUS)) {
			data.getBonusStats().removeAllBonuses(DEMON_EYE_BONUS);
			changed = true;
		}

		if (changed) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	@Override
	public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {
	}

	@Override
	public void onPlayerSecond(ServerPlayer player, StatsData data) {
		boolean wearing = isWearingDemonEye(player);
		updateDemonEyeGrowth(player, data, wearing);
		if (!wearing || player.isCreative() || player.isSpectator()) return;
		var gameplay = ConfigManager.getServerConfig().getGameplay();

		float kiCost = (float) (data.getMaxEnergy() * gameplay.getDemonEyeKiDrainPercent());
		float energy = data.getResources().getCurrentEnergy();
		if (energy >= kiCost) {
			data.getResources().setCurrentEnergy(energy - kiCost);
		} else {
			data.getResources().setCurrentEnergy(0);
			float healthCost = (float) (player.getMaxHealth() * gameplay.getDemonEyeHealthDrainPercent());
			if (healthCost > 0) player.setHealth(Math.max(1.0F, player.getHealth() - healthCost));
		}

		int interval = gameplay.getDemonEyeAlignmentLossIntervalSeconds();
		if (interval > 0 && data.getResources().getAlignment() > 0 && (player.level().getGameTime() / 20) % interval == 0) {
			data.getResources().removeAlignment(1);
		}

		NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
	}

	private static void updateDemonEyeGrowth(ServerPlayer player, StatsData data, boolean wearing) {
		var gameplay = ConfigManager.getServerConfig().getGameplay();
		float maxGrowth = 1.0f + gameplay.getDemonEyeMaxGrowth().floatValue();
		float current = data.getCharacter().getDemonEyeGrowth();
		float next;
		if (wearing) {
			int seconds = gameplay.getDemonEyeGrowthSeconds();
			float step = seconds > 0 ? (maxGrowth - 1.0f) / seconds : maxGrowth;
			next = Math.min(maxGrowth, current + step);
		} else {
			if (current <= 1.0f) return;
			int seconds = gameplay.getDemonEyeShrinkSeconds();
			float step = seconds > 0 ? (Math.max(maxGrowth, current) - 1.0f) / seconds : current;
			next = Math.max(1.0f, current - step);
		}
		if (Math.abs(next - current) < 1.0E-6f) return;

		data.getCharacter().setDemonEyeGrowth(next);
		player.refreshDimensions();
	}

	public static void onFinalDamage(LivingDamageEvent event) {
		if (event.isCanceled() || event.getAmount() <= 0.0f) return;
		if (!(event.getEntity() instanceof ServerPlayer victim)) return;
		DamageSource source = event.getSource();
		if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == victim) return;
		if (MainDamageTypes.isKiblastDamage(source)) return;
		if (source.getDirectEntity() != null && source.getDirectEntity() != attacker) return;

		int required = ConfigManager.getServerConfig().getGameplay().getDemonEyeBackHitsToRemove();
		if (required <= 0 || !isWearingDemonEye(victim) || !isBehind(victim, attacker)) return;

		CompoundTag persistent = victim.getPersistentData();
		long now = victim.level().getGameTime();
		long window = ConfigManager.getServerConfig().getGameplay().getDemonEyeBackHitWindowSeconds() * 20L;
		int hits = now - persistent.getLong(BACK_HIT_TICK_KEY) <= window ? persistent.getInt(BACK_HITS_KEY) + 1 : 1;

		if (hits < required) {
			persistent.putInt(BACK_HITS_KEY, hits);
			persistent.putLong(BACK_HIT_TICK_KEY, now);
			victim.displayClientMessage(Component.translatable("item.dragonminez.demon_eye.back_hit", hits, required)
					.withStyle(ChatFormatting.RED), true);
			return;
		}

		persistent.remove(BACK_HITS_KEY);
		persistent.remove(BACK_HIT_TICK_KEY);
		if (!knockOffDemonEye(victim)) return;

		victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.2F, 0.6F);
		victim.displayClientMessage(Component.translatable("item.dragonminez.demon_eye.knocked_off")
				.withStyle(ChatFormatting.DARK_RED), true);
		if (attacker instanceof ServerPlayer attackerPlayer) {
			attackerPlayer.displayClientMessage(Component.translatable("item.dragonminez.demon_eye.knocked_off_attacker", victim.getDisplayName())
					.withStyle(ChatFormatting.GOLD), true);
		}
	}

	private static boolean isBehind(ServerPlayer victim, LivingEntity attacker) {
		Vec3 look = victim.getViewVector(1.0F);
		Vec3 facing = new Vec3(look.x, 0.0, look.z);
		Vec3 toAttacker = new Vec3(attacker.getX() - victim.getX(), 0.0, attacker.getZ() - victim.getZ());
		if (facing.lengthSqr() < 1.0E-6 || toAttacker.lengthSqr() < 1.0E-6) return false;
		return facing.normalize().dot(toAttacker.normalize()) < BEHIND_DOT;
	}

	private static boolean knockOffDemonEye(ServerPlayer player) {
		var inventory = CuriosApi.getCuriosInventory(player).resolve().orElse(null);
		if (inventory == null) return false;
		var handler = inventory.getCurios().get("head_tech");
		if (handler == null) return false;

		for (int i = 0; i < handler.getStacks().getSlots(); i++) {
			ItemStack stack = handler.getStacks().getStackInSlot(i);
			if (!stack.is(MainItems.DEMON_EYE.get())) continue;
			ItemStack dropped = stack.copy();
			handler.getStacks().setStackInSlot(i, ItemStack.EMPTY);
			player.drop(dropped, true, false);
			return true;
		}
		return false;
	}

	public static boolean isDemonEyeActive(StatsData data) {
		return hasStatMultiplier(data, DEMON_EYE_BONUS);
	}

	public static boolean isWearingDemonEye(ServerPlayer player) {
		return !CuriosUtil.getFirstStackForItem(player, "head_tech", "demon_eye").isEmpty();
	}

	public static boolean hasStatMultiplier(StatsData data, String bonusName) {
		return data.getBonusStats().hasBonus("STR", bonusName);
	}

	public static boolean setStatMultiplier(StatsData data, String bonusName, double multiplier) {
		boolean upToDate = true;
		for (String stat : BONUS_STATS) {
			upToDate &= data.getBonusStats().getBonuses(stat).stream()
					.anyMatch(bonus -> bonus.name.equals(bonusName) && bonus.operation.equals("*")
							&& Math.abs(bonus.value - multiplier) < 1.0E-6);
		}
		if (upToDate) return false;

		for (String stat : BONUS_STATS) data.getBonusStats().addBonus(stat, bonusName, "*", multiplier, true);
		return true;
	}
}
