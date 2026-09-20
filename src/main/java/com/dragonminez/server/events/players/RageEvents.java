package com.dragonminez.server.events.players;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.util.MutantManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RageEvents {
	private enum AttackCategory { MELEE, STRIKE, KI, OTHER }

	private static final class CombatState {
		private long lastCombatMs;
		private long lastHitMs;
		private AttackCategory lastCategory;
		private int varietySteps;
	}

	private static final Map<UUID, CombatState> STATES = new HashMap<>();
	private static final int REVERT_GUARD = 6;

	private static GeneralServerConfig.RageConfig config() {
		GeneralServerConfig server = ConfigManager.getServerConfig();
		return server != null && server.getMutant() != null ? server.getMutant().getRage() : null;
	}

	public static void onFinalDamage(LivingDamageEvent event) {
		if (event.isCanceled() || event.getAmount() <= 0.0f) return;
		LivingEntity victim = event.getEntity();
		if (victim.level().isClientSide) return;
		GeneralServerConfig.RageConfig cfg = config();
		if (cfg == null) return;

		float amount = Math.min(event.getAmount(), victim.getHealth());
		DamageSource source = event.getSource();

		if (victim instanceof ServerPlayer hurt) {
			withMutant(hurt, data -> {
				float fraction = amount / Math.max(1.0f, hurt.getMaxHealth());
				if (fraction < cfg.getSignificantDamageTakenRatio()) return;
				state(hurt).lastCombatMs = System.currentTimeMillis();
				addRage(data, cfg, fraction * cfg.getGainPerReceivedHealthFraction());
			});
		}

		if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
			withMutant(attacker, data -> {
				if (!isWorthyTarget(attacker, victim, event.getAmount(), cfg)) return;
				CombatState state = state(attacker);
				long now = System.currentTimeMillis();
				state.lastCombatMs = now;

				AttackCategory category = categorize(source, attacker);
				if (now - state.lastHitMs > cfg.getVarietyWindowSeconds() * 1000.0) state.varietySteps = 0;
				else if (state.lastCategory != null && state.lastCategory != category) state.varietySteps = Math.min(cfg.getVarietyMaxSteps(), state.varietySteps + 1);
				else state.varietySteps = Math.max(0, state.varietySteps - 1);
				state.lastCategory = category;
				state.lastHitMs = now;

				double variety = 1.0 + cfg.getVarietyBonusPerStep() * state.varietySteps;
				float fraction = amount / Math.max(1.0f, victim.getMaxHealth());
				addRage(data, cfg, fraction * cfg.getGainPerDealtHealthFraction() * variety);
			});
		}
	}

	private static boolean isWorthyTarget(ServerPlayer attacker, LivingEntity victim, float damage, GeneralServerConfig.RageConfig cfg) {
		if (victim instanceof Player) return true;
		if (victim.getMaxHealth() < attacker.getMaxHealth() * cfg.getSignificantTargetHealthRatio()) return false;
		boolean oneShot = victim.getHealth() >= victim.getMaxHealth() * 0.99f && damage >= victim.getHealth();
		return !oneShot;
	}

	private static AttackCategory categorize(DamageSource source, ServerPlayer attacker) {
		if (MainDamageTypes.isStrikeAttackDamage(source)) return AttackCategory.STRIKE;
		if (MainDamageTypes.isKiblastDamage(source)) return AttackCategory.KI;
		if (source.getDirectEntity() == null || source.getDirectEntity() == attacker) return AttackCategory.MELEE;
		return AttackCategory.OTHER;
	}

	private static void addRage(StatsData data, GeneralServerConfig.RageConfig cfg, double gain) {
		if (gain <= 0.0) return;
		if (data.getStatus().isRageActive()) gain *= cfg.getActiveGainMultiplier();
		gain = Math.min(gain, cfg.getMaxGainPerHit());
		data.getResources().setRage((float) (data.getResources().getRage() + gain));
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
		GeneralServerConfig.RageConfig cfg = config();
		if (cfg == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			var resources = data.getResources();
			var status = data.getStatus();

			if (!MutantManager.isMutant(data)) {
				if (resources.getRage() > 0.0f || status.isRageActive()) {
					resources.setRage(0.0f);
					status.setRageActive(false);
					sync(player);
				}
				return;
			}

			String group = data.getCharacter().getActiveFormGroup();
			boolean inLegendary = data.getCharacter().hasActiveForm() && TransformationsHelper.isMutantLegendaryGroup(group);

			if (!status.isRageActive() && resources.isRageFull() && inLegendary) {
				status.setRageActive(true);
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.INSTA_FORM_ON.get(), SoundSource.PLAYERS, 0.8F, 0.7F);
				sync(player);
			}

			if (status.isRageActive()) {
				if (!inLegendary) {
					status.setRageActive(false);
					sync(player);
				} else {
					resources.setRage(resources.getRage() - (float) (100.0 / (cfg.getActiveDurationSeconds() * 20.0)));
					if (resources.getRage() <= 0.0f) endRage(player, data);
				}
				return;
			}

			if (inLegendary && isBorrowed(data)) {
				revertBorrowedForm(player, data);
				sync(player);
				return;
			}

			if (resources.getRage() > 0.0f) {
				double idleSeconds = resources.isRageFull() ? cfg.getFullIdleSeconds() : cfg.getPartialIdleSeconds();
				long idleMs = System.currentTimeMillis() - state(player).lastCombatMs;
				if (idleMs > idleSeconds * 1000.0) {
					resources.setRage(resources.getRage() - (float) (100.0 / (cfg.getIdleDrainSeconds() * 20.0)));
				}
			}
		});
	}

	private static void endRage(ServerPlayer player, StatsData data) {
		data.getResources().setRage(0.0f);
		data.getStatus().setRageActive(false);
		if (isBorrowed(data)) {
			revertBorrowedForm(player, data);
			player.sendSystemMessage(Component.translatable("message.dragonminez.mutant.rage_ended"), true);
		}
		sync(player);
	}

	private static boolean isBorrowed(StatsData data) {
		FormConfig.FormData form = data.getCharacter().getActiveFormData();
		return TransformationsHelper.isRageBorrowedForm(data, data.getCharacter().getActiveFormGroup(), form);
	}

	private static void revertBorrowedForm(ServerPlayer player, StatsData data) {
		var character = data.getCharacter();
		for (int i = 0; i < REVERT_GUARD && character.hasActiveForm() && isBorrowed(data); i++) {
			String previousForm = character.isHasPreviousFormRecord() ? character.getPreviousForm() : null;
			String previousGroup = character.getPreviousFormGroup();
			boolean hadRecord = character.isHasPreviousFormRecord();
			character.clearPreviousFormRecord();

			if (hadRecord && previousForm != null && !previousForm.isEmpty()) {
				character.setActiveForm(previousGroup, previousForm);
				continue;
			}
			FormConfig.FormData lower = hadRecord ? null : TransformationsHelper.getPreviousForm(data);
			if (lower != null) {
				character.setActiveForm(character.getActiveFormGroup(), lower.getName());
			} else {
				TransformationsHelper.revertToBaseForm(player, data);
				player.removeEffect(MainEffects.TRANSFORMED.get());
			}
		}
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.INSTA_FORM_OFF.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		player.refreshDimensions();
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			data.getResources().setRage(0.0f);
			data.getStatus().setRageActive(false);
		});
		STATES.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		STATES.remove(event.getEntity().getUUID());
	}

	private static CombatState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), id -> {
			CombatState created = new CombatState();
			created.lastCombatMs = System.currentTimeMillis();
			return created;
		});
	}

	private static void withMutant(ServerPlayer player, java.util.function.Consumer<StatsData> action) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (MutantManager.isMutant(data) && data.getStatus().isHasCreatedCharacter()) action.accept(data);
		});
	}

	private static void sync(ServerPlayer player) {
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}
}
