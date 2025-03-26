package com.yuseix.dragonminez.common.init.entity.custom;

import com.mojang.logging.LogUtils;
import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.init.MainBlocks;
import com.yuseix.dragonminez.common.init.menus.screens.ShenlongMenu;
import com.yuseix.dragonminez.common.network.ModMessages;
import com.yuseix.dragonminez.common.network.S2C.UpdateDragonRadarS2C;
import com.yuseix.dragonminez.common.world.cap.provider.DragonBallGenProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShenlongEntity extends Mob implements GeoEntity {
	private static final Logger LOGGER = LogUtils.getLogger();
	private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
	private long invokingTime;

	private int tiempo = 20 * 5; //Tiempo de desaparicion despues de que los deseos sean 0 (20 ticks = 1 segundo)
	private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(ShenlongEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> DESEOS = SynchedEntityData.defineId(ShenlongEntity.class, EntityDataSerializers.INT);

	public ShenlongEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);

		this.entityData.define(OWNER_NAME, "");
		this.entityData.define(DESEOS, 0); // deseos

	}

	public static AttributeSupplier setAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 25000.0D)
				.add(Attributes.ATTACK_DAMAGE, 10.5f)
				.add(Attributes.ATTACK_SPEED, 0.5f)
				.add(Attributes.MOVEMENT_SPEED, 0.18F).build();
	}

	public void setOwnerName(String name) {
		this.entityData.set(OWNER_NAME, name);
	}

	public String getOwnerName() {
		return this.entityData.get(OWNER_NAME);
	}

	public int getDeseos() {
		return this.entityData.get(DESEOS);
	}

	public void setDeseos(int deseos) {
		this.entityData.set(DESEOS, deseos);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 35.0f));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (this.level().isClientSide) {
			// Verifica que el UUID de esta entidad coincida con el del jugador
			if (this.getOwnerName().equals(player.getName().getString())) {
				//DebugUtils.dmzSout("Nombre coincide con el del jugador");
				if (getDeseos() > 0) {
					if (Minecraft.getInstance().player.equals(player)) {
						Minecraft.getInstance().setScreen(new ShenlongMenu());
					}
				}
			}
		}

		return super.mobInteract(player, hand);
	}

	public void setInvokingTime(long time) {
		this.invokingTime = time;
	}

	public long getInvokingTime() {
		return invokingTime;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));

	}

	@Override
	public void tick() {
		super.tick();

		if (this.getDeseos() == 0) {
			tiempo--;
		}

		if (this.tickCount == 1) dragonBallPositions.clear();

		if (tiempo == 0) {
			this.discard();
		}
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {

		tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.shenlong.idle", Animation.LoopType.LOOP));

		return PlayState.CONTINUE;

	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean canCollideWith(Entity pEntity) {
		return false;
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide && reason == RemovalReason.DISCARDED) {
			onDespawn();
		}
		super.remove(reason);
	}

	private static final List<BlockPos> dragonBallPositions = new ArrayList<>();

	private void onDespawn() {
		if (!DMZGeneralConfig.SHOULD_DBALL_DRAGON_SPAWN.get()) return;
		if (this.level() instanceof ServerLevel serverWorld) {

			serverWorld.setDayTime(getInvokingTime());
			serverWorld.getCapability(DragonBallGenProvider.CAPABILITY).ifPresent(dragonBallsCapability -> {
				dragonBallsCapability.loadFromSavedData(serverWorld);

				if (dragonBallsCapability.hasDragonBalls()) {
					dragonBallsCapability.setHasDragonBalls(false);
				}

				if (!dragonBallsCapability.hasDragonBalls()) {
					spawnDragonBall(serverWorld, MainBlocks.DBALL1_BLOCK.get().defaultBlockState(), 1);
					spawnDragonBall(serverWorld, MainBlocks.DBALL2_BLOCK.get().defaultBlockState(), 2);
					spawnDragonBall(serverWorld, MainBlocks.DBALL3_BLOCK.get().defaultBlockState(), 3);
					spawnDragonBall(serverWorld, MainBlocks.DBALL4_BLOCK.get().defaultBlockState(), 4);
					spawnDragonBall(serverWorld, MainBlocks.DBALL5_BLOCK.get().defaultBlockState(), 5);
					spawnDragonBall(serverWorld, MainBlocks.DBALL6_BLOCK.get().defaultBlockState(), 6);
					spawnDragonBall(serverWorld, MainBlocks.DBALL7_BLOCK.get().defaultBlockState(), 7);

					dragonBallsCapability.setDragonBallPositions(dragonBallPositions);
					ModMessages.sendToClients(new UpdateDragonRadarS2C(dragonBallPositions));
					dragonBallsCapability.setHasDragonBalls(true);
					dragonBallsCapability.saveToSavedData(serverWorld);
				}
			});
		}
	}

	private void spawnDragonBall(ServerLevel serverWorld, BlockState dragonBall, int dBallNum) {
		//Spawn the dragon balls
		BlockPos spawnPos = serverWorld.getSharedSpawnPos();
		Random random = new Random();
		int range = DMZGeneralConfig.DBALL_SPAWN_RANGE.get();

		BlockPos posicionValida; // Posición válida inicializada a 0, 0, 0

		// Generar posición aleatoria dentro de un rango de Xk bloques desde el spawn
		int x = spawnPos.getX() + random.nextInt(range * 2) - range;
		int z = spawnPos.getZ() + random.nextInt(range * 2) - range;

		serverWorld.getChunk(x >> 4, z >> 4); // Cargar el chunk

		// Obtener la altura del terreno en esa posición
		int y = serverWorld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos posiblePos = new BlockPos(x, y, z);

		BlockState belowBlockState = serverWorld.getBlockState(posiblePos.below()); // Bloque debajo de la posición
		BlockPos belowPos = posiblePos.below();

		if (belowBlockState.isAir() || (belowBlockState.getBlock() == Blocks.WATER)) {
			serverWorld.setBlock(belowPos, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
		}
		posicionValida = posiblePos;

		// Place a Dragon Ball block at the generated position
		serverWorld.setBlock(posicionValida, dragonBall, 2);
		LOGGER.info("[Shenron] Dragon Ball [{}] spawned at {}", dBallNum, posicionValida);

		dragonBallPositions.add(posicionValida);
	}

	@Override
	public boolean hurt(DamageSource pSource, float pAmount) {
		if ("discarded".equals(pSource.getMsgId()) || "generic".equals(pSource.getMsgId()) || "out_of_world".equals(pSource.getMsgId()) || "genericKill".equals(pSource.getMsgId())) {
			return super.hurt(pSource, pAmount);
		}
		return false;
	}
}
