package com.dragonminez.server.events.players;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.GodRitualHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KiShareService {
	private static final double START_RANGE_TOLERANCE = 2.0;
	private static final float START_SOUND_VOLUME = 0.6f;

	private static final Map<UUID, Link> LINKS = new LinkedHashMap<>();
	private static final Map<UUID, Integer> BOOSTED = new HashMap<>();
	private static final Map<UUID, Float> ATTACK_CREDIT = new HashMap<>();

	private static final class Link {
		private final UUID receiver;
		private final long startTick;
		private float pendingKi;
		private float damageTaken;

		private Link(UUID receiver, long startTick) {
			this.receiver = receiver;
			this.startTick = startTick;
		}
	}

	public record SettledDonor(ServerPlayer player, StatsData data) {}

	private KiShareService() {}

	public static boolean isDonating(Player player) {
		return player != null && LINKS.containsKey(player.getUUID());
	}

	public static boolean isReceiving(UUID receiver) {
		for (Link link : LINKS.values()) {
			if (link.receiver.equals(receiver)) return true;
		}
		return false;
	}

	public static void requestStart(ServerPlayer donor, int targetId) {
		GeneralServerConfig.KiTransferConfig cfg = GodRitualHelper.transferConfig();
		if (cfg == null || !cfg.getEnabled() || LINKS.containsKey(donor.getUUID())) return;
		if (!(donor.level().getEntity(targetId) instanceof ServerPlayer receiver) || receiver == donor) return;
		StatsData donorData = stats(donor);
		StatsData receiverData = stats(receiver);
		if (donorData == null || receiverData == null) return;

		String refusal = refusal(donor, donorData, receiver, receiverData, cfg);
		if (refusal != null) {
			donor.displayClientMessage(Component.translatable(refusal, receiver.getDisplayName()), true);
			return;
		}

		LINKS.put(donor.getUUID(), new Link(receiver.getUUID(), donor.level().getGameTime()));
		var status = donorData.getStatus();
		status.setKiTransferTarget(receiver.getId());
		status.setChargingKi(false);
		status.setActionCharging(false);
		status.setBlocking(false);
		donorData.getTechniques().clearTechniqueCharge();
		sync(donor);

		donor.level().playSound(null, donor.getX(), donor.getY(), donor.getZ(), MainSounds.AURA_START.get(), SoundSource.PLAYERS, START_SOUND_VOLUME, 1.2f);
		donor.displayClientMessage(Component.translatable("message.dragonminez.kishare.giving", receiver.getDisplayName()), true);
		receiver.displayClientMessage(Component.translatable("message.dragonminez.kishare.receiving", donor.getDisplayName()), true);
		LogUtil.debug(Env.SERVER, "Ki share started: {} -> {} (donor ki {}/{})", donor.getGameProfile().getName(),
				receiver.getGameProfile().getName(), donorData.getResources().getCurrentEnergy(), donorData.getMaxEnergy());
	}

	public static void requestStop(ServerPlayer donor) {
		stop(donor.getServer(), donor.getUUID(), null);
	}

	public static void releaseForRitual(MinecraftServer server, UUID receiver) {
		for (UUID donorId : new ArrayList<>(LINKS.keySet())) {
			if (LINKS.get(donorId).receiver.equals(receiver)) stop(server, donorId, null);
		}
		stop(server, receiver, null);
	}

	public static List<SettledDonor> settledDonors(MinecraftServer server, UUID receiver, long now) {
		GeneralServerConfig.KiTransferConfig cfg = GodRitualHelper.transferConfig();
		long delay = cfg != null ? Math.round(cfg.getBoostDelaySeconds() * 20.0) : 20L;
		List<SettledDonor> out = new ArrayList<>();
		for (Map.Entry<UUID, Link> entry : LINKS.entrySet()) {
			Link link = entry.getValue();
			if (!link.receiver.equals(receiver) || now - link.startTick < delay) continue;
			ServerPlayer donor = server.getPlayerList().getPlayer(entry.getKey());
			StatsData data = donor != null ? stats(donor) : null;
			if (data != null) out.add(new SettledDonor(donor, data));
		}
		return out;
	}

	public static int drawAttackCredit(ServerPlayer receiver, int amount) {
		Float credit = ATTACK_CREDIT.get(receiver.getUUID());
		if (credit == null || amount <= 0) return 0;
		int drawn = (int) Math.min(amount, Math.floor(credit));
		if (drawn <= 0) return 0;
		float left = credit - drawn;
		if (left <= 0.0f) ATTACK_CREDIT.remove(receiver.getUUID());
		else ATTACK_CREDIT.put(receiver.getUUID(), left);
		return drawn;
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;
		if (LINKS.isEmpty() && BOOSTED.isEmpty() && ATTACK_CREDIT.isEmpty()) return;

		GeneralServerConfig.KiTransferConfig cfg = GodRitualHelper.transferConfig();
		if (cfg == null || !cfg.getEnabled()) {
			for (UUID donorId : new ArrayList<>(LINKS.keySet())) stop(server, donorId, null);
			refreshBoosts(server, Map.of());
			ATTACK_CREDIT.clear();
			return;
		}

		Map<UUID, Integer> settledPerReceiver = new HashMap<>();
		Set<UUID> receivers = new HashSet<>();
		long delay = Math.round(cfg.getBoostDelaySeconds() * 20.0);
		for (UUID donorId : new ArrayList<>(LINKS.keySet())) {
			Link link = LINKS.get(donorId);
			if (link == null) continue;
			ServerPlayer donor = server.getPlayerList().getPlayer(donorId);
			ServerPlayer receiver = server.getPlayerList().getPlayer(link.receiver);
			StatsData donorData = donor != null ? stats(donor) : null;
			StatsData receiverData = receiver != null ? stats(receiver) : null;
			String reason = linkBreak(donor, donorData, receiver, receiverData, cfg);
			if (reason != null) {
				stop(server, donorId, reason.isEmpty() ? null : reason);
				continue;
			}
			if (!transfer(donor, donorData, receiver, receiverData, link, cfg)) {
				stop(server, donorId, "message.dragonminez.kishare.out_of_ki");
				continue;
			}
			receivers.add(link.receiver);
			if (donor.level().getGameTime() - link.startTick >= delay) settledPerReceiver.merge(link.receiver, 1, Integer::sum);
		}

		refreshBoosts(server, settledPerReceiver);
		ATTACK_CREDIT.keySet().removeIf(id -> {
			ServerPlayer receiver = server.getPlayerList().getPlayer(id);
			StatsData data = receiver != null ? stats(receiver) : null;
			return data == null || !isChargingAttack(data);
		});

		for (UUID receiverId : receivers) {
			ServerPlayer receiver = server.getPlayerList().getPlayer(receiverId);
			if (receiver != null) GodRitualService.evaluate(server, receiver);
		}
	}

	private static String refusal(ServerPlayer donor, StatsData donorData, ServerPlayer receiver, StatsData receiverData,
								  GeneralServerConfig.KiTransferConfig cfg) {
		if (!canAct(donor, donorData)) return "message.dragonminez.kishare.cannot";
		if (!isValidReceiver(receiver, receiverData) || receiverData.getStatus().isInGodRitual()) return "message.dragonminez.kishare.invalid_target";
		if (LINKS.containsKey(receiver.getUUID())) return "message.dragonminez.kishare.target_busy";
		if (isReceiving(donor.getUUID())) return "message.dragonminez.kishare.self_busy";
		if (donorData.getResources().getCurrentEnergy() <= 0.0f) return "message.dragonminez.kishare.out_of_ki";
		if (donor.level() != receiver.level() || donor.distanceTo(receiver) > cfg.getStartRange() + START_RANGE_TOLERANCE
				|| !donor.hasLineOfSight(receiver)) return "message.dragonminez.kishare.too_far";
		return null;
	}

	private static String linkBreak(ServerPlayer donor, StatsData donorData, ServerPlayer receiver, StatsData receiverData,
									GeneralServerConfig.KiTransferConfig cfg) {
		if (donor == null || donorData == null) return "";
		if (donorData.getStatus().isInGodRitual()) return "";
		if (!canAct(donor, donorData)) return "message.dragonminez.kishare.interrupted";
		if (receiver == null || receiverData == null || !isValidReceiver(receiver, receiverData)) return "message.dragonminez.kishare.lost";
		if (receiverData.getStatus().isInGodRitual()) return "";
		if (donor.level() != receiver.level() || donor.distanceTo(receiver) > cfg.getMaxLinkDistance()) return "message.dragonminez.kishare.lost";
		return null;
	}

	private static boolean canAct(ServerPlayer player, StatsData data) {
		if (!player.isAlive() || player.isSpectator() || !data.getStatus().isHasCreatedCharacter()) return false;
		if (player.hasEffect(MainEffects.STUN.get())) return false;
		var status = data.getStatus();
		return !status.isKnockedDown() && !status.isStrikeLocked() && !status.isMatchFrozen() && !status.isInGodRitual();
	}

	private static boolean isValidReceiver(ServerPlayer receiver, StatsData data) {
		return receiver.isAlive() && !receiver.isSpectator() && data.getStatus().isHasCreatedCharacter();
	}

	private static boolean transfer(ServerPlayer donor, StatsData donorData, ServerPlayer receiver, StatsData receiverData,
									Link link, GeneralServerConfig.KiTransferConfig cfg) {
		link.pendingKi += (float) (donorData.getMaxEnergy() * cfg.getKiPercentPerSecond() / 20.0);
		float whole = (float) (Math.floor(link.pendingKi * 4.0) / 4.0);
		if (whole <= 0.0f) return true;
		link.pendingKi -= whole;

		float available = donorData.getResources().getCurrentEnergy();
		float given = donor.isCreative() ? whole : Math.min(whole, available);
		if (given <= 0.0f) return false;
		if (!donor.isCreative()) donorData.getResources().setCurrentEnergy(available - given);
		deliver(receiver, receiverData, given, cfg);
		return donor.isCreative() || donorData.getResources().getCurrentEnergy() > 0.0f;
	}

	private static void deliver(ServerPlayer receiver, StatsData data, float amount, GeneralServerConfig.KiTransferConfig cfg) {
		float current = data.getResources().getCurrentEnergy();
		float max = data.getMaxEnergy();
		float stored = Math.min(Math.max(0.0f, max - current), amount);
		if (stored > 0.0f) data.getResources().setCurrentEnergy(current + stored);
		float overflow = amount - stored;
		if (overflow <= 0.0f || !isChargingAttack(data)) return;
		float cap = (float) (max * cfg.getAttackChargeCreditRatio());
		ATTACK_CREDIT.merge(receiver.getUUID(), overflow, (a, b) -> Math.min(cap, a + b));
	}

	private static boolean isChargingAttack(StatsData data) {
		return data.getTechniques().isTechniqueCharging() || data.getTechniques().isTechniqueChargeActive();
	}

	private static void refreshBoosts(MinecraftServer server, Map<UUID, Integer> settledPerReceiver) {
		Set<UUID> touched = new HashSet<>(BOOSTED.keySet());
		touched.addAll(settledPerReceiver.keySet());
		for (UUID receiverId : touched) {
			int donors = settledPerReceiver.getOrDefault(receiverId, 0);
			if (donors > 0) BOOSTED.put(receiverId, donors);
			else BOOSTED.remove(receiverId);
			ServerPlayer receiver = server.getPlayerList().getPlayer(receiverId);
			StatsData data = receiver != null ? stats(receiver) : null;
			if (data == null || data.getStatus().getKiTransferDonors() == donors) continue;
			data.getStatus().setKiTransferDonors(donors);
			sync(receiver);
		}
	}

	private static void stop(@Nullable MinecraftServer server, UUID donorId, @Nullable String messageKey) {
		Link link = LINKS.remove(donorId);
		if (link == null || server == null) return;
		ServerPlayer donor = server.getPlayerList().getPlayer(donorId);
		StatsData data = donor != null ? stats(donor) : null;
		if (data == null) return;
		if (data.getStatus().getKiTransferTarget() != -1) {
			data.getStatus().setKiTransferTarget(-1);
			sync(donor);
		}
		if (messageKey != null) donor.displayClientMessage(Component.translatable(messageKey), true);
	}

	private static boolean isLocked(Player player) {
		if (player == null) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> data.getStatus().isKiShareLocked()).orElse(false);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDamage(LivingDamageEvent event) {
		if (event.isCanceled() || event.getAmount() <= 0.0f || !(event.getEntity() instanceof ServerPlayer victim)) return;
		Link link = LINKS.get(victim.getUUID());
		if (link == null) return;
		GeneralServerConfig.KiTransferConfig cfg = GodRitualHelper.transferConfig();
		if (cfg == null) return;
		link.damageTaken += Math.min(event.getAmount(), victim.getHealth());
		if (link.damageTaken >= victim.getMaxHealth() * cfg.getInterruptDamageRatio()) {
			stop(victim.getServer(), victim.getUUID(), "message.dragonminez.kishare.interrupted");
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onAttackEntity(AttackEntityEvent event) {
		if (isLocked(event.getEntity())) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingAttack(LivingAttackEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (event.getSource().getEntity() instanceof Player attacker && isLocked(attacker)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		if (isLocked(event.getEntity())) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (isLocked(event.getEntity())) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (isLocked(event.getEntity())) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		if (isLocked(event.getEntity())) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (isLocked(event.getEntity())) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
		if (event.getEntity() instanceof Player player && isLocked(player)) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onEnergyRegen(DMZEvent.EnergyRegenEvent event) {
		if (event.getAmount() > 0.0 && isDonating(event.getPlayer())) event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) stop(player.getServer(), player.getUUID(), null);
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) stop(player.getServer(), player.getUUID(), null);
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		StatsData data = stats(player);
		if (data == null) return;
		var status = data.getStatus();
		if (status.getKiTransferTarget() == -1 && status.getKiTransferDonors() == 0) return;
		status.setKiTransferTarget(-1);
		status.setKiTransferDonors(0);
		sync(player);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		stop(player.getServer(), player.getUUID(), null);
		BOOSTED.remove(player.getUUID());
		ATTACK_CREDIT.remove(player.getUUID());
		StatsData data = stats(player);
		if (data != null) data.getStatus().setKiTransferDonors(0);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		LINKS.clear();
		BOOSTED.clear();
		ATTACK_CREDIT.clear();
	}

	private static StatsData stats(Player player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
	}

	private static void sync(ServerPlayer player) {
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}
}
