package com.dragonminez.common.init.entities.worldboss;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.helper.DBSagasAnimationHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;

import java.util.ArrayList;
import java.util.List;

public abstract class WorldBossEntity extends DBSagasEntity {

    private static final EntityDataAccessor<Boolean> BOSS_ASLEEP =
            SynchedEntityData.defineId(WorldBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BOSS_ABILITY =
            SynchedEntityData.defineId(WorldBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BOSS_RESTING =
            SynchedEntityData.defineId(WorldBossEntity.class, EntityDataSerializers.BOOLEAN);

    protected static final int WAKE_RADIUS = 32;
    protected static final int LEASH_RADIUS = 64;
    private static final double RENDER_RANGE = 512.0D;

    private static final int SLEEP_SCAN_INTERVAL = 20;
    private static final int LEASH_CHECK_INTERVAL = 20;
    private static final int DUPLICATE_CHECK_INTERVAL = 40;

    private ServerBossEvent bossEvent;
    private BlockPos anchor;

    private int abilityTick;
    private int clientAbilityTick;

    protected WorldBossEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BOSS_ASLEEP, false);
        this.entityData.define(BOSS_ABILITY, -1);
        this.entityData.define(BOSS_RESTING, false);
    }

    @Override
    public boolean isBossAsleep() {
        return this.entityData.get(BOSS_ASLEEP);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "boss_controller", 3,
                DBSagasAnimationHandler::bossAbilityPredicate));
    }

    @Override
    public boolean isInSleepPose() {
        return this.isBossAsleep() || this.entityData.get(BOSS_RESTING);
    }

    public boolean isBossResting() {
        return this.entityData.get(BOSS_RESTING);
    }

    protected void setBossResting(boolean resting) {
        if (this.isBossResting() == resting) return;
        this.entityData.set(BOSS_RESTING, resting);
        this.setCombatFrozen(resting);
        if (resting) this.getNavigation().stop();
    }

    @Override
    public int getBossAbility() {
        return this.entityData.get(BOSS_ABILITY);
    }

    @Override
    public int getBossAbilityTicks() {
        return this.level().isClientSide ? this.clientAbilityTick : this.abilityTick;
    }

    @Override
    public boolean isMeleeAllowed() {
        return !this.isInSleepPose() && super.isMeleeAllowed();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = RENDER_RANGE * getViewScale();
        return distance < range * range;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public abstract String getWorldBossKey();

    public BlockPos getAnchor() {
        return this.anchor != null ? this.anchor : this.blockPosition();
    }

    public void setAnchor(BlockPos pos) {
        this.anchor = pos;
    }

    public void fallAsleep() {
        stopBossAbility();
        applyAsleep(true);
    }

    private void applyAsleep(boolean asleep) {
        this.entityData.set(BOSS_ASLEEP, asleep);
        this.setRaidDormant(asleep);
        this.setCombatFrozen(asleep);
        if (asleep) this.clearBossEvent();
    }

    public void wakeUp(Player trigger) {
        if (!this.isBossAsleep()) return;
        applyAsleep(false);
        if (trigger != null) this.setTarget(trigger);
        if (this.level() instanceof ServerLevel) {
            com.dragonminez.server.world.worldboss.WorldBossSessions.onBossEngaged(this);
        }
        this.onWakeUp(trigger);
    }

    protected void onWakeUp(Player trigger) {
    }

    protected boolean onReturnToSleep() {
        return false;
    }

    protected void applyFixedStats() {
    }

    public boolean startBossAbility(int ability) {
        if (this.level().isClientSide) return false;
        if (this.getBossAbility() >= 0 || this.isBossAsleep()) return false;
        if (this.isCasting() || this.isComboing() || this.isTransforming() || this.isStunned()) return false;

        this.entityData.set(BOSS_ABILITY, ability);
        this.abilityTick = 0;
        this.setCombatFrozen(true);
        return true;
    }

    public void stopBossAbility() {
        if (this.getBossAbility() < 0) return;
        this.entityData.set(BOSS_ABILITY, -1);
        this.abilityTick = 0;
        this.setCombatFrozen(false);
        this.setNoGravity(false);
    }

    protected int getBossAbilityDuration(int ability) {
        return 0;
    }

    protected void tickBossAbility(int ability, int tick) {
    }

    @Override
    protected void finishTransformationSpawn(DBSagasEntity newEntity, boolean fullHealth) {
        WorldBossEntity next = newEntity instanceof WorldBossEntity wb ? wb : null;
        if (next != null) next.setAnchor(this.getAnchor());

        super.finishTransformationSpawn(newEntity, fullHealth);

        if (next == null || !(this.level() instanceof ServerLevel)) return;
        next.applyFixedStats();
        com.dragonminez.server.world.worldboss.WorldBossManager.onBossTransformed(next);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            this.clientAbilityTick = this.getBossAbility() >= 0 ? this.clientAbilityTick + 1 : 0;
            return;
        }

        if (!this.isAlive()) return;

        if (this.anchor != null && this.tickCount % DUPLICATE_CHECK_INTERVAL == 0
                && !com.dragonminez.server.world.worldboss.WorldBossManager.isRegistered(this)) {
            this.discard();
            return;
        }

        int ability = this.getBossAbility();
        if (ability >= 0) {
            this.abilityTick++;
            tickBossAbility(ability, this.abilityTick);
            if (this.abilityTick >= getBossAbilityDuration(ability)) stopBossAbility();
        }

        if (this.isBossAsleep()) {
            if (this.tickCount % SLEEP_SCAN_INTERVAL == 0) scanForIntruder();
            return;
        }

        updateBossBar();

        if (this.tickCount % LEASH_CHECK_INTERVAL == 0 && !hasEngagedPlayer()) {
            returnToSleep();
        }
    }

    private void scanForIntruder() {
        BlockPos center = this.getAnchor();
        for (Player player : this.level().players()) {
            if (!isEligible(player)) continue;
            if (player.blockPosition().closerThan(center, WAKE_RADIUS)) {
                wakeUp(player);
                return;
            }
        }
    }

    private boolean hasEngagedPlayer() {
        BlockPos center = this.getAnchor();
        for (Player player : this.level().players()) {
            if (!isEligible(player)) continue;
            if (player.blockPosition().closerThan(center, LEASH_RADIUS)) return true;
        }
        return false;
    }

    private boolean isEligible(Player player) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) return false;
        return !StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> data.getStatus().isKnockedDown()).orElse(false);
    }

    public void returnToSleep() {
        if (this.level() instanceof ServerLevel) {
            com.dragonminez.server.world.worldboss.WorldBossSessions.onBossReset(this);
        }
        this.setTarget(null);
        this.stopBossAbility();
        this.clearBossEvent();
        if (this.onReturnToSleep()) return;

        this.applyFixedStats();
        this.setHealth(this.getMaxHealth());
        this.teleportTo(this.getAnchor().getX() + 0.5D, this.getAnchor().getY() + 1.0D, this.getAnchor().getZ() + 0.5D);
        this.fallAsleep();
    }

    private void updateBossBar() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        if (this.bossEvent == null) {
            this.bossEvent = new ServerBossEvent(this.getDisplayName(),
                    BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        }

        this.bossEvent.setName(this.getDisplayName());
        this.bossEvent.setProgress(Math.min(1.0F, this.getHealth() / this.getMaxHealth()));

        List<ServerPlayer> nearby = serverLevel.getPlayers(player ->
                isEligible(player) && player.blockPosition().closerThan(this.getAnchor(), LEASH_RADIUS));

        for (ServerPlayer shown : new ArrayList<>(this.bossEvent.getPlayers())) {
            if (!nearby.contains(shown)) this.bossEvent.removePlayer(shown);
        }
        for (ServerPlayer player : nearby) {
            this.bossEvent.addPlayer(player);
        }
    }

    private void clearBossEvent() {
        if (this.bossEvent == null) return;
        this.bossEvent.removeAllPlayers();
        this.bossEvent.setVisible(false);
        this.bossEvent = null;
    }

    @Override
    public void die(DamageSource pCause) {
        clearBossEvent();
        if (this.level() instanceof ServerLevel) {
            com.dragonminez.server.world.worldboss.WorldBossManager.onBossDefeated(this);
        }
        super.die(pCause);
    }

    @Override
    public void remove(RemovalReason pReason) {
        clearBossEvent();
        if (pReason.shouldDestroy()) {
            com.dragonminez.server.world.worldboss.WorldBossManager.onBossRemoved(this);
        }
        super.remove(pReason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("BossAsleep", this.isBossAsleep());
        if (this.anchor != null) {
            pCompound.putInt("AnchorX", this.anchor.getX());
            pCompound.putInt("AnchorY", this.anchor.getY());
            pCompound.putInt("AnchorZ", this.anchor.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("AnchorX")) {
            this.anchor = new BlockPos(pCompound.getInt("AnchorX"),
                    pCompound.getInt("AnchorY"), pCompound.getInt("AnchorZ"));
        }
        if (pCompound.contains("BossAsleep")) {
            applyAsleep(pCompound.getBoolean("BossAsleep"));
        }
    }
}
