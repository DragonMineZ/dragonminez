package com.dragonminez.common.init.entities.worldboss;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ai.AiTier;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.helper.DBSagasAnimationHandler;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.AttributeMods;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class WorldBossEntity extends DBSagasEntity {

    public static final String JANEMBA = "janemba";
    public static final String TURLES = "turles";
    public static final String METAL_COOLER_CORE = "metal_cooler_core";
    public static final String TAMAGAMI_1 = "tamagami_1";
    public static final String TAMAGAMI_2 = "tamagami_2";
    public static final String TAMAGAMI_3 = "tamagami_3";
    public static final String GOMAH = "gomah";

    public static String tamagamiKey(int number) {
        return switch (number) {
            case 1 -> TAMAGAMI_1;
            case 2 -> TAMAGAMI_2;
            default -> TAMAGAMI_3;
        };
    }

    public static final double HEALTH_OVER_HARD = 4.0D;

    private static final UUID GROUP_HEALTH_ID = UUID.fromString("3f1c2a5e-7b8d-4e61-9a0c-5d2e8f4b7a11");
    private static final UUID GROUP_MELEE_ID = UUID.fromString("8a4d6e2b-1c3f-4b7a-a95e-0f6c2d8b3e22");
    private static final UUID GROUP_KI_ID = UUID.fromString("c27e9b14-5a6d-4f83-b0e1-7d4a9c3f5e33");

    public static float bossHealth(float sagaNormalHealth) {
        return (float) (sagaNormalHealth * Difficulty.HARD.hpMultiplier() * HEALTH_OVER_HARD);
    }

    public static float bossDamage(float sagaNormalDamage) {
        return (float) (sagaNormalDamage * Difficulty.HARD.damageMultiplier());
    }

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
    private int clientAbilityId = -1;

    protected WorldBossEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
        this.setAiTier(AiTier.EXPERT);
    }

    @Override
    public boolean zanzokensOnAnyHit() {
        return true;
    }

    @Override
    protected boolean brainMovementAllowed() {
        return !this.isInSleepPose() && this.getBossAbility() < 0 && super.brainMovementAllowed();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_ASLEEP, false);
        builder.define(BOSS_ABILITY, -1);
        builder.define(BOSS_RESTING, false);
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
        if (asleep) {
            this.clearBossEvent();
            this.clearGroupScaling();
        }
    }

    public void applyGroupScaling(int members) {
        if (this.level().isClientSide || this.isBossAsleep()) return;
        GeneralServerConfig.WorldBossConfig config = ConfigManager.getServerConfig().getWorldBoss();
        int extra = Math.max(0, members - 1);
        float fraction = this.getMaxHealth() > 0.0F ? this.getHealth() / this.getMaxHealth() : 1.0F;
        boolean healthChanged = setGroupModifier(Attributes.MAX_HEALTH, GROUP_HEALTH_ID, config.getHealthPerExtraMember() * extra);
        setGroupModifier(Attributes.ATTACK_DAMAGE, GROUP_MELEE_ID, config.getDamagePerExtraMember() * extra);
        setGroupModifier(EntityAttributes.KI_BLAST_DAMAGE, GROUP_KI_ID, config.getDamagePerExtraMember() * extra);
        if (healthChanged && this.isAlive()) this.setHealth(Math.max(1.0F, this.getMaxHealth() * fraction));
    }

    private void clearGroupScaling() {
        setGroupModifier(Attributes.MAX_HEALTH, GROUP_HEALTH_ID, 0.0D);
        setGroupModifier(Attributes.ATTACK_DAMAGE, GROUP_MELEE_ID, 0.0D);
        setGroupModifier(EntityAttributes.KI_BLAST_DAMAGE, GROUP_KI_ID, 0.0D);
        if (this.getHealth() > this.getMaxHealth()) this.setHealth(this.getMaxHealth());
    }

    private boolean setGroupModifier(Holder<Attribute> attribute, UUID id, double amount) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) return false;
        AttributeModifier current = instance.getModifier(AttributeMods.id(id));
        if (current == null && amount <= 0.0D) return false;
        if (current != null && Math.abs(current.amount() - amount) < 1.0E-6D) return false;
        if (current != null) instance.removeModifier(AttributeMods.id(id));
        if (amount > 0.0D) {
            instance.addPermanentModifier(AttributeMods.of(id, "World boss group scaling", amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        return true;
    }

    public void wakeUp(Player trigger) {
        if (!this.isBossAsleep()) return;
        if (this.level() instanceof ServerLevel && !com.dragonminez.server.world.worldboss.WorldBossManager.canWake(this)) return;
        applyAsleep(false);
        if (trigger != null) this.setTarget(trigger);
        if (this.level() instanceof ServerLevel) {
            com.dragonminez.server.world.worldboss.WorldBossManager.onBossEngaged(this);
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

    protected BossEvent.BossBarColor getBossBarColor() {
        return BossEvent.BossBarColor.PURPLE;
    }

    protected Component getBossBarName() {
        return this.getDisplayName();
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
            int ability = this.getBossAbility();
            this.clientAbilityTick = ability < 0 ? 0 : ability == this.clientAbilityId ? this.clientAbilityTick + 1 : 1;
            this.clientAbilityId = ability;
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
        if (this.level() instanceof ServerLevel
                && com.dragonminez.server.world.worldboss.WorldBossManager.isFightEngaged(this, LEASH_RADIUS)) return true;
        BlockPos center = this.getAnchor();
        for (Player player : this.level().players()) {
            if (!isEligible(player)) continue;
            if (player.blockPosition().closerThan(center, LEASH_RADIUS)) return true;
        }
        return false;
    }

    protected boolean isEligible(Player player) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) return false;
        return !StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> data.getStatus().isKnockedDown()).orElse(false);
    }

    public void returnToSleep() {
        if (this.level() instanceof ServerLevel) {
            com.dragonminez.server.world.worldboss.WorldBossManager.onBossReset(this);
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
            this.bossEvent = new ServerBossEvent(this.getBossBarName(),
                    this.getBossBarColor(), BossEvent.BossBarOverlay.NOTCHED_10);
        }

        this.bossEvent.setName(this.getBossBarName());
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
        if (this.isBossAsleep()) this.applyFixedStats();
    }
}
