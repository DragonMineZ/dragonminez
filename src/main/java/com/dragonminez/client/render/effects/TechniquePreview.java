package com.dragonminez.client.render.effects;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.animation.KiAnimations;
import com.dragonminez.client.render.camera.TechniquePreviewCamera;
import com.dragonminez.client.render.util.KiTrailRenderer;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiAreaEntity;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiDiskEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class TechniquePreview {
	private enum Phase { CHARGE, FIRE, REST }

	private static final int PREVIEW_ID_BASE = -2_000_000;
	private static final int PREVIEW_ID_SPAN = 4096;
	private static final int INSTANT_CHARGE_TICKS = 10;
	private static final int MIN_CHARGE_TICKS = 24;
	private static final int MAX_CHARGE_TICKS = 60;
	private static final int REST_TICKS = 16;
	private static final float MAX_BEAM_LENGTH = 30.0F;
	private static final int SMALL_BALL_SHOTS = 3;
	private static final int SMALL_BALL_INTERVAL = 10;
	private static final int SMALL_BALL_LIFE = 30;
	private static final int BARRAGE_BULLETS = 3;
	private static final int BARRAGE_INTERVAL = 2;
	private static final int BARRAGE_BULLET_LIFE = 18;
	private static final float BARRAGE_SPREAD = 6.0F;
	private static final int IMPACT_LIFE = 25;
	private static final float MIN_IMPACT_RADIUS = 1.5F;
	private static final float LIFT_GRAVITY = 0.08F;
	private static final float LIFT_DRAG = 0.98F;

	private static final class Piece {
		private final Entity entity;
		private int expiresAt;
		private boolean flying;
		private boolean explodes;
		private final boolean coreOnly;

		private Piece(Entity entity, int expiresAt, boolean flying, boolean explodes, boolean coreOnly) {
			this.entity = entity;
			this.expiresAt = expiresAt;
			this.flying = flying;
			this.explodes = explodes;
			this.coreOnly = coreOnly;
		}
	}

	private record OwnerState(double x, double y, double z, Vec3 motion, float fallDistance, boolean hasImpulse, float xRot, float xRotO) {
		private static OwnerState capture(LocalPlayer player) {
			return new OwnerState(player.getX(), player.getY(), player.getZ(), player.getDeltaMovement(),
					player.fallDistance, player.hasImpulse, player.getXRot(), player.xRotO);
		}

		private void restore(LocalPlayer player) {
			if (player.getX() != x || player.getY() != y || player.getZ() != z) player.setPos(x, y, z);
			player.setDeltaMovement(motion);
			player.fallDistance = fallDistance;
			player.hasImpulse = hasImpulse;
			player.setXRot(xRot);
			player.xRotO = xRotO;
		}
	}

	private static boolean active = false;
	private static KiAttackData spec;
	private static Phase phase = Phase.REST;
	private static int phaseTicks;
	private static Piece main;
	private static final List<Piece> PIECES = new ArrayList<>();
	private static final List<Piece> IMPACTS = new ArrayList<>();
	private static int idCursor;
	private static int shotsFired;
	private static float beamLimit;
	private static boolean beamBlocked;
	private static String playingAnimation;
	private static float lift;
	private static float liftO;
	private static float fallSpeed;

	private static boolean poseOverridden;
	private static float savedXRot;
	private static float savedXRotO;
	private static float savedBodyRot;
	private static float savedBodyRotO;
	private static float savedHeadRot;
	private static float savedHeadRotO;

	private TechniquePreview() {
	}

	public static boolean isActive() {
		return active;
	}

	public static void show(KiAttackData data) {
		boolean restart = !active || spec == null || spec.getKiType() != data.getKiType()
				|| spec.getSize() != data.getSize() || spec.getSpeed() != data.getSpeed();
		spec = data;
		active = true;
		if (restart) restartCycle();
		else recolor();
	}

	public static void hide() {
		if (!active) return;
		clearAll();
		stopAnimation();
		active = false;
		spec = null;
		phase = Phase.REST;
		resetLift();
	}

	public static float modelLift(Entity entity, float partialTick) {
		if (!active || entity != Minecraft.getInstance().player) return 0.0F;
		return Mth.lerp(partialTick, liftO, lift);
	}

	public static double frameDistance(LocalPlayer player) {
		if (spec == null) return 4.5;
		float size = castSize(player);
		double base = switch (spec.getKiType()) {
			case GIANT_BALL -> 3.0 + size * 0.85;
			case EXPLOSION -> 6.0 + Math.max(size, 10.0F) * 0.9;
			case MEDIUM_BALL -> 5.5 + size * 0.8;
			case WAVE, BEAM, LASER -> 6.0 + size * 0.8;
			case BARRAGE -> 5.0 + size * 1.5;
			case AREA -> 4.5 + size * 1.8;
			case SMALL_BALL -> 4.5 + size * 0.3;
			default -> 4.6 + size * 0.4;
		};
		return base * AbstractKiProjectile.ownerScaleOf(player);
	}

	public static double frameHeight(LocalPlayer player) {
		double body = player.getBbHeight();
		if (spec == null) return body * 0.62;
		float scale = AbstractKiProjectile.ownerScaleOf(player);
		float size = castSize(player);
		return switch (spec.getKiType()) {
			case GIANT_BALL -> (body + 0.6 * scale + size) * 0.5;
			case MEDIUM_BALL -> (body * 0.62 + Math.max(body * 0.5 + 0.5 * scale, size * 0.5 + 0.1)) * 0.5;
			case AREA -> body * 0.45;
			default -> body * 0.62;
		};
	}

	public static double frameLead(LocalPlayer player) {
		if (spec == null) return 0.0;
		return switch (spec.getKiType()) {
			case MEDIUM_BALL -> castSize(player) * 0.5;
			default -> 0.0;
		};
	}

	public static void tick() {
		if (!active || spec == null) return;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			hide();
			return;
		}
		StatsData stats = stats(player);
		if (stats == null) return;

		OwnerState owner = OwnerState.capture(player);
		player.setXRot(0.0F);
		player.xRotO = 0.0F;
		liftO = lift;
		try {
			advance(mc.level, player, stats);
		} catch (RuntimeException e) {
			LogUtil.error(Env.CLIENT, "Technique preview stopped for {}: {}", spec.getKiType(), e.toString());
			clearAll();
			stopAnimation();
			active = false;
		} finally {
			owner.restore(player);
		}
		updateLift(player);
	}

	private static void advance(ClientLevel level, LocalPlayer player, StatsData stats) {
		tickPieces(level, player);
		phaseTicks++;
		switch (phase) {
			case CHARGE -> {
				if (phaseTicks == 1) beginCharge(level, player, stats);
				if (phaseTicks >= chargeTicks()) {
					phase = Phase.FIRE;
					phaseTicks = 0;
					beginFire(level, player);
					updateFire(level, player, stats);
				}
			}
			case FIRE -> {
				updateFire(level, player, stats);
				if (phaseTicks >= fireTicks()) endFire(level);
			}
			case REST -> {
				if (phaseTicks >= REST_TICKS) {
					phase = Phase.CHARGE;
					phaseTicks = 0;
				}
			}
		}
		syncAnimation(player);
	}

	private static void updateLift(LocalPlayer player) {
		if (main != null && main.entity instanceof KiExplosionEntity explosion && !explosion.isRemoved()) {
			lift = (float) Math.max(0.0, explosion.getY() - (player.getY() + player.getBbHeight() * 0.5));
			fallSpeed = 0.0F;
			return;
		}
		if (lift <= 0.0F) return;
		fallSpeed = (fallSpeed + LIFT_GRAVITY) * LIFT_DRAG;
		lift = Math.max(0.0F, lift - fallSpeed);
	}

	private static void resetLift() {
		lift = 0.0F;
		liftO = 0.0F;
		fallSpeed = 0.0F;
	}

	private static void restartCycle() {
		clearAll();
		stopAnimation();
		resetLift();
		phase = Phase.CHARGE;
		phaseTicks = 0;
	}

	private static int chargeTicks() {
		int base = spec.getBaseChargeTicks();
		if (base <= 0) return INSTANT_CHARGE_TICKS;
		return Mth.clamp(base, MIN_CHARGE_TICKS, MAX_CHARGE_TICKS);
	}

	private static int fireTicks() {
		return switch (spec.getKiType()) {
			case SMALL_BALL -> SMALL_BALL_SHOTS * SMALL_BALL_INTERVAL + 20;
			case WAVE, LASER, BEAM, GIANT_BALL -> 45;
			case MEDIUM_BALL, DISK -> 35;
			case EXPLOSION -> 40;
			case BARRAGE -> 36;
			case SHIELD, AREA -> 50;
		};
	}

	private static float castSize(LocalPlayer player) {
		StatsData stats = stats(player);
		return stats == null ? spec.getSize() : spec.resolveCastSize(stats);
	}

	private static float kiSpeed(StatsData stats) {
		return (float) (spec.getSpeed() * stats.getKiAttackSpeedModifier());
	}

	private static void beginCharge(ClientLevel level, LocalPlayer player, StatsData stats) {
		float size = spec.resolveCastSize(stats);
		float speed = kiSpeed(stats);
		int core = spec.getColorInterior();
		int border = spec.getColorExterior();
		int outline = spec.getColorOutline();
		int charge = chargeTicks();
		shotsFired = 0;

		AbstractKiProjectile entity = switch (spec.getKiType()) {
			case SMALL_BALL -> null;
			case MEDIUM_BALL -> {
				KiBlastEntity ball = new KiBlastEntity(level, player);
				ball.setupKiBlastPlayer(player, 0.0F, speed, core, border, outline, size);
				ball.setCastTime(charge);
				yield ball;
			}
			case GIANT_BALL -> {
				KiBlastEntity ball = new KiBlastEntity(level, player);
				ball.setupKiLargeBlastPlayer(player, 0.0F, speed, core, border, outline, size);
				ball.setCastTime(charge);
				yield ball;
			}
			case WAVE -> {
				KiWaveEntity wave = new KiWaveEntity(level, player);
				wave.setupKiWavePlayer(player, 0.0F, speed, core, border, outline, size);
				wave.setCastWave(charge);
				yield wave;
			}
			case LASER -> {
				KiLaserEntity laser = new KiLaserEntity(level, player);
				laser.setupKiLaserPlayer(player, 0.0F, speed, core, border, outline, size);
				laser.setCastTime(charge);
				yield laser;
			}
			case BEAM -> {
				KiLaserEntity beam = new KiLaserEntity(level, player);
				beam.setupKiBeamPlayer(player, 0.0F, speed, core, border, outline, size);
				beam.setCastTime(charge);
				yield beam;
			}
			case DISK -> {
				KiDiskEntity disk = new KiDiskEntity(level, player);
				disk.setupKiDiskPlayer(player, 0.0F, speed, core, border, outline, size);
				disk.setCastTime(charge);
				yield disk;
			}
			case SHIELD -> {
				KiBarrierEntity barrier = new KiBarrierEntity(level, player);
				barrier.setupBarrierPlayer(player, 0.0F, size, core, border, outline);
				yield barrier;
			}
			case EXPLOSION -> {
				KiExplosionEntity explosion = new KiExplosionEntity(level, player);
				explosion.setupExplosionPlayer(player, 0.0F, size, core, border, outline);
				yield explosion;
			}
			case BARRAGE -> {
				KiBlastEntity volley = new KiBlastEntity(level, player);
				volley.setupKiVolleyPlayer(player, 0.0F, speed, core, 40, size);
				volley.setColors(core, border, outline);
				volley.setFiring(false);
				volley.setMaxLife(99999);
				yield volley;
			}
			case AREA -> {
				KiAreaEntity area = new KiAreaEntity(level, player);
				area.setupAreaPlayer(player, 0.0F, size * 1.5F, core, border, outline);
				area.setFiring(false);
				area.setMaxLife(99999);
				yield area;
			}
		};

		if (entity == null) return;
		entity.setKiType(spec.getKiType().ordinal());
		main = add(entity, Integer.MAX_VALUE, false, false, false, PIECES);
	}

	private static void beginFire(ClientLevel level, LocalPlayer player) {
		beamBlocked = false;
		beamLimit = MAX_BEAM_LENGTH;
		if (main == null) return;

		int fire = fireTicks();
		Entity entity = main.entity;
		main.expiresAt = entity.tickCount + fire + 1;

		if (entity instanceof KiBlastEntity ball) {
			ball.fireHability(fire);
			if (ball.getKiRenderType() == 9) return;
			if (spec.getKiType() == KiAttackData.KiType.MEDIUM_BALL) {
				ball.setDeltaMovement(player.getLookAngle().scale(ball.getKiSpeed()));
			}
			main.flying = true;
			main.explodes = true;
		} else if (entity instanceof KiWaveEntity wave) {
			wave.fireHability(fire);
			measureBeam(level, wave, wave.getXRot(), wave.getYRot());
		} else if (entity instanceof KiLaserEntity laser) {
			laser.fireHability(fire);
			measureBeam(level, laser, laser.getFixedPitch(), laser.getFixedYaw());
		} else if (entity instanceof KiDiskEntity disk) {
			disk.fireHability(fire);
			main.flying = true;
		} else if (entity instanceof KiBarrierEntity barrier) {
			barrier.fireHability(fire);
		} else if (entity instanceof KiExplosionEntity explosion) {
			explosion.fireHability(fire);
		} else if (entity instanceof KiAreaEntity area) {
			area.fireHability(fire);
		}
	}

	private static void updateFire(ClientLevel level, LocalPlayer player, StatsData stats) {
		KiAttackData.KiType type = spec.getKiType();
		if (type == KiAttackData.KiType.SMALL_BALL) {
			if (shotsFired < SMALL_BALL_SHOTS && phaseTicks % SMALL_BALL_INTERVAL == 0) {
				spawnSmallBall(level, player, stats);
				shotsFired++;
			}
			return;
		}
		if (type == KiAttackData.KiType.BARRAGE) {
			if (phaseTicks % BARRAGE_INTERVAL == 0 && phaseTicks < fireTicks() - BARRAGE_BULLET_LIFE / 2) {
				for (int i = 0; i < BARRAGE_BULLETS; i++) spawnBarrageBullet(level, player, stats);
			}
			return;
		}
		if (main == null) return;

		Entity entity = main.entity;
		if (entity instanceof KiWaveEntity wave) {
			wave.setBeamLength(Math.min(beamLimit, wave.getBeamLength() + wave.getKiSpeed()));
		} else if (entity instanceof KiLaserEntity laser) {
			laser.setBeamLength(Math.min(beamLimit, laser.getBeamLength() + laser.getKiSpeed()));
		} else if (entity instanceof KiBarrierEntity barrier) {
			float shown = barrier.tickCount - barrier.getFireTick();
			float size = barrier.getSize();
			barrier.setCurrentSize(Math.min(size, 1.0F + size * shown / 10.0F));
		}
	}

	private static void endFire(ClientLevel level) {
		if (main != null) {
			Entity entity = main.entity;
			if (entity instanceof KiWaveEntity wave) {
				impactAtBeamTip(level, wave, wave.getXRot(), wave.getYRot(), wave.getBeamLength(), 1.8F);
			} else if (entity instanceof KiLaserEntity laser && beamBlocked) {
				impactAtBeamTip(level, laser, laser.getFixedPitch(), laser.getFixedYaw(), laser.getBeamLength(), 0.6F);
			}
		}
		for (Piece piece : PIECES) {
			if (piece.flying && piece.explodes && piece.entity instanceof AbstractKiProjectile projectile) impact(level, projectile, center(projectile), 0.25F);
			forget(piece.entity);
		}
		PIECES.clear();
		main = null;
		phase = Phase.REST;
		phaseTicks = 0;
	}

	private static void spawnSmallBall(ClientLevel level, LocalPlayer player, StatsData stats) {
		KiBlastEntity ball = new KiBlastEntity(level, player);
		ball.setOwner(player);
		ball.setKiType(spec.getKiType().ordinal());
		ball.setKiRenderType(0);
		ball.setSize(spec.resolveCastSize(stats));
		ball.setKiSpeed(kiSpeed(stats));
		ball.setColors(spec.getColorInterior(), spec.getColorExterior(), spec.getColorOutline());
		ball.setCastTime(0);
		ball.setMaxLife(SMALL_BALL_LIFE);
		ball.setFiring(true);

		Vec3 look = player.getLookAngle();
		Vec3 spawn = player.getEyePosition().add(look.scale(0.5D));
		ball.setPos(spawn.x, spawn.y - 0.2D, spawn.z);
		ball.setDeltaMovement(look.scale(ball.getKiSpeed()));
		ball.setYRot(player.getYRot());
		ball.setXRot(player.getXRot());
		add(ball, SMALL_BALL_LIFE, true, true, false, PIECES);
	}

	private static void spawnBarrageBullet(ClientLevel level, LocalPlayer player, StatsData stats) {
		KiBlastEntity bullet = new KiBlastEntity(level, player);
		bullet.setupKiSmall(player, 0.0F, kiSpeed(stats), spec.getColorInterior(), spec.getColorOutline());
		bullet.setKiType(spec.getKiType().ordinal());
		bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, bullet.getKiSpeed(), BARRAGE_SPREAD);
		add(bullet, BARRAGE_BULLET_LIFE, true, false, true, PIECES);
	}

	private static Piece add(Entity entity, int life, boolean flying, boolean explodes, boolean coreOnly, List<Piece> into) {
		idCursor = (idCursor + 1) % PREVIEW_ID_SPAN;
		entity.setId(PREVIEW_ID_BASE - idCursor);
		entity.setOldPosAndRot();
		int expiresAt = life == Integer.MAX_VALUE ? Integer.MAX_VALUE : entity.tickCount + life;
		Piece piece = new Piece(entity, expiresAt, flying, explodes, coreOnly);
		into.add(piece);
		return piece;
	}

	private static void tickPieces(ClientLevel level, LocalPlayer player) {
		Iterator<Piece> pieces = PIECES.iterator();
		while (pieces.hasNext()) {
			Piece piece = pieces.next();
			Entity entity = piece.entity;
			Vec3 before = entity instanceof AbstractKiProjectile projectile ? center(projectile) : entity.position();
			entity.setOldPosAndRot();
			entity.tickCount++;
			entity.tick();

			boolean expired = entity.tickCount >= piece.expiresAt || entity.isRemoved();
			if (!expired && piece.flying && entity instanceof AbstractKiProjectile projectile) {
				Vec3 after = center(projectile);
				BlockHitResult hit = level.clip(new ClipContext(before, after, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
				if (hit.getType() != HitResult.Type.MISS) {
					impact(level, projectile, hit.getLocation(), 0.25F);
					expired = true;
				} else if (piece.explodes && entity.tickCount >= piece.expiresAt - 1) {
					impact(level, projectile, after, 0.25F);
					expired = true;
				}
			}
			if (!expired) continue;
			forget(entity);
			pieces.remove();
			if (piece == main) main = null;
		}

		Iterator<Piece> impacts = IMPACTS.iterator();
		while (impacts.hasNext()) {
			Piece piece = impacts.next();
			piece.entity.setOldPosAndRot();
			piece.entity.tickCount++;
			if (piece.entity.tickCount < piece.expiresAt) continue;
			forget(piece.entity);
			impacts.remove();
		}
	}

	private static void measureBeam(ClientLevel level, Entity beam, float pitch, float yaw) {
		Vec3 start = beam.position();
		Vec3 end = start.add(Vec3.directionFromRotation(pitch, yaw).scale(MAX_BEAM_LENGTH));
		BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, beam));
		if (hit.getType() == HitResult.Type.MISS) return;
		beamBlocked = true;
		beamLimit = (float) Math.min(MAX_BEAM_LENGTH, hit.getLocation().distanceTo(start) + 0.1);
	}

	private static void impactAtBeamTip(ClientLevel level, AbstractKiProjectile beam, float pitch, float yaw, float length, float visualFactor) {
		Vec3 tip = beam.position().add(Vec3.directionFromRotation(pitch, yaw).scale(length));
		impact(level, beam, tip, visualFactor);
	}

	private static void impact(ClientLevel level, AbstractKiProjectile source, Vec3 at, float visualFactor) {
		float size = Math.max(source.getSize(), 0.1F);
		KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), level);
		visual.setPos(at.x, at.y, at.z);
		visual.setupExplosion(source.getColor(), source.getColorBorder(), source.getColorOutline(), size * visualFactor);
		add(visual, IMPACT_LIFE, false, false, false, IMPACTS);
		float radius = Math.max(size * 0.5F, MIN_IMPACT_RADIUS);
		level.addParticle(MainParticles.KI_EXPLOSION.get(), at.x, at.y, at.z, radius * 1.8F, 0.0D, 0.0D);
	}

	private static Vec3 center(AbstractKiProjectile projectile) {
		return projectile.position().add(0.0D, projectile.getBbHeight() * 0.5D, 0.0D);
	}

	private static void recolor() {
		int core = spec.getColorInterior();
		int border = spec.getColorExterior();
		int outline = spec.getColorOutline();
		for (Piece piece : PIECES) {
			if (piece.entity instanceof AbstractKiProjectile projectile) {
				projectile.setColors(core, piece.coreOnly ? core : border, outline);
			}
		}
	}

	private static void clearAll() {
		for (Piece piece : PIECES) forget(piece.entity);
		for (Piece piece : IMPACTS) forget(piece.entity);
		PIECES.clear();
		IMPACTS.clear();
		main = null;
	}

	private static void forget(Entity entity) {
		KiTrailRenderer.forget(entity.getId());
	}

	private static void syncAnimation(LocalPlayer player) {
		if (!(player instanceof IPlayerAnimatable animatable)) return;
		String wanted = switch (phase) {
			case CHARGE -> spec.getKiType() == KiAttackData.KiType.LASER ? null : KiAnimations.resolve(spec.getAnimationPrefix() + "_cast");
			case FIRE -> KiAnimations.resolve(spec.getAnimationPrefix() + "_fire");
			case REST -> null;
		};
		if (wanted == null) {
			stopAnimation();
			return;
		}
		if (!wanted.equals(playingAnimation) || phase == Phase.CHARGE) {
			animatable.dragonminez$playKiAnimation(wanted, holdsPose());
			playingAnimation = wanted;
		}
	}

	private static boolean holdsPose() {
		return switch (spec.getKiType()) {
			case GIANT_BALL, WAVE, BEAM, EXPLOSION, BARRAGE -> true;
			default -> false;
		};
	}

	private static void stopAnimation() {
		if (playingAnimation == null) return;
		playingAnimation = null;
		LocalPlayer player = Minecraft.getInstance().player;
		if (!(player instanceof IPlayerAnimatable animatable)) return;
		StatsData stats = stats(player);
		if (stats != null && stats.getTechniques().isTechniqueCharging()) return;
		animatable.dragonminez$stopKiAnimation();
	}

	private static StatsData stats(LocalPlayer player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (!active) return;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) return;

		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
			overridePose(player);
		} else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			restorePose(player);
			enqueue(mc, event.getPoseStack(), event.getPartialTick());
		}
	}

	@SubscribeEvent
	public static void onRenderTick(TickEvent.RenderTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !poseOverridden) return;
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) restorePose(player);
		else poseOverridden = false;
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Pre event) {
		if (TechniquePreviewCamera.isActive()) event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onComputeFov(ViewportEvent.ComputeFov event) {
		if (TechniquePreviewCamera.isActive() && event.usedConfiguredFov()) event.setFOV(TechniquePreviewCamera.fov());
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		hide();
		TechniquePreviewCamera.deactivate();
	}

	private static void overridePose(LocalPlayer player) {
		if (poseOverridden) return;
		savedXRot = player.getXRot();
		savedXRotO = player.xRotO;
		savedBodyRot = player.yBodyRot;
		savedBodyRotO = player.yBodyRotO;
		savedHeadRot = player.yHeadRot;
		savedHeadRotO = player.yHeadRotO;
		float yaw = player.getYRot();
		player.setXRot(0.0F);
		player.xRotO = 0.0F;
		player.yBodyRot = yaw;
		player.yBodyRotO = yaw;
		player.yHeadRot = yaw;
		player.yHeadRotO = yaw;
		poseOverridden = true;
	}

	private static void restorePose(LocalPlayer player) {
		if (!poseOverridden) return;
		player.setXRot(savedXRot);
		player.xRotO = savedXRotO;
		player.yBodyRot = savedBodyRot;
		player.yBodyRotO = savedBodyRotO;
		player.yHeadRot = savedHeadRot;
		player.yHeadRotO = savedHeadRotO;
		poseOverridden = false;
	}

	@SuppressWarnings("unchecked")
	private static void enqueue(Minecraft mc, PoseStack poseStack, float partialTick) {
		if (PIECES.isEmpty() && IMPACTS.isEmpty()) return;
		Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		enqueue(mc, PIECES, camera, poseStack, buffers, partialTick);
		enqueue(mc, IMPACTS, camera, poseStack, buffers, partialTick);
	}

	@SuppressWarnings("unchecked")
	private static void enqueue(Minecraft mc, List<Piece> pieces, Vec3 camera, PoseStack poseStack, MultiBufferSource buffers, float partialTick) {
		for (Piece piece : pieces) {
			Entity entity = piece.entity;
			EntityRenderer<Entity> renderer = (EntityRenderer<Entity>) mc.getEntityRenderDispatcher().getRenderer(entity);
			if (renderer == null) continue;
			poseStack.pushPose();
			poseStack.translate(
					Mth.lerp(partialTick, entity.xOld, entity.getX()) - camera.x,
					Mth.lerp(partialTick, entity.yOld, entity.getY()) - camera.y,
					Mth.lerp(partialTick, entity.zOld, entity.getZ()) - camera.z);
			renderer.render(entity, Mth.lerp(partialTick, entity.yRotO, entity.getYRot()), partialTick, poseStack, buffers, LightTexture.FULL_BRIGHT);
			poseStack.popPose();
		}
	}
}
