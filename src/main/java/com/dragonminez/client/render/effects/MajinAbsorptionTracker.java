package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.ActionMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MajinAbsorptionTracker {

	private static final double ABSORB_RANGE = 8.0;
	private static final float FADE_IN = 0.06f;
	private static final float FADE_OUT = 0.10f;

	private static final float INFLATE = 1.12f;
	private static final float MIN_GROW = 0.25f;
	private static final float BREATH_AMP = 0.18f;
	private static final float WOBBLE_AMP = 0.22f;
	private static final float SPIKE_AMP = 0.55f;
	private static final float ANIM_SPEED = 0.45f;

	private static final int SOUND_INTERVAL = 24;
	private static final Map<UUID, Integer> SOUND_COOLDOWN = new HashMap<>();
	private static final Set<UUID> absorbingThisTick = new HashSet<>();

	private static final float SHADE_AMBIENT = 0.55f;
	private static final float[] LIGHT_DIR = normalize(0.3f, 1.0f, 0.45f);

	private static final int STACKS = 18;
	private static final int SECTORS = 28;

	private static final Map<Integer, AbsorbState> ACTIVE = new HashMap<>();

	private MajinAbsorptionTracker() {
	}

	private static final class AbsorbState {
		float intensity;
		float r = 1.0f;
		float g = 0.4f;
		float b = 0.7f;
		boolean seenThisTick;
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.isPaused()) {
			if (!ACTIVE.isEmpty()) ACTIVE.clear();
			if (!SOUND_COOLDOWN.isEmpty()) SOUND_COOLDOWN.clear();
			return;
		}

		absorbingThisTick.clear();
		for (AbsorbState state : ACTIVE.values()) state.seenThisTick = false;

		for (Player player : mc.level.players()) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null) continue;
			if (!data.getStatus().isActionCharging()) continue;
			if (data.getStatus().getSelectedAction() != ActionMode.RACIAL) continue;

			RaceCharacterConfig race = ConfigManager.getRaceCharacter(data.getCharacter().getRaceName());
			if (race == null || !"majin".equals(race.getRacialSkill())) continue;

			LivingEntity target = findTarget(player);
			if (target == null || target == player) continue;

			float[] body = data.getCharacter().getRgbBodyColor();
			AbsorbState state = ACTIVE.computeIfAbsent(target.getId(), id -> new AbsorbState());
			state.seenThisTick = true;
			if (body != null && body.length >= 3) {
				state.r = body[0];
				state.g = body[1];
				state.b = body[2];
			}

			playAbsorbSound(mc, player);
		}

		SOUND_COOLDOWN.keySet().retainAll(absorbingThisTick);

		ACTIVE.entrySet().removeIf(entry -> {
			AbsorbState state = entry.getValue();
			if (state.seenThisTick) {
				state.intensity = Math.min(1.0f, state.intensity + FADE_IN);
				return false;
			}
			state.intensity -= FADE_OUT;
			return state.intensity <= 0.0f;
		});
	}

	private static void playAbsorbSound(Minecraft mc, Player player) {
		UUID id = player.getUUID();
		absorbingThisTick.add(id);
		int cooldown = SOUND_COOLDOWN.getOrDefault(id, 0);
		if (cooldown > 0) {
			SOUND_COOLDOWN.put(id, cooldown - 1);
			return;
		}
		mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
				MainSounds.MAJIN_ABSORB.get(), SoundSource.PLAYERS, 0.8F, 1.0F, false);
		SOUND_COOLDOWN.put(id, SOUND_INTERVAL);
	}

	private static LivingEntity findTarget(Player player) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = start.add(look.scale(ABSORB_RANGE));
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(ABSORB_RANGE)).inflate(1.0);

		List<Entity> entities = player.level().getEntities(player, searchBox,
				e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable());

		for (Entity entity : entities) {
			AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
			if (box.contains(start) || box.clip(start, end).isPresent()) {
				return (LivingEntity) entity;
			}
		}
		return null;
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
		if (ACTIVE.isEmpty()) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;

		float partialTick = event.getPartialTick();
		Camera camera = event.getCamera();
		Vec3 cam = camera.getPosition();
		PoseStack pose = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = buffers.getBuffer(ModRenderTypes.gooBlob());

		for (Map.Entry<Integer, AbsorbState> entry : ACTIVE.entrySet()) {
			Entity entity = mc.level.getEntity(entry.getKey());
			if (entity == null) continue;
			AbsorbState state = entry.getValue();
			if (state.intensity <= 0.0f) continue;

			Vec3 feet = entity.getPosition(partialTick);
			float time = entity.tickCount + partialTick;

			pose.pushPose();
			pose.translate(feet.x - cam.x, feet.y - cam.y, feet.z - cam.z);
			renderBlob(pose, consumer, time, state, entity.getBbWidth(), entity.getBbHeight());
			pose.popPose();
		}

		buffers.endBatch(ModRenderTypes.gooBlob());
	}

	private static void renderBlob(PoseStack pose, VertexConsumer consumer, float time, AbsorbState state,
	                               float width, float height) {
		float intensity = state.intensity;
		if (intensity <= 0.0f) return;

		float cy = height * 0.5f;
		float t = time * ANIM_SPEED;
		float grow = MIN_GROW + (1.0f - MIN_GROW) * intensity;
		float breath = BREATH_AMP * ((float) Math.sin(t * 0.9) * 0.6f + (float) Math.sin(t * 2.3) * 0.4f);
		float ry = height * 0.5f * INFLATE * grow * (1.0f + breath);
		float rxz = width * 0.5f * INFLATE * grow * (1.0f - breath * 0.5f);
		float wob = WOBBLE_AMP;

		Matrix4f matrix = pose.last().pose();

		float[][][] grid = new float[STACKS + 1][SECTORS + 1][3];
		int[][] colors = new int[STACKS + 1][SECTORS + 1];
		for (int i = 0; i <= STACKS; i++) {
			double lat = Math.PI * i / STACKS;
			double sinLat = Math.sin(lat);
			double cosLat = Math.cos(lat);
			for (int j = 0; j <= SECTORS; j++) {
				double lon = 2.0 * Math.PI * j / SECTORS;
				double dx = sinLat * Math.cos(lon);
				double dy = cosLat;
				double dz = sinLat * Math.sin(lon);

				double lumps = Math.sin(dx * 5.0 + t * 0.9)
						* Math.sin(dy * 4.0 - t * 0.7)
						* Math.sin(dz * 6.0 + t * 1.1);
				double s = Math.sin(dx * 3.0 + dy * 2.5 + dz * 3.5 + t * 2.6);
				double spike = Math.pow(Math.max(0.0, s), 6.0);
				float disp = 1.0f + wob * (float) lumps + SPIKE_AMP * intensity * (float) spike;

				grid[i][j][0] = (float) (dx * rxz * disp);
				grid[i][j][1] = (float) (cy + dy * ry * disp);
				grid[i][j][2] = (float) (dz * rxz * disp);

				float ndl = (float) (dx * LIGHT_DIR[0] + dy * LIGHT_DIR[1] + dz * LIGHT_DIR[2]);
				float shade = SHADE_AMBIENT + (1.0f - SHADE_AMBIENT) * Math.max(0.0f, ndl);
				colors[i][j] = packColor(state.r * shade, state.g * shade, state.b * shade);
			}
		}

		for (int i = 0; i < STACKS; i++) {
			for (int j = 0; j < SECTORS; j++) {
				vertex(consumer, matrix, grid[i][j], colors[i][j]);
				vertex(consumer, matrix, grid[i + 1][j], colors[i + 1][j]);
				vertex(consumer, matrix, grid[i + 1][j + 1], colors[i + 1][j + 1]);

				vertex(consumer, matrix, grid[i][j], colors[i][j]);
				vertex(consumer, matrix, grid[i + 1][j + 1], colors[i + 1][j + 1]);
				vertex(consumer, matrix, grid[i][j + 1], colors[i][j + 1]);
			}
		}
	}

	private static void vertex(VertexConsumer consumer, Matrix4f matrix, float[] p, int argb) {
		consumer.vertex(matrix, p[0], p[1], p[2])
				.color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, 0xFF)
				.endVertex();
	}

	private static int packColor(float r, float g, float b) {
		return (clampByte(r) << 16) | (clampByte(g) << 8) | clampByte(b);
	}

	private static int clampByte(float channel) {
		return Math.max(0, Math.min(255, Math.round(channel * 255)));
	}

	private static float[] normalize(float x, float y, float z) {
		float len = (float) Math.sqrt(x * x + y * y + z * z);
		if (len == 0.0f) return new float[]{0.0f, 1.0f, 0.0f};
		return new float[]{x / len, y / len, z / len};
	}
}
