package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.ActionMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NamekAssimilationEffect {

	private static final double RANGE = 3.0;
	private static final int CHARGE_TICKS = 80;
	private static final float WHITE_CAP = 0.85f;
	private static final float FADE_BACK = 0.04f;
	private static final float CONSUME_WHITE_STEP = 0.1f;
	private static final int VANISH_TICKS = 14;
	private static final int SURVIVOR_TICKS = 70;
	private static final int LIGHTNING_TICKS = 60;

	private static final Map<Integer, State> ACTIVE = new HashMap<>();
	private static final Map<Integer, Integer> LIGHTNING = new HashMap<>();

	private NamekAssimilationEffect() {
	}

	private static final class State {
		float prev;
		float progress;
		boolean seen;
		int assimilatorId = -1;
		int consumeTicks = -1;
	}

	public static void consume(int targetId, int assimilatorId) {
		State state = ACTIVE.computeIfAbsent(targetId, id -> new State());
		state.assimilatorId = assimilatorId;
		if (state.consumeTicks < 0) state.consumeTicks = 0;
		LIGHTNING.put(assimilatorId, LIGHTNING_TICKS);
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			ACTIVE.clear();
			LIGHTNING.clear();
			return;
		}
		if (mc.isPaused()) return;

		LIGHTNING.replaceAll((id, ticks) -> ticks - 1);
		LIGHTNING.values().removeIf(ticks -> ticks <= 0);

		for (State state : ACTIVE.values()) {
			state.seen = false;
			state.prev = state.progress;
		}

		for (Player player : level.players()) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null) continue;
			if (!data.getStatus().isActionCharging()) continue;
			if (data.getStatus().getSelectedAction() != ActionMode.RACIAL) continue;

			RaceCharacterConfig race = ConfigManager.getRaceCharacter(data.getCharacter().getRaceName());
			if (race == null || !"namekian".equals(race.getRacialSkill())) continue;

			LivingEntity target = findTarget(player);
			if (target == null || target == player || !target.isAlive()) continue;

			State state = ACTIVE.computeIfAbsent(target.getId(), id -> new State());
			state.seen = true;
			state.assimilatorId = player.getId();
			if (state.consumeTicks < 0) state.progress = Math.min(WHITE_CAP, state.progress + WHITE_CAP / CHARGE_TICKS);
		}

		RandomSource random = level.getRandom();
		Iterator<Map.Entry<Integer, State>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, State> entry = it.next();
			State state = entry.getValue();
			Entity entity = level.getEntity(entry.getKey());
			if (entity == null) {
				it.remove();
				continue;
			}

			if (state.consumeTicks >= 0) {
				state.consumeTicks++;
				if (state.progress < WHITE_CAP) {
					state.progress = Math.min(WHITE_CAP, state.progress + CONSUME_WHITE_STEP);
				} else if (state.progress < 1.0f) {
					state.progress = Math.min(1.0f, state.progress + (1.0f - WHITE_CAP) / VANISH_TICKS);
					spawnAbsorbParticles(level, entity, level.getEntity(state.assimilatorId), random);
				}
				if (state.consumeTicks > SURVIVOR_TICKS && entity.isAlive()) it.remove();
				continue;
			}

			if (!state.seen) {
				state.progress -= FADE_BACK;
				if (state.progress <= 0.0f) it.remove();
			}
		}
	}

	private static LivingEntity findTarget(Player player) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = start.add(look.scale(RANGE));
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);

		List<Entity> entities = player.level().getEntities(player, searchBox,
				e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable());

		for (Entity entity : entities) {
			AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
			if (box.contains(start) || box.clip(start, end).isPresent()) return (LivingEntity) entity;
		}
		return null;
	}

	private static Vec3 randomPointIn(Entity entity, RandomSource random) {
		AABB box = entity.getBoundingBox();
		return new Vec3(Mth.lerp(random.nextDouble(), box.minX, box.maxX),
				Mth.lerp(random.nextDouble(), box.minY, box.maxY),
				Mth.lerp(random.nextDouble(), box.minZ, box.maxZ));
	}

	private static void spawnAbsorbParticles(ClientLevel level, Entity entity, Entity assimilator, RandomSource random) {
		Vec3 sink = assimilator != null
				? assimilator.position().add(0.0, assimilator.getBbHeight() * 0.6, 0.0)
				: entity.position().add(0.0, entity.getBbHeight() + 2.0, 0.0);
		for (int i = 0; i < 8; i++) {
			Vec3 pos = randomPointIn(entity, random);
			Vec3 dir = sink.subtract(pos);
			double dist = dir.length();
			Vec3 vel = dist > 1.0e-4 ? dir.scale((0.12 + random.nextDouble() * 0.14) / Math.max(1.0, dist * 0.5)) : Vec3.ZERO;
			level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, vel.x, vel.y + 0.02, vel.z);
		}
	}

	public static boolean hasLightning(Player player) {
		return player != null && LIGHTNING.containsKey(player.getId());
	}

	public static MultiBufferSource wrap(Entity entity, MultiBufferSource source, float partialTick) {
		if (ACTIVE.isEmpty() || DMZSkinLayer.PREVIEW_MODE || IrisCompat.isRenderingShadowPass()) return source;
		if (!ModRenderTypes.hasAssimilationSketchShader() || IrisCompat.isShaderPackInUse()) return source;
		State state = ACTIVE.get(entity.getId());
		if (state == null) return source;
		float progress = Mth.lerp(partialTick, state.prev, state.progress);
		if (progress <= 0.001f) return source;
		return new SketchSource(source, Math.round(progress * 1000.0f));
	}

	private record SketchSource(MultiBufferSource delegate, int encodedProgress) implements MultiBufferSource {
		@Override
		public VertexConsumer getBuffer(RenderType type) {
			RenderType sketch = ModRenderTypes.assimilationSketch(type);
			if (sketch == null) return delegate.getBuffer(type);
			return new SketchConsumer(delegate.getBuffer(sketch), encodedProgress);
		}
	}

	private static final class SketchConsumer implements VertexConsumer {
		private final VertexConsumer delegate;
		private final int encodedProgress;

		private SketchConsumer(VertexConsumer delegate, int encodedProgress) {
			this.delegate = delegate;
			this.encodedProgress = encodedProgress;
		}

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			delegate.vertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			delegate.color(red, green, blue, alpha);
			return this;
		}

		@Override
		public VertexConsumer uv(float u, float v) {
			delegate.uv(u, v);
			return this;
		}

		@Override
		public VertexConsumer overlayCoords(int u, int v) {
			delegate.overlayCoords(encodedProgress, 0);
			return this;
		}

		@Override
		public VertexConsumer uv2(int u, int v) {
			delegate.uv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			delegate.normal(x, y, z);
			return this;
		}

		@Override
		public void endVertex() {
			delegate.endVertex();
		}

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) {
			delegate.defaultColor(red, green, blue, alpha);
		}

		@Override
		public void unsetDefaultColor() {
			delegate.unsetDefaultColor();
		}
	}
}
