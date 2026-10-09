package com.dragonminez.client.systems.kishare;

import com.dragonminez.Reference;
import com.dragonminez.client.systems.FormVisualTransition;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.particles.KiTrailParticle;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.util.GodRitualHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KiShareVisuals {
	private static final float STREAM_TRAVEL_FACTOR = 9.6f;
	private static final double STREAM_JITTER = 0.06;
	private static final float STREAM_SCALE = 0.6f;
	private static final int TRACERS_PER_WAY = 3;
	private static final int TRACER_SUBSTEPS = 3;
	private static final float TRACER_SPEED = 0.03f;
	private static final float TRACER_RETURN_SPEED = 0.022f;
	private static final int TRACER_LIFETIME = 9;
	private static final float TRACER_SCALE = 0.65f;
	private static final double CHEST_RATIO = 0.6;

	private KiShareVisuals() {}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.isPaused()) return;
		for (Player player : level.players()) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null) continue;
			if (data.getStatus().getGodRitualRole() == Status.GOD_RITUAL_DONOR) {
				faceRitualCenter(mc, player, data);
				ritualFlow(mc, level, player, data);
			} else if (data.getStatus().isSharingKi()) transferStream(mc, level, player, data);
		}
	}

	private static void faceRitualCenter(Minecraft mc, Player player, StatsData data) {
		var anchor = data.getStatus().getGodRitualAnchor();
		Player center = anchor != null ? player.level().getPlayerByUUID(anchor) : null;
		if (center == null) return;
		double dx = center.getX() - player.getX();
		double dz = center.getZ() - player.getZ();
		if (dx * dx + dz * dz < 1.0E-4) return;
		float yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
		if (player == mc.player) {
			player.setYRot(yaw);
			player.yRotO = yaw;
		}
		player.setYHeadRot(yaw);
		player.yHeadRotO = yaw;
		player.yBodyRot = yaw;
		player.yBodyRotO = yaw;
	}

	private static void transferStream(Minecraft mc, ClientLevel level, Player donor, StatsData data) {
		if (!(level.getEntity(data.getStatus().getKiTransferTarget()) instanceof Player receiver)) return;
		KiSharePose.Aim aim = KiSharePose.resolve(donor, data, 1.0f);
		if (aim == null) return;
		Vec3 chest = receiver.position().add(0.0, receiver.getBbHeight() * CHEST_RATIO, 0.0);
		float[] color = auraColor(data);
		RandomSource random = level.random;
		emitStream(mc, KiSharePose.hand(donor, data, 1.0f, true, aim.right()), chest, color, random);
		emitStream(mc, KiSharePose.hand(donor, data, 1.0f, false, aim.left()), chest, color, random);
	}

	private static void emitStream(Minecraft mc, Vec3 from, Vec3 to, float[] color, RandomSource random) {
		Vec3 start = from.add(jitter(random), jitter(random), jitter(random));
		float spread = 0.9f + random.nextFloat() * 0.2f;
		Vec3 velocity = to.subtract(start).scale(spread / STREAM_TRAVEL_FACTOR);
		spawn(mc, start, velocity, color, STREAM_SCALE, -1);
	}

	private static void ritualFlow(Minecraft mc, ClientLevel level, Player donor, StatsData data) {
		KiSharePose.Ring ring = KiSharePose.ring(donor, data);
		if (ring == null) return;
		List<Player> donors = ring.donors();
		int index = donors.indexOf(donor);
		if (index < 0) return;
		float[] color = ritualColor(data, ring);
		double time = level.getGameTime();

		if (donors.size() < 3) {
			if (ring.center() == null) return;
			Vec3 chest = ring.center().position().add(0.0, ring.center().getBbHeight() * CHEST_RATIO, 0.0);
			Vec3[] path = {KiSharePose.shoulder(donor, data, 1.0f, true), KiSharePose.hand(donor, data, 1.0f, true, chest), chest};
			emitTracers(mc, path, color, time, index);
			return;
		}

		Player next = donors.get((index + 1) % donors.size());
		StatsData nextData = StatsProvider.get(StatsCapability.INSTANCE, next).orElse(null);
		if (nextData == null) return;
		Vec3 meeting = KiSharePose.meetingPoint(donor, next, data, 1.0f);
		boolean donorRight = isOnRight(donor, meeting);
		boolean nextRight = isOnRight(next, meeting);
		Vec3[] path = {
				KiSharePose.shoulder(donor, data, 1.0f, donorRight),
				KiSharePose.hand(donor, data, 1.0f, donorRight, meeting),
				KiSharePose.hand(next, nextData, 1.0f, nextRight, meeting),
				KiSharePose.shoulder(next, nextData, 1.0f, nextRight)
		};
		emitTracers(mc, path, color, time, index);
	}

	private static void emitTracers(Minecraft mc, Vec3[] path, float[] color, double time, int seed) {
		double[] lengths = new double[path.length];
		double total = 0.0;
		for (int i = 1; i < path.length; i++) {
			total += path[i].distanceTo(path[i - 1]);
			lengths[i] = total;
		}
		if (total < 1.0E-3) return;
		for (int sub = 0; sub < TRACER_SUBSTEPS; sub++) {
			double t = time + (double) sub / TRACER_SUBSTEPS;
			for (int k = 0; k < TRACERS_PER_WAY; k++) {
				double offset = (double) k / TRACERS_PER_WAY + seed * 0.137;
				double forward = Mth.frac(t * TRACER_SPEED + offset);
				double backward = 1.0 - Mth.frac(t * TRACER_RETURN_SPEED + offset + 0.5 / TRACERS_PER_WAY);
				spawn(mc, pointAt(path, lengths, total * forward), Vec3.ZERO, color, TRACER_SCALE, TRACER_LIFETIME);
				spawn(mc, pointAt(path, lengths, total * backward), Vec3.ZERO, color, TRACER_SCALE, TRACER_LIFETIME);
			}
		}
	}

	private static Vec3 pointAt(Vec3[] path, double[] lengths, double distance) {
		for (int i = 1; i < path.length; i++) {
			if (distance > lengths[i] && i < path.length - 1) continue;
			double segment = lengths[i] - lengths[i - 1];
			double f = segment > 1.0E-6 ? Mth.clamp((distance - lengths[i - 1]) / segment, 0.0, 1.0) : 0.0;
			return path[i - 1].lerp(path[i], f);
		}
		return path[path.length - 1];
	}

	private static boolean isOnRight(Player player, Vec3 point) {
		float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
		return (point.x - player.getX()) * -Mth.cos(yaw) + (point.z - player.getZ()) * -Mth.sin(yaw) >= 0.0;
	}

	private static float[] auraColor(StatsData data) {
		return FormVisualTransition.color(data, data.getCharacter().getRgbAuraColor(), FormConfig.FormData::getRgbAuraColor);
	}

	private static float[] ritualColor(StatsData data, KiSharePose.Ring ring) {
		float[] own = auraColor(data);
		FormConfig.FormData target = GodRitualHelper.formData();
		if (ring.center() == null || target == null || target.getRgbAuraColor() == null) return own;
		StatsData centerData = StatsProvider.get(StatsCapability.INSTANCE, ring.center()).orElse(null);
		float progress = centerData != null ? Mth.clamp(centerData.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f) : 0.0f;
		float[] god = target.getRgbAuraColor();
		return new float[]{Mth.lerp(progress, own[0], god[0]), Mth.lerp(progress, own[1], god[1]), Mth.lerp(progress, own[2], god[2])};
	}

	private static double jitter(RandomSource random) {
		return (random.nextDouble() - 0.5) * 2.0 * STREAM_JITTER;
	}

	private static void spawn(Minecraft mc, Vec3 pos, Vec3 velocity, float[] color, float scale, int lifetime) {
		Particle particle = mc.particleEngine.createParticle(MainParticles.KI_TRAIL.get(), pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
		if (!(particle instanceof KiTrailParticle trail)) return;
		trail.setKiColor(color[0], color[1], color[2]);
		trail.setKiScale(scale);
		if (lifetime > 0) trail.setLifetime(lifetime);
	}
}
