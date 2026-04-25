package com.dragonminez.common.init.entities.dragon;

import com.dragonminez.client.gui.WishesScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.server.events.DragonBallsHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

	private long invokingTime;
	private int despawnDelay = 20 * 5;
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
	private final String defaultDragonDefinitionId;

	public DragonWishEntity(EntityType<? extends Mob> entityType, Level level, String dragonDefinitionId) {
		super(entityType, level);
		this.defaultDragonDefinitionId = dragonDefinitionId;
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
		if (hasGrantedWish()) despawnDelay--;
		if (despawnDelay <= 0) this.discard();
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public @NonNull InteractionResult mobInteract(@NonNull Player player, @NonNull InteractionHand hand) {
		DragonDefinition definition = getDragonDefinition();
		if (definition != null && this.level().isClientSide && this.getOwnerName().equals(player.getName().getString())) {
			if (!this.hasGrantedWish() && Minecraft.getInstance().player != null && Minecraft.getInstance().player.equals(player)) {
				Minecraft.getInstance().setScreen(new WishesScreen(definition.getWishScreenId(), definition.getWishCount()));
				Minecraft.getInstance().player.playSound(MainSounds.UI_MENU_SWITCH.get());
			}
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public void remove(@NonNull RemovalReason reason) {
		if (!this.level().isClientSide && reason == RemovalReason.DISCARDED) {
			onDespawn();
		}
		super.remove(reason);
	}

	private void onDespawn() {
		DragonDefinition definition = getDragonDefinition();
		if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
			serverLevel.setWeatherParameters(6000, 0, false, false);
			serverLevel.setDayTime(this.getInvokingTime());

			if (definition != null && ConfigManager.getServerConfig().getWorldGen().getGenerateDragonBalls()) {
				if (definition.getBallSetId() != null && !definition.getBallSetId().isBlank()) {
					DragonBallsHandler.scatterDragonBalls(serverLevel, definition.getBallSetId());
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
	public long getInvokingTime() { return this.invokingTime; }
	public void setDragonDefinitionId(String id) { this.entityData.set(DRAGON_DEFINITION_ID, id); }
	public String getDragonDefinitionId() { return this.entityData.get(DRAGON_DEFINITION_ID); }

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
		compound.putInt("DespawnDelay", this.despawnDelay);
		compound.putString("OwnerName", this.getOwnerName());
		compound.putBoolean("GrantedWish", this.hasGrantedWish());
		compound.putString("DragonDefinitionId", this.getDragonDefinitionId());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("InvokingTime")) this.invokingTime = compound.getLong("InvokingTime");
		if (compound.contains("DespawnDelay")) this.despawnDelay = compound.getInt("DespawnDelay");
		if (compound.contains("OwnerName")) this.setOwnerName(compound.getString("OwnerName"));
		if (compound.contains("GrantedWish")) this.setGrantedWish(compound.getBoolean("GrantedWish"));
		if (compound.contains("DragonDefinitionId")) this.setDragonDefinitionId(compound.getString("DragonDefinitionId"));
	}
}
