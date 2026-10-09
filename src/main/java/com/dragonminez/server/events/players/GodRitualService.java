package com.dragonminez.server.events.players;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.util.GodRitualHelper;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GodRitualService {
	private static final int GATHER_TICKS = 20;
	private static final int CHARGE_STEP_TICKS = 20;
	private static final int FEEDBACK_INTERVAL_TICKS = 60;
	private static final int SLOW_FALL_TICKS = 160;
	private static final double PIN_TOLERANCE_SQ = 0.25;
	private static final double GROUND_PROBE = 0.2;
	private static final double LIFT_PROBE_STEP = 0.25;

	private enum Phase { GATHER, CHANNEL, ASCEND, SHELL }

	private static final Map<UUID, Ritual> RITUALS = new LinkedHashMap<>();
	private static final Map<UUID, UUID> PARTICIPANTS = new HashMap<>();
	private static final Map<UUID, Long> LAST_FEEDBACK = new HashMap<>();

	private record Slot(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, float yaw) {}

	private record Candidate(ServerPlayer player, StatsData data, @Nullable ServerPlayer partner, int individuals, @Nullable Component problem) {}

	private static final class Ritual {
		private final UUID recipient;
		private final Vec3 center;
		private final List<UUID> donors;
		private final List<UUID> partners;
		private final Map<UUID, Slot> slots = new HashMap<>();
		private final Map<UUID, Float> damage = new HashMap<>();
		private Phase phase = Phase.GATHER;
		private int ticks;
		private int charge;
		private double liftFromY;
		private double liftToY;

		private Ritual(UUID recipient, Vec3 center, List<UUID> donors, List<UUID> partners) {
			this.recipient = recipient;
			this.center = center;
			this.donors = donors;
			this.partners = partners;
		}

		private List<UUID> bodies() {
			List<UUID> all = new ArrayList<>(donors);
			all.add(recipient);
			return all;
		}
	}

	private GodRitualService() {}

	public static boolean isParticipant(Player player) {
		return player != null && PARTICIPANTS.containsKey(player.getUUID());
	}

	public static boolean ownsActionCharge(Player player) {
		return player != null && RITUALS.containsKey(player.getUUID());
	}

	public static void evaluate(MinecraftServer server, ServerPlayer recipient) {
		GeneralServerConfig.GodRitualConfig cfg = GodRitualHelper.config();
		if (cfg == null || !cfg.getEnabled() || isParticipant(recipient)) return;
		StatsData data = stats(recipient);
		if (data == null || !GodRitualHelper.isSaiyan(data) || !GodRitualHelper.isInAnyForm(data, cfg.getRecipientForms())) return;

		long now = recipient.level().getGameTime();
		List<Candidate> candidates = new ArrayList<>();
		int attempted = 0;
		for (KiShareService.SettledDonor donor : KiShareService.settledDonors(server, recipient.getUUID(), now)) {
			Candidate candidate = candidate(server, donor.player(), donor.data(), recipient, cfg);
			if (candidate == null) continue;
			candidates.add(candidate);
			attempted += candidate.individuals();
		}
		if (attempted < cfg.getRequiredDonors()) return;

		Component problem = recipientProblem(recipient, data);
		List<Candidate> chosen = new ArrayList<>();
		int ready = 0;
		for (Candidate candidate : candidates) {
			if (candidate.problem() != null) {
				if (problem == null) problem = candidate.problem();
				continue;
			}
			if (ready >= cfg.getRequiredDonors()) continue;
			chosen.add(candidate);
			ready += candidate.individuals();
		}

		if (ready >= cfg.getRequiredDonors() && recipientProblem(recipient, data) == null) {
			begin(server, recipient, data, chosen, cfg);
			return;
		}
		if (problem != null) feedback(recipient, candidates, problem, now);
	}

	private static Candidate candidate(MinecraftServer server, ServerPlayer donor, StatsData data, ServerPlayer recipient,
									   GeneralServerConfig.GodRitualConfig cfg) {
		if (!GodRitualHelper.isSaiyan(data) || !GodRitualHelper.isInAnyForm(data, cfg.getDonorForms())) return null;
		ServerPlayer partner = null;
		StatsData partnerData = null;
		if (data.getStatus().isFused()) {
			partner = data.getStatus().getFusionPartnerUUID() != null ? server.getPlayerList().getPlayer(data.getStatus().getFusionPartnerUUID()) : null;
			partnerData = partner != null ? stats(partner) : null;
			if (partnerData == null || !GodRitualHelper.isSaiyan(partnerData)) return null;
		}
		int individuals = partner != null ? 2 : 1;

		Component problem = null;
		if (!GodRitualHelper.isPureHeart(data)) problem = blocked("impure", donor);
		else if (partnerData != null && !GodRitualHelper.isPureHeart(partnerData)) problem = blocked("impure", partner);
		else if (data.getCooldowns().hasCooldown(Cooldowns.GOD_RITUAL)) problem = blocked("cooldown", donor);
		else if (partnerData != null && partnerData.getCooldowns().hasCooldown(Cooldowns.GOD_RITUAL)) problem = blocked("cooldown", partner);
		else if (!withinCircle(donor, recipient, cfg)) problem = blocked("too_far", donor);
		return new Candidate(donor, data, partner, individuals, problem);
	}

	private static Component recipientProblem(ServerPlayer recipient, StatsData data) {
		if (data.getStatus().isFused() || data.getStatus().getFusionPartnerUUID() != null) return Component.translatable("message.dragonminez.godritual.blocked.fused");
		if (!GodRitualHelper.isPureHeart(data)) return blocked("impure", recipient);
		if (data.getCooldowns().hasCooldown(Cooldowns.GOD_RITUAL)) return blocked("cooldown", recipient);
		if (data.getStatus().isKnockedDown() || !recipient.isAlive()) return blocked("unable", recipient);
		return null;
	}

	private static Component blocked(String reason, ServerPlayer who) {
		return Component.translatable("message.dragonminez.godritual.blocked." + reason, who.getDisplayName());
	}

	private static boolean withinCircle(ServerPlayer donor, ServerPlayer recipient, GeneralServerConfig.GodRitualConfig cfg) {
		if (donor.level() != recipient.level()) return false;
		double dx = donor.getX() - recipient.getX();
		double dz = donor.getZ() - recipient.getZ();
		double radius = cfg.getRitualRadius();
		return dx * dx + dz * dz <= radius * radius && Math.abs(donor.getY() - recipient.getY()) <= cfg.getRitualVerticalRange();
	}

	private static void feedback(ServerPlayer recipient, List<Candidate> candidates, Component problem, long now) {
		Long last = LAST_FEEDBACK.get(recipient.getUUID());
		if (last != null && now - last < FEEDBACK_INTERVAL_TICKS) return;
		LAST_FEEDBACK.put(recipient.getUUID(), now);
		recipient.displayClientMessage(problem, true);
		for (Candidate candidate : candidates) candidate.player().displayClientMessage(problem, true);
	}

	private static void begin(MinecraftServer server, ServerPlayer recipient, StatsData recipientData, List<Candidate> chosen,
							  GeneralServerConfig.GodRitualConfig cfg) {
		Vec3 center = recipient.position();
		chosen.sort(Comparator.comparingDouble(c -> Math.atan2(c.player().getZ() - center.z, c.player().getX() - center.x)));
		List<UUID> donors = new ArrayList<>();
		List<UUID> partners = new ArrayList<>();
		for (Candidate candidate : chosen) {
			donors.add(candidate.player().getUUID());
			if (candidate.partner() != null) partners.add(candidate.partner().getUUID());
		}

		KiShareService.releaseForRitual(server, recipient.getUUID());
		Ritual ritual = new Ritual(recipient.getUUID(), center, donors, partners);
		planRing(ritual, recipient, chosen, cfg);
		RITUALS.put(recipient.getUUID(), ritual);
		for (UUID body : ritual.bodies()) PARTICIPANTS.put(body, recipient.getUUID());
		LAST_FEEDBACK.remove(recipient.getUUID());

		enter(recipient, recipientData, Status.GOD_RITUAL_RECIPIENT, recipient.getUUID());
		recipientData.getResources().setActionCharge(0);
		for (Candidate candidate : chosen) enter(candidate.player(), candidate.data(), Status.GOD_RITUAL_DONOR, recipient.getUUID());

		recipient.level().playSound(null, center.x, center.y, center.z, MainSounds.AURA_START.get(), SoundSource.PLAYERS, 1.5f, 0.8f);
		broadcast(server, ritual, Component.translatable("message.dragonminez.godritual.begin"));
		LogUtil.info(Env.SERVER, "God ritual started for {} with {} donors ({} fused partners) at {}", recipient.getGameProfile().getName(),
				donors.size(), partners.size(), center);
	}

	private static void planRing(Ritual ritual, ServerPlayer recipient, List<Candidate> chosen, GeneralServerConfig.GodRitualConfig cfg) {
		Vec3 center = recipient.position();
		int count = chosen.size();
		ServerPlayer first = chosen.get(0).player();
		double baseAngle = Math.atan2(first.getZ() - center.z, first.getX() - center.x);
		double radius = cfg.getRingRadius();
		for (int i = 0; i < count; i++) {
			ServerPlayer donor = chosen.get(i).player();
			double angle = baseAngle + i * (Math.PI * 2.0 / count);
			double x = center.x + Math.cos(angle) * radius;
			double z = center.z + Math.sin(angle) * radius;
			double y = center.y;
			if (!canStand(donor, x, y, z, recipient.onGround())) {
				x = donor.getX();
				y = donor.getY();
				z = donor.getZ();
			}
			float yaw = (float) (Mth.atan2(-(center.x - x), center.z - z) * Mth.RAD_TO_DEG);
			ritual.slots.put(donor.getUUID(), new Slot(donor.getX(), donor.getY(), donor.getZ(), x, y, z, yaw));
		}
	}

	private static boolean canStand(ServerPlayer player, double x, double y, double z, boolean needsGround) {
		AABB box = player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
		if (!player.level().noCollision(player, box)) return false;
		if (!needsGround) return true;
		return !player.level().noCollision(player, new AABB(box.minX, box.minY - GROUND_PROBE, box.minZ, box.maxX, box.minY, box.maxZ));
	}

	private static void enter(ServerPlayer player, StatsData data, int role, UUID anchor) {
		var status = data.getStatus();
		status.setGodRitualRole(role);
		status.setGodRitualAnchor(anchor);
		status.setChargingKi(false);
		status.setActionCharging(false);
		status.setDescending(false);
		status.setBlocking(false);
		data.getTechniques().clearTechniqueCharge();
		player.setSprinting(false);
		sync(player);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || RITUALS.isEmpty()) return;
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		GeneralServerConfig.GodRitualConfig cfg = GodRitualHelper.config();
		if (server == null || cfg == null) return;
		for (Ritual ritual : new ArrayList<>(RITUALS.values())) tickRitual(server, ritual, cfg);
	}

	private static void tickRitual(MinecraftServer server, Ritual ritual, GeneralServerConfig.GodRitualConfig cfg) {
		ServerPlayer recipient = server.getPlayerList().getPlayer(ritual.recipient);
		StatsData recipientData = recipient != null ? stats(recipient) : null;
		if (recipientData == null || !recipient.isAlive() || !GodRitualHelper.isInAnyForm(recipientData, cfg.getRecipientForms())) {
			cancel(server, ritual, cfg, "message.dragonminez.godritual.interrupted");
			return;
		}
		for (UUID donorId : ritual.donors) {
			ServerPlayer donor = server.getPlayerList().getPlayer(donorId);
			StatsData donorData = donor != null ? stats(donor) : null;
			if (donorData == null || !donor.isAlive() || donor.level() != recipient.level()
					|| !GodRitualHelper.isInAnyForm(donorData, cfg.getDonorForms())) {
				cancel(server, ritual, cfg, "message.dragonminez.godritual.interrupted");
				return;
			}
		}

		ritual.ticks++;
		switch (ritual.phase) {
			case GATHER -> {
				float t = Math.min(1.0f, (float) ritual.ticks / GATHER_TICKS);
				float eased = t * t * (3.0f - 2.0f * t);
				for (UUID donorId : ritual.donors) glide(server.getPlayerList().getPlayer(donorId), ritual.slots.get(donorId), eased);
				if (ritual.ticks >= GATHER_TICKS) {
					ritual.phase = Phase.CHANNEL;
					ritual.ticks = 0;
				}
			}
			case CHANNEL -> {
				for (UUID donorId : ritual.donors) pin(server.getPlayerList().getPlayer(donorId), ritual.slots.get(donorId));
				pinCenter(recipient, ritual.center);
				int channelTicks = cfg.getChannelTicks();
				if (ritual.ticks % CHARGE_STEP_TICKS == 0 || ritual.ticks >= channelTicks) {
					ritual.charge = Math.min(100, (int) Math.round(100.0 * ritual.ticks / channelTicks));
					recipientData.getResources().setActionCharge(ritual.charge);
					sync(recipient);
				}
				if (ritual.ticks >= channelTicks) {
					ritual.phase = Phase.ASCEND;
					ritual.ticks = 0;
					ritual.liftFromY = recipient.getY();
					ritual.liftToY = recipient.getY() + clearance(recipient, cfg.getLiftHeight());
					recipient.level().playSound(null, recipient.getX(), recipient.getY(), recipient.getZ(), MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
				}
			}
			case ASCEND -> {
				for (UUID donorId : ritual.donors) pin(server.getPlayerList().getPlayer(donorId), ritual.slots.get(donorId));
				int liftTicks = cfg.getLiftTicks();
				float t = Math.min(1.0f, (float) ritual.ticks / liftTicks);
				float eased = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
				double y = Mth.lerp(eased, ritual.liftFromY, ritual.liftToY);
				recipient.connection.teleport(ritual.center.x, y, ritual.center.z, recipient.getYRot(), recipient.getXRot());
				recipient.setDeltaMovement(Vec3.ZERO);
				recipient.resetFallDistance();
				if (ritual.ticks >= liftTicks) {
					ritual.phase = Phase.SHELL;
					ritual.ticks = 0;
					recipientData.getStatus().setGodRitualGlowStart(recipient.level().getGameTime());
					sync(recipient);
				}
			}
			case SHELL -> {
				for (UUID donorId : ritual.donors) pin(server.getPlayerList().getPlayer(donorId), ritual.slots.get(donorId));
				recipient.connection.teleport(ritual.center.x, ritual.liftToY, ritual.center.z, recipient.getYRot(), recipient.getXRot());
				recipient.setDeltaMovement(Vec3.ZERO);
				recipient.resetFallDistance();
				if (ritual.ticks >= cfg.getGlowInTicks()) complete(server, ritual, recipient, recipientData, cfg);
			}
		}
	}

	private static double clearance(ServerPlayer player, double height) {
		double free = 0.0;
		for (double h = LIFT_PROBE_STEP; h <= height + 1.0E-6; h += LIFT_PROBE_STEP) {
			if (!player.level().noCollision(player, player.getBoundingBox().move(0.0, h, 0.0))) break;
			free = h;
		}
		return free;
	}

	private static void glide(@Nullable ServerPlayer player, @Nullable Slot slot, float factor) {
		if (player == null || slot == null) return;
		double x = Mth.lerp(factor, slot.fromX(), slot.toX());
		double y = Mth.lerp(factor, slot.fromY(), slot.toY());
		double z = Mth.lerp(factor, slot.fromZ(), slot.toZ());
		face(player, x, y, z, slot.yaw());
	}

	private static void pin(@Nullable ServerPlayer player, @Nullable Slot slot) {
		if (player == null || slot == null) return;
		double dx = player.getX() - slot.toX();
		double dz = player.getZ() - slot.toZ();
		if (dx * dx + dz * dz <= PIN_TOLERANCE_SQ && Math.abs(Mth.wrapDegrees(player.getYRot() - slot.yaw())) < 1.0f) return;
		face(player, slot.toX(), player.getY(), slot.toZ(), slot.yaw());
	}

	private static void pinCenter(ServerPlayer recipient, Vec3 center) {
		double dx = recipient.getX() - center.x;
		double dz = recipient.getZ() - center.z;
		if (dx * dx + dz * dz <= PIN_TOLERANCE_SQ) return;
		recipient.connection.teleport(center.x, recipient.getY(), center.z, recipient.getYRot(), recipient.getXRot());
		recipient.setDeltaMovement(Vec3.ZERO);
	}

	private static void face(ServerPlayer player, double x, double y, double z, float yaw) {
		player.connection.teleport(x, y, z, yaw, 0.0f);
		player.setDeltaMovement(Vec3.ZERO);
		player.setYHeadRot(yaw);
		player.yHeadRotO = yaw;
		player.yBodyRot = yaw;
		player.yBodyRotO = yaw;
		player.resetFallDistance();
	}

	private static void complete(MinecraftServer server, Ritual ritual, ServerPlayer recipient, StatsData data, GeneralServerConfig.GodRitualConfig cfg) {
		FormConfig.FormData form = GodRitualHelper.formData();
		if (form == null) {
			LogUtil.error(Env.SERVER, "God ritual form {}.{} is missing from the saiyan forms config", cfg.getGroupName(), cfg.getFormName());
			cancel(server, ritual, cfg, "message.dragonminez.godritual.interrupted");
			return;
		}
		finish(server, ritual);

		var character = data.getCharacter();
		if (!character.getFormsUsedBefore().getFormGroup(cfg.getGroupName()).contains(form.getName())) {
			character.getFormsUsedBefore().putForm(cfg.getGroupName(), form.getName());
		}
		float[] resourceSnapshot = data.snapshotMultiplierResources();
		character.clearPreviousFormRecord();
		character.setActiveForm(cfg.getGroupName(), form.getName());
		data.restoreMultiplierGains(recipient, resourceSnapshot);
		data.getStatus().setGodRitualTicks(cfg.getDurationTicks());
		data.getResources().setActionCharge(0);
		if (!recipient.hasEffect(MainEffects.TRANSFORMED.get())) {
			recipient.addEffect(new MobEffectInstance(MainEffects.TRANSFORMED.get(), -1, 0, false, false, true));
		}
		recipient.refreshDimensions();
		recipient.resetFallDistance();
		if (!data.getSkills().isSkillActive("fly")) {
			recipient.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, false, false, false));
		}

		for (UUID donorId : ritual.donors) applyCooldown(server.getPlayerList().getPlayer(donorId), cfg.getCooldownTicks());
		for (UUID partnerId : ritual.partners) applyCooldown(server.getPlayerList().getPlayer(partnerId), cfg.getCooldownTicks());

		recipient.level().playSound(null, recipient.getX(), recipient.getY(), recipient.getZ(), MainSounds.FUSION.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		recipient.level().playSound(null, recipient.getX(), recipient.getY(), recipient.getZ(), MainSounds.TRANSFORM_ON.get(), SoundSource.PLAYERS, 1.0f, 0.9f);
		recipient.sendSystemMessage(Component.translatable("message.dragonminez.godritual.transformed"), true);
		broadcastDonors(server, ritual, Component.translatable("message.dragonminez.godritual.completed", recipient.getDisplayName()));
		sync(recipient);

		FormConfig.FormData reference = data.getGodRitualReferenceForm();
		LogUtil.info(Env.SERVER, "God ritual completed for {}: reference form {}, STR x{}", recipient.getGameProfile().getName(),
				reference != null ? reference.getName() : "none", data.getFormMultiplier("STR"));
	}

	private static void finish(MinecraftServer server, Ritual ritual) {
		RITUALS.remove(ritual.recipient);
		for (UUID body : ritual.bodies()) {
			PARTICIPANTS.remove(body);
			ServerPlayer player = server.getPlayerList().getPlayer(body);
			StatsData data = player != null ? stats(player) : null;
			if (data == null) continue;
			data.getStatus().setGodRitualRole(Status.GOD_RITUAL_NONE);
			data.getStatus().setGodRitualAnchor(null);
			if (body.equals(ritual.recipient)) data.getResources().setActionCharge(0);
			sync(player);
		}
	}

	private static void cancel(MinecraftServer server, Ritual ritual, GeneralServerConfig.GodRitualConfig cfg, String messageKey) {
		boolean lifted = ritual.phase == Phase.ASCEND || ritual.phase == Phase.SHELL;
		ServerPlayer vessel = server.getPlayerList().getPlayer(ritual.recipient);
		StatsData vesselData = vessel != null ? stats(vessel) : null;
		if (vesselData != null) vesselData.getStatus().setGodRitualGlowStart(0L);
		finish(server, ritual);
		List<UUID> everyone = ritual.bodies();
		everyone.addAll(ritual.partners);
		for (UUID id : everyone) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) continue;
			applyCooldown(player, cfg.getInterruptCooldownTicks());
			player.displayClientMessage(Component.translatable(messageKey), true);
		}
		ServerPlayer recipient = server.getPlayerList().getPlayer(ritual.recipient);
		if (lifted && recipient != null) {
			recipient.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, false, false, false));
		}
		LogUtil.info(Env.SERVER, "God ritual for {} cancelled in {} ({})", ritual.recipient, ritual.phase, messageKey);
	}

	private static void applyCooldown(@Nullable ServerPlayer player, int ticks) {
		StatsData data = player != null ? stats(player) : null;
		if (data == null || ticks <= 0) return;
		Cooldowns cooldowns = data.getCooldowns();
		if (cooldowns.getCooldown(Cooldowns.GOD_RITUAL) < ticks) cooldowns.setCooldown(Cooldowns.GOD_RITUAL, ticks);
		sync(player);
	}

	private static void broadcast(MinecraftServer server, Ritual ritual, Component message) {
		for (UUID id : ritual.bodies()) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) player.displayClientMessage(message, true);
		}
	}

	private static void broadcastDonors(MinecraftServer server, Ritual ritual, Component message) {
		for (UUID id : ritual.donors) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) player.displayClientMessage(message, true);
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
		GeneralServerConfig.GodRitualConfig cfg = GodRitualHelper.config();
		if (cfg == null) return;
		StatsData data = stats(player);
		if (data == null) return;
		var status = data.getStatus();
		if (GodRitualHelper.isShellExpired(data, player.level().getGameTime())) status.setGodRitualGlowStart(0L);
		boolean active = GodRitualHelper.isActiveForm(data);
		int ticks = status.getGodRitualTicks();
		if (active) {
			if (ticks <= 0) {
				status.setGodRitualTicks(cfg.getDurationTicks());
				return;
			}
			status.setGodRitualTicks(ticks - 1);
			if (ticks - 1 <= 0) endForm(player, data, cfg, "message.dragonminez.godritual.expired");
		} else if (ticks > 0) {
			status.setGodRitualTicks(0);
			applyCooldown(player, cfg.getCooldownTicks());
			LogUtil.debug(Env.SERVER, "God ritual form of {} ended outside the timer, cooldown applied", player.getGameProfile().getName());
		}
	}

	private static void endForm(ServerPlayer player, StatsData data, GeneralServerConfig.GodRitualConfig cfg, @Nullable String messageKey) {
		if (GodRitualHelper.isActiveForm(data)) {
			float[] resourceSnapshot = data.snapshotMultiplierResources();
			data.getCharacter().clearPreviousFormRecord();
			TransformationsHelper.revertToBaseForm(player, data, false);
			data.restoreMultiplierGains(player, resourceSnapshot);
			player.removeEffect(MainEffects.TRANSFORMED.get());
			player.refreshDimensions();
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.INSTA_FORM_OFF.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		}
		data.getStatus().setGodRitualTicks(0);
		applyCooldown(player, cfg.getCooldownTicks());
		if (messageKey != null) player.sendSystemMessage(Component.translatable(messageKey), true);
		sync(player);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDamage(LivingDamageEvent event) {
		if (event.isCanceled() || event.getAmount() <= 0.0f || !(event.getEntity() instanceof ServerPlayer victim)) return;
		UUID ritualId = PARTICIPANTS.get(victim.getUUID());
		Ritual ritual = ritualId != null ? RITUALS.get(ritualId) : null;
		GeneralServerConfig.GodRitualConfig cfg = GodRitualHelper.config();
		if (ritual == null || cfg == null) return;
		float total = ritual.damage.merge(victim.getUUID(), Math.min(event.getAmount(), victim.getHealth()), Float::sum);
		if (total > victim.getMaxHealth() * cfg.getInterruptDamageRatio()) {
			cancel(victim.getServer(), ritual, cfg, "message.dragonminez.godritual.interrupted");
		}
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		GeneralServerConfig.GodRitualConfig cfg = GodRitualHelper.config();
		if (cfg == null) return;
		cancelFor(player, cfg);
		StatsData data = stats(player);
		if (data != null && (GodRitualHelper.isActiveForm(data) || data.getStatus().getGodRitualTicks() > 0)) endForm(player, data, cfg, null);
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		GeneralServerConfig.GodRitualConfig cfg = GodRitualHelper.config();
		if (cfg != null && event.getEntity() instanceof ServerPlayer player) cancelFor(player, cfg);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		GeneralServerConfig.GodRitualConfig cfg = GodRitualHelper.config();
		if (cfg != null && event.getEntity() instanceof ServerPlayer player) cancelFor(player, cfg);
		LAST_FEEDBACK.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || isParticipant(player)) return;
		StatsData data = stats(player);
		if (data == null || !data.getStatus().isInGodRitual()) return;
		data.getStatus().setGodRitualRole(Status.GOD_RITUAL_NONE);
		data.getStatus().setGodRitualAnchor(null);
		sync(player);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		RITUALS.clear();
		PARTICIPANTS.clear();
		LAST_FEEDBACK.clear();
	}

	private static void cancelFor(ServerPlayer player, GeneralServerConfig.GodRitualConfig cfg) {
		UUID ritualId = PARTICIPANTS.get(player.getUUID());
		Ritual ritual = ritualId != null ? RITUALS.get(ritualId) : null;
		if (ritual != null) cancel(player.getServer(), ritual, cfg, "message.dragonminez.godritual.interrupted");
	}

	private static StatsData stats(Player player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
	}

	private static void sync(ServerPlayer player) {
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}
}
