package com.dragonminez.common.init.entities.dragon;

import com.dragonminez.client.gui.WishesScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.server.events.DragonBallsHandler;
import com.dragonminez.server.world.data.DragonBallSavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.jspecify.annotations.NonNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class DragonWishEntity extends Mob implements GeoEntity {
	private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(DragonWishEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> GRANTED_WISH = SynchedEntityData.defineId(DragonWishEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<String> DRAGON_DEFINITION_ID = SynchedEntityData.defineId(DragonWishEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> DESPAWN_FADE = SynchedEntityData.defineId(DragonWishEntity.class, EntityDataSerializers.INT);
	public static final int FADE_TICKS = 50;

	private long invokingTime;
	private long summonExpiresAt;
	private int despawnDelay = 20 * 5;
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
	private final String defaultDragonDefinitionId;

	public DragonWishEntity(EntityType<? extends Mob> entityType, Level level, String dragonDefinitionId) {
		super(entityType, level);
		this.defaultDragonDefinitionId = dragonDefinitionId;
		this.noCulling = true;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 1000.0D)
				.add(Attributes.MOVEMENT_SPEED, 2.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(OWNER_NAME, "");
		this.entityData.define(GRANTED_WISH, false);
		this.entityData.define(DRAGON_DEFINITION_ID, defaultDragonDefinitionId == null ? "" : defaultDragonDefinitionId);
		this.entityData.define(DESPAWN_FADE, -1);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 35.0f));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel serverLevel && !this.isRemoved() && this.tickCount % 20 == 0) checkSummon(serverLevel);
		if (this.level().isClientSide) {
			if (isFading()) spawnFadeParticles();
			return;
		}
		if (hasGrantedWish() && despawnDelay-- <= FADE_TICKS) beginDespawn();
		int fade = this.entityData.get(DESPAWN_FADE);
		if (fade < 0) return;
		if (fade <= 0) this.discard();
		else this.entityData.set(DESPAWN_FADE, fade - 1);
	}

	public void beginDespawn() {
		if (this.entityData.get(DESPAWN_FADE) < 0) this.entityData.set(DESPAWN_FADE, FADE_TICKS);
	}

	public boolean isFading() {
		return this.entityData.get(DESPAWN_FADE) >= 0;
	}

	public float getFadeProgress(float partialTick) {
		int fade = this.entityData.get(DESPAWN_FADE);
		if (fade < 0) return 0.0F;
		return Mth.clamp((FADE_TICKS - fade + partialTick) / FADE_TICKS, 0.0F, 1.0F);
	}

	private void spawnFadeParticles() {
		float progress = getFadeProgress(0.0F);
		int count = 2 + (int) (progress * 6);
		for (int i = 0; i < count; i++) {
			double x = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth() * 1.5D;
			double y = this.getY() + this.random.nextDouble() * this.getBbHeight() * 1.5D;
			double z = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth() * 1.5D;
			this.level().addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.04D + this.random.nextDouble() * 0.08D, 0.0D);
		}
	}

	private void checkSummon(ServerLevel serverLevel) {
		DragonBallSavedData data = DragonBallSavedData.get(serverLevel);
		DragonBallSavedData.Summon summon = data.getSummon(this.getUUID());
		if (summon == null) {
			if (this.summonExpiresAt > 0) {
				beginDespawn();
				return;
			}
			DragonDefinition definition = getDragonDefinition();
			this.summonExpiresAt = serverLevel.getGameTime() + DragonBallsHandler.DRAGON_WAIT_TICKS;
			DragonBallsHandler.registerSummon(serverLevel, this, definition == null ? null : definition.getBallSetId());
			return;
		}
		if (!hasGrantedWish() && serverLevel.getGameTime() >= summon.expiresAt()) beginDespawn();
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public @NonNull InteractionResult mobInteract(@NonNull Player player, @NonNull InteractionHand hand) {
		DragonDefinition definition = getDragonDefinition();
		if (definition != null && this.level().isClientSide && this.getOwnerName().equals(player.getName().getString())) {
			if (!this.hasGrantedWish() && Minecraft.getInstance().player != null && Minecraft.getInstance().player.equals(player)) {
				Minecraft.getInstance().setScreen(new WishesScreen(this.getId(), definition.getWishScreenId(), definition.getWishCount()));
				Minecraft.getInstance().player.playSound(MainSounds.UI_MENU_SWITCH.get());
			}
		}
		return super.mobInteract(player, hand);
	}

	private boolean despawnHandled = false;

	@Override
	public void remove(@NonNull RemovalReason reason) {
		if (!this.level().isClientSide && reason.shouldDestroy() && !this.despawnHandled) {
			this.despawnHandled = true;
			onDespawn();
		}
		super.remove(reason);
	}

	private void onDespawn() {
		DragonDefinition definition = getDragonDefinition();
		if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
			DragonBallSavedData.Summon summon = DragonBallSavedData.get(serverLevel).removeSummon(this.getUUID());
			if (summon == null && this.summonExpiresAt > 0) return;

			serverLevel.setWeatherParameters(6000, 0, false, false);
			serverLevel.setDayTime(this.getInvokingTime());

			String ballSetId = definition != null ? definition.getBallSetId() : summon != null ? summon.setId() : null;
			if (ConfigManager.getServerConfig().getWorldGen().getGenerateDragonBalls()) {
				if (ballSetId != null && !ballSetId.isBlank()) {
					DragonBallsHandler.scatterDragonBalls(serverLevel, ballSetId);
				}
				ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayerByName(this.getOwnerName());
				if (owner != null) {
					DragonBallsHandler.syncRadar(owner.serverLevel());
				}
			}
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
		animationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
		return PlayState.CONTINUE;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC) || source.is(DamageTypes.GENERIC_KILL)) {
			return super.hurt(source, amount);
		}
		return false;
	}

	@Override
	public boolean canBeCollidedWith() { return false; }
	@Override
	public boolean canCollideWith(Entity entity) { return false; }
	@Override
	public boolean canBeHitByProjectile() { return false; }
	@Override
	public void push(Entity entity) { }
	@Override
	public boolean isPushable() { return false; }
	@Override
	protected void doPush(Entity entity) { }
	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

	public void setOwnerName(String name) { this.entityData.set(OWNER_NAME, name); }
	public String getOwnerName() { return this.entityData.get(OWNER_NAME); }
	public void setGrantedWish(boolean granted) { this.entityData.set(GRANTED_WISH, granted); }
	public boolean hasGrantedWish() { return this.entityData.get(GRANTED_WISH); }
	public void setInvokingTime(long time) { this.invokingTime = time; }
	public void setSummonExpiresAt(long time) { this.summonExpiresAt = time; }
	public long getSummonExpiresAt() { return this.summonExpiresAt; }
	public long getInvokingTime() { return this.invokingTime; }
	public void setDragonDefinitionId(String id) { this.entityData.set(DRAGON_DEFINITION_ID, id); }
	public String getDragonDefinitionId() { return this.entityData.get(DRAGON_DEFINITION_ID); }

	public float getRenderScale() {
		return switch (getDragonDefinitionId()) {
			case "shenron", "porunga" -> 3.5F;
			case "porunga_daima" -> 7.0F;
			default -> 1.5F;
		};
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double range = 128.0D * getRenderScale();
		return distance < range * range;
	}

	public DragonDefinition getDragonDefinition() {
		String definitionId = getDragonDefinitionId();
		if (definitionId == null || definitionId.isBlank()) {
			var key = ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
			definitionId = key == null ? defaultDragonDefinitionId : key.getPath();
		}
		return DragonBallDefinitions.getDragon(definitionId);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putLong("InvokingTime", this.invokingTime);
		compound.putLong("SummonExpiresAt", this.summonExpiresAt);
		compound.putInt("DespawnDelay", this.despawnDelay);
		compound.putInt("DespawnFade", this.entityData.get(DESPAWN_FADE));
		compound.putString("OwnerName", this.getOwnerName());
		compound.putBoolean("GrantedWish", this.hasGrantedWish());
		compound.putString("DragonDefinitionId", this.getDragonDefinitionId());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("InvokingTime")) this.invokingTime = compound.getLong("InvokingTime");
		if (compound.contains("SummonExpiresAt")) this.summonExpiresAt = compound.getLong("SummonExpiresAt");
		if (compound.contains("DespawnDelay")) this.despawnDelay = compound.getInt("DespawnDelay");
		if (compound.contains("DespawnFade")) this.entityData.set(DESPAWN_FADE, compound.getInt("DespawnFade"));
		if (compound.contains("OwnerName")) this.setOwnerName(compound.getString("OwnerName"));
		if (compound.contains("GrantedWish")) this.setGrantedWish(compound.getBoolean("GrantedWish"));
		if (compound.contains("DragonDefinitionId")) this.setDragonDefinitionId(compound.getString("DragonDefinitionId"));
	}
}
