package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.entities.ITextureVariant;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.dragonminez.common.init.entities.goals.SagasUseSkillGoal;
import com.dragonminez.common.init.entities.ki.*;
import com.dragonminez.common.init.entities.sagas.helper.ComboManager;
import com.dragonminez.common.init.entities.sagas.helper.DBSagasAnimationHandler;
import com.dragonminez.common.init.entities.sagas.helper.SkillManager;
import com.dragonminez.common.quest.QuestService;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;

import java.util.ArrayList;
import java.util.List;

public abstract class DBSagasEntity extends Monster implements GeoEntity, ITextureVariant {

    public enum KiSkillType {
        KAMEHAMEHA(1),
        GALICK_GUN(2),
        MAKANKOSAPPO(3),
        KI_LASER(4),
        KI_EXPLOSION(5),
        KI_BARRIER(6),
        OOZARU_ROAR(7),
        GENERIC_KI_WAVE(8),
        OOZARU_BEAM(9),
        KI_VOLLEY(10),
        KI_SMALL(11),
        BLUE_HURRICANE(12),
        TRIPLE_LASER(13),
        KIENZAN(14),
        DEATH_BALL(15),
        MASENKO(16),
        BIG_BANG(17),
        FINAL_FLASH(18),
        MAJIN_CANDY(19),
        KI_AIR_VOLLEY(20);

        private final int id;
        KiSkillType(int id) { this.id = id; }
        public int getId() { return id; }
    }

    public enum ComboType {
        BASIC(0), AIR(1), KI_CHARGE_ATTACK(2), METEOR_COMBINATION(3), ANDROID_ABSORPTION(4),
        GUM_PUNCH(5), GUM_EXPAND(6), SLEEP_RECOVERY(7), RAPID_KICKS(8);

        private final int id;
        ComboType(int id) { this.id = id; }
        public int getId() { return id; }
    }

    // --- DATA ACCESSORS ---
    private static final EntityDataAccessor<Boolean> IS_CASTING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FLYING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FLYING_FAST = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> SKILL_TYPE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AURA_COLOR = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> AURA_TYPE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> IS_EVADING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_COMBOING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CURRENT_COMBO_ID = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> BATTLE_POWER = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TRANSFORMING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KI_CHARGE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_LIGHTNING = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIGHTNING_COLOR = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TEXTURE_VARIANT = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DBZ_STYLE = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_ZANZOKEN = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_KID = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Float> SCALE_VAL = SynchedEntityData.defineId(DBSagasEntity.class, EntityDataSerializers.FLOAT);

    protected int castTimer = 0;
    protected int transformTick = 0;
    private int chargeSoundTimer = 0;
    private static final int AURA_LIGHT_LEVEL = 12;
    private static final int AURA_LIGHT_INTERVAL = 2;
    private static final int AURA_LIGHT_STEP = 1;
    private BlockPos auraLightPos;
    private int auraLightLevel = 0;

    private boolean canEvade = false;
    private int evadeCooldownMax = 0;
    private int currentEvadeTimer = 0;
    private int evasionStateTicks = 0;

    @Getter @Setter private boolean canUseZanzoken = false;
    @Getter @Setter private int zanzokenCooldownMax = 0;
    @Getter @Setter private int currentZanzokenCooldown = 0;
    @Getter @Setter private int zanzokenTicks = 0;

    private boolean canUseWildSense = false;
    private int wildSenseCooldownMax = 0;
    private int currentWildSenseCooldown = 0;

    private boolean comboEnabled = false;
    private int comboCooldownMax = 0;
    private int currentComboCooldown = 0;
    public int comboTimer = 0;
    private LivingEntity comboTarget = null;
    private int activeComboId = -1;
    private int[] allowedCombos = new int[0];

    @Getter @Setter protected double defaultMovementSpeed = 0.25D;
    @Getter @Setter protected double defaultAttackSpeed = 4.0D;

    @Getter @Setter
    private boolean isAttacking = false;

    private final List<KiSkill> skillPool = new ArrayList<>();
    @Getter
    private float currentPoolSkillSize = 1.0F;
    @Getter
    private int currentPoolColorMain = 0xFFFFFF;
    @Getter
    private int currentPoolColorBorder = 0xFFFFFF;
    @Getter
    private int currentPoolColorOutline = 0xFFFFFF;

    public static class KiSkill {
        public int id;
        public int cooldownMax;
        public int currentCooldown;
        public float size;
        public int colorMain;
        public int colorBorder;
        public int colorOutline;

        public KiSkill(int id, int cooldown, float size, int colorMain, int colorBorder, int colorOutline) {
            this.id = id;
            this.cooldownMax = cooldown;
            this.currentCooldown = 0;
            this.size = size;
            this.colorMain = colorMain;
            this.colorBorder = colorBorder;
            this.colorOutline = colorOutline;
        }
    }

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    protected DBSagasEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public void setSkillColors(int mainColor, int borderColor, int outlineColor) {
        this.currentPoolColorMain = mainColor;
        this.currentPoolColorBorder = borderColor;
        this.currentPoolColorOutline = outlineColor;

    }

    public void setSkillColors(int mainColor, int borderColor) {
        this.currentPoolColorMain = mainColor;
        this.currentPoolColorBorder = borderColor;
        this.currentPoolColorOutline = ColorUtils.darkenColor(borderColor, 0.6f);
    }

    public boolean isZanzoken() {
        return this.entityData.get(IS_ZANZOKEN);
    }

    public void setZanzokenState(boolean active) {
        this.entityData.set(IS_ZANZOKEN, active);
    }

    private boolean isSafeTeleportLocation(double targetX, double targetY, double targetZ) {
        AABB targetBox = this.getBoundingBox().move(targetX - this.getX(), targetY - this.getY(), targetZ - this.getZ());
        return this.level().noCollision(this, targetBox);
    }

    public void performZanzoken() {
        this.setZanzokenState(true);
        this.zanzokenTicks = 0;
        this.currentZanzokenCooldown = this.zanzokenCooldownMax;
        executeZanzokenJump();
    }

    private void executeZanzokenJump() {
        this.playSound(MainSounds.ZANZOKEN.get(), 1.0F, 1.0F);
        boolean teleported = false;

        if (this.getTarget() != null) {
            for (int i = 0; i < 10; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double distance = 4.0D + this.random.nextDouble() * 2.0D;
                double newX = this.getTarget().getX() + Math.cos(angle) * distance;
                double newZ = this.getTarget().getZ() + Math.sin(angle) * distance;
                double newY = this.getTarget().getY();

                if (isSafeTeleportLocation(newX, newY, newZ)) {
                    this.teleportTo(newX, newY, newZ);
                    teleported = true;
                    break;
                } else if (isSafeTeleportLocation(newX, newY + 1.0D, newZ)) {
                    this.teleportTo(newX, newY + 1.0D, newZ);
                    teleported = true;
                    break;
                } else if (isSafeTeleportLocation(newX, newY - 1.0D, newZ)) {
                    this.teleportTo(newX, newY - 1.0D, newZ);
                    teleported = true;
                    break;
                }
            }

            if (teleported) {
                this.lookAt(this.getTarget(), 360, 360);
            }
        }

        if (!teleported) {
            Vec3 look = this.getLookAngle();
            this.setDeltaMovement(new Vec3(-look.x * 2.0, 0.2, -look.z * 2.0));
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1, this.getZ(), 5, 0.2, 0.5, 0.2, 0.0);
        }
    }

    public void setDBZStyle(int style) { this.entityData.set(DBZ_STYLE, style); }
    public int getDBZStyle() { return this.entityData.get(DBZ_STYLE); }
    public boolean canFly() {
        return this.getFlySpeed() > 0.0D;
    }
    public void setFlyingFast(boolean flyingFast) { this.entityData.set(IS_FLYING_FAST, flyingFast); }
    public boolean isFlyingFast() { return this.entityData.get(IS_FLYING_FAST); }
    public String getAuraType() {return this.entityData.get(AURA_TYPE);}
    public void setAuraType(String type) {this.entityData.set(AURA_TYPE, type);}
    public float getScale() {
        return this.isKid() ? 0.7F : 1.0F;
    }

    public void setScaleVal(float scale) {
        this.entityData.set(SCALE_VAL, scale);
    }


    public void setCombo(int id, int cooldown) {
        this.comboEnabled = true;
        this.activeComboId = id;
        this.comboCooldownMax = cooldown;
        this.currentComboCooldown = cooldown;
    }

    public void setAllowedCombos(int cooldown, int... comboIds) {
        this.comboEnabled = true;
        this.comboCooldownMax = cooldown;
        this.currentComboCooldown = 10;
        this.allowedCombos = comboIds;
    }

    public void setAllowedCombos(int cooldown, ComboType... combos) {
        this.comboEnabled = true;
        this.comboCooldownMax = cooldown;
        this.currentComboCooldown = 10;

        int[] ids = new int[combos.length];
        for (int i = 0; i < combos.length; i++) {
            ids[i] = combos[i].getId();
        }
        this.allowedCombos = ids;
    }

    public void setEvade(boolean active, int cooldown) {
        this.canEvade = active;
        this.evadeCooldownMax = cooldown;
        this.currentEvadeTimer = cooldown;
    }

    public void setWildSense(boolean active, int cooldown) {
        this.canUseWildSense = active;
        this.wildSenseCooldownMax = cooldown;
        this.currentWildSenseCooldown = cooldown;
    }

    public void setZanzoken(boolean active, int cooldown) {
        this.canUseZanzoken = active;
        this.zanzokenCooldownMax = cooldown;
        this.currentZanzokenCooldown = cooldown;
    }

    public void addKiSkill(KiSkillType type, int cooldown, float size, int colorMain, int colorBorder, int colorOutline) {
        this.skillPool.add(new KiSkill(type.getId(), cooldown, size, colorMain, colorBorder, colorOutline));
    }

    public void addKiSkill(KiSkillType type, int cooldown, float size, int colorMain, int colorBorder) {
        this.skillPool.add(new KiSkill(type.getId(), cooldown, size, colorMain, colorBorder, ColorUtils.darkenColor(colorBorder, 0.6f)));
    }

    public void addKiSkill(KiSkillType type, int cooldown, float size) {
        this.addKiSkill(type, cooldown, size, 0xFFFFFF, 0xFFFFFF);
    }

    public void addKiSkill(KiSkillType type, int cooldown) {
        this.addKiSkill(type, cooldown, 1.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && getTarget() == null;
            }

            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse() && getTarget() == null;
            }
        });

        this.goalSelector.addGoal(2, new SagasUseSkillGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.8D, false) {
            @Override
            protected int getAttackInterval() {
                double attackSpeed = this.mob.getAttributeValue(Attributes.ATTACK_SPEED);

                if (attackSpeed <= 0) return 20;

                //Si tiene 4.0, golpea cada 5 ticks. Si tiene 8.0 (Kid Buu), golpea cada 2.5 ticks.
                return (int) Math.max(2, 20.0D / attackSpeed);
            }
        });
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 45.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(EntityAttributes.KI_BLAST_DAMAGE.get(), 20.0D)
                .add(EntityAttributes.FLY_SPEED.get(), 0.35D)
                .add(EntityAttributes.KI_BLAST_SPEED.get(), 0.6D);
    }

    public void setKiBlastDamage(float damage) {
        if (this.getAttributes().hasAttribute(EntityAttributes.KI_BLAST_DAMAGE.get())) {
            this.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get()).setBaseValue(damage);
        }
    }

    public float getKiBlastDamage() {
        return this.getAttributes().hasAttribute(EntityAttributes.KI_BLAST_DAMAGE.get())
                ? (float) this.getAttributeValue(EntityAttributes.KI_BLAST_DAMAGE.get())
                : 20.0F;
    }

    public void setFlySpeed(double speed) {
        if (this.getAttributes().hasAttribute(EntityAttributes.FLY_SPEED.get())) {
            this.getAttribute(EntityAttributes.FLY_SPEED.get()).setBaseValue(speed);
        }
    }

    public double getFlySpeed() {
        return this.getAttributes().hasAttribute(EntityAttributes.FLY_SPEED.get())
                ? this.getAttributeValue(EntityAttributes.FLY_SPEED.get())
                : 0.35D;
    }

    public void setKiBlastSpeed(float speed) {
        if (this.getAttributes().hasAttribute(EntityAttributes.KI_BLAST_SPEED.get())) {
            this.getAttribute(EntityAttributes.KI_BLAST_SPEED.get()).setBaseValue(speed);
        }
    }

    public float getKiBlastSpeed() {
        return this.getAttributes().hasAttribute(EntityAttributes.KI_BLAST_SPEED.get())
                ? (float) this.getAttributeValue(EntityAttributes.KI_BLAST_SPEED.get())
                : 0.6F;
    }

    public void setCanFly(boolean canFly) {
        double currentFlySpeed = this.getFlySpeed();
        if (canFly) {
            if (currentFlySpeed <= 0.0D) {
                this.setFlySpeed(0.35D);
            }
        } else {
            this.setFlySpeed(0.0D);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {

            if (!this.isAlive()) {
                if (this.isCasting()) this.stopCasting();
                if (this.isComboing()) this.stopCombo();
                return;
            }

            this.handleCommonCombatMovement(this.getTarget(), this.isCasting() || this.isComboing() || this.isTransforming());

            if (this.tickCount % AURA_LIGHT_INTERVAL == 0) updateAuraLight();

            if (!this.isFlying() && this.isFlyingFast()) {
                this.setFlyingFast(false);
            }

            if (!this.isTransforming()) {

                if (!this.isCasting() && !this.isComboing()) {
                    if (this.canEvade && this.currentEvadeTimer > 0) this.currentEvadeTimer--;
                    if (this.canUseWildSense && this.currentWildSenseCooldown > 0) this.currentWildSenseCooldown--;
                    if (this.comboEnabled && this.currentComboCooldown > 0) this.currentComboCooldown--;
                    if (this.canUseZanzoken && this.currentZanzokenCooldown > 0) this.currentZanzokenCooldown--;

                    for (KiSkill skill : this.skillPool) {
                        if (skill.currentCooldown > 0) {
                            skill.currentCooldown--;
                        }
                    }
                }

                if (this.isZanzoken()) {
                    this.zanzokenTicks++;
                    if (this.zanzokenTicks % 5 == 0) {
                        executeZanzokenJump();
                    }
                    if (this.zanzokenTicks >= 40) {
                        this.setZanzokenState(false);
                        this.zanzokenTicks = 0;
                    }
                }

                if (this.isCasting()) {
                    this.castTimer++;

                    this.getNavigation().stop();
                    this.setDeltaMovement(0, 0, 0);

                    if (this.getTarget() != null) {
                        this.lookAt(this.getTarget(), 360, 360);
                    }

                    int skill = this.getSkillType();

                    if (skill == 20) {
                        this.setDeltaMovement(0, 0.15D, 0);
                    } else {
                        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                    }

                    if (skill == 7 && this.level() instanceof ServerLevel serverLevel) {
                        if (this.castTimer > 5 && this.castTimer < 30) {
                            for (int i = 0; i < 25; i++) {
                                double distance = 3.0D + this.random.nextDouble() * 2.0D;
                                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                                double heightOffset = (this.random.nextDouble() - 0.5D) * this.getBbHeight();

                                double spawnX = this.getX() + Math.cos(angle) * distance;
                                double spawnY = this.getY() + (this.getBbHeight() / 2.0) + heightOffset;
                                double spawnZ = this.getZ() + Math.sin(angle) * distance;

                                double velX = (this.getX() - spawnX) * 0.15D;
                                double velY = ((this.getY() + this.getBbHeight() * 0.8) - spawnY) * 0.15D;
                                double velZ = (this.getZ() - spawnZ) * 0.15D;

                                serverLevel.sendParticles(ParticleTypes.CLOUD, spawnX, spawnY, spawnZ, 0, velX, velY, velZ, 1.0D);
                            }
                        }
                    }

                    if (this.castTimer == 1) {
                        if (skill != 7 && skill != 13) {
                            executeSkillEffect(skill);
                        }
                    }

                    if (skill == 7 && this.castTimer == 30) {
                        executeSkillEffect(skill);
                    }

                    if (skill == 13) {
                        if (this.castTimer == 10 || this.castTimer == 20 || this.castTimer == 30) {
                            executeSkillEffect(13);
                        }
                    }

                    int maxCastDuration = SkillManager.getCastDuration(skill);

                    if (this.castTimer >= maxCastDuration) {
                        this.stopCasting();
                    }
                }

                if (this.canUseWildSense && this.currentWildSenseCooldown <= 0 && this.getTarget() != null && !this.isCasting() && !this.isComboing()) {
                    this.performTeleport(this.getTarget());
                    this.currentWildSenseCooldown = this.wildSenseCooldownMax;
                }

                if (this.hurtTime > 0 && !this.isCasting() && !this.isComboing() && !this.isZanzoken()) {
                    if (this.canUseZanzoken && this.currentZanzokenCooldown <= 0 && !this.isZanzoken()) {
                        this.performZanzoken();
                    }
                    else if (this.canEvade && this.currentEvadeTimer <= 0 && !this.isEvading()) {
                        this.performEvasion();
                    }
                }

                if (this.isEvading()) {
                    evasionStateTicks++;
                    if (evasionStateTicks > 12) {
                        this.setEvading(false);
                        this.evasionStateTicks = 0;
                    }
                }

                if (this.comboEnabled) {
                    if (this.isComboing()) {
                        this.comboTimer++;
                        handleComboLogic();
                    } else if (this.currentComboCooldown <= 0 && !this.isCasting() && this.getTarget() != null) {
                        if (this.distanceTo(this.getTarget()) < 6.0D) {
                            this.comboTarget = this.getTarget();
                            this.setComboing(true);

                            if (this.allowedCombos != null && this.allowedCombos.length > 0) {
                                int randomCombo = this.allowedCombos[this.random.nextInt(this.allowedCombos.length)];
                                this.entityData.set(CURRENT_COMBO_ID, randomCombo);
                            } else if (this.activeComboId == 10) {
                                this.entityData.set(CURRENT_COMBO_ID, this.random.nextInt(3));
                            } else {
                                this.entityData.set(CURRENT_COMBO_ID, this.activeComboId);
                            }

                            this.comboTimer = 0;
                            this.currentComboCooldown = comboCooldownMax;
                        }

                    }
                }
            }

            if (this.isTransforming()) {
                this.transformTick++;
                if (this.handleTransformationLogic(this.transformTick, 80)) {
                    if (!this.level().isClientSide) {
                        EntityType<? extends DBSagasEntity> nextFormType = this.getNextTransform();
                        if (nextFormType != null) {
                            DBSagasEntity nextForm = nextFormType.create(this.level());
                            this.finishTransformationSpawn(nextForm, this.spawnsNewFormFullHealth());
                        }
                    }
                }
            }

            if (this.isCharge()) {
                if (this.chargeSoundTimer <= 0) {
                    this.playSound(MainSounds.KI_CHARGE_LOOP.get(), 0.8F, 1.0F);
                    this.chargeSoundTimer = 40;
                }
                this.chargeSoundTimer--;
            } else {
                this.chargeSoundTimer = 0;
            }

            if (this.isLightning()) {
                if (this.random.nextInt(30) == 0) {
                    float pitch = 0.9F + this.random.nextFloat() * 0.2F;
                    this.playSound(MainSounds.KI_SPARKS.get(), 0.3F, pitch);
                }
            }
        }
    }

    private void executeSkillEffect(int skillType) {
        if (this.getTarget() == null) return;
        SkillManager.execute(skillType, this, this.getTarget());
    }

    private void handleComboLogic() {
        int comboId = this.getComboId();
        ComboManager.handleCombo(this, this.comboTarget, comboId, this.comboTimer);
    }

    public boolean hasSkillReady() {
        if (this.isComboing() || this.isZanzoken()) {
            return false;
        }

        if (this.getTarget() == null || this.distanceTo(this.getTarget()) <= 4.0D) {
            return false;
        }

        for (KiSkill skill : skillPool) {
            if (skill.currentCooldown <= 0) return true;
        }

        return false;
    }

    public void startFirstAvailableSkill() {
        for (KiSkill skill : this.skillPool) {
            if (skill.currentCooldown <= 0) {
                this.currentPoolSkillSize = skill.size;
                this.currentPoolColorMain = skill.colorMain;
                this.currentPoolColorBorder = skill.colorBorder;
                this.currentPoolColorOutline = skill.colorOutline;

                this.startCasting(skill.id);

                skill.currentCooldown = skill.cooldownMax;

                return;
            }
        }
    }

    private void updateAuraLight() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        boolean shouldEmitLight = this.isTransforming() || this.isCharge();
        int targetLevel = shouldEmitLight ? AURA_LIGHT_LEVEL : 0;
        this.auraLightLevel = approach(this.auraLightLevel, targetLevel, AURA_LIGHT_STEP);

        if (this.auraLightLevel <= 0) {
            removeAuraLight(serverLevel);
            return;
        }

        BlockPos targetPos = this.blockPosition().above();
        if (!canHostAuraLight(serverLevel, targetPos)) {
            targetPos = this.blockPosition();
            if (!canHostAuraLight(serverLevel, targetPos)) {
                removeAuraLight(serverLevel);
                return;
            }
        }

        if (this.auraLightPos != null && !this.auraLightPos.equals(targetPos)) {
            clearAuraLightIfOwned(serverLevel, this.auraLightPos);
        }

        BlockState currentState = serverLevel.getBlockState(targetPos);
        if (!isAuraLight(currentState) || currentState.getValue(LightBlock.LEVEL) != this.auraLightLevel) {
            BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, this.auraLightLevel);
            serverLevel.setBlock(targetPos, lightState, 3);
        }

        this.auraLightPos = targetPos.immutable();
    }

    private boolean canHostAuraLight(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || isAuraLight(state);
    }

    private boolean isAuraLight(BlockState state) {
        return state.is(Blocks.LIGHT);
    }

    private void removeAuraLight(ServerLevel level) {
        if (this.auraLightPos == null) return;
        clearAuraLightIfOwned(level, this.auraLightPos);
        this.auraLightPos = null;
        this.auraLightLevel = 0;
    }

    private void clearAuraLightIfOwned(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isAuraLight(state) && state.getValue(LightBlock.LEVEL) <= AURA_LIGHT_LEVEL) {
            level.removeBlock(pos, false);
        }
    }

    private int approach(int current, int target, int step) {
        if (current < target) return Math.min(target, current + step);
        if (current > target) return Math.max(target, current - step);
        return current;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            removeAuraLight(serverLevel);
        }
        super.remove(reason);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 5, DBSagasAnimationHandler::walkPredicate));
        controllers.add(new AnimationController<>(this, "skill_controller", 5, DBSagasAnimationHandler::skillPredicate));
        controllers.add(new AnimationController<>(this, "evasion_controller", 5, DBSagasAnimationHandler::evasionPredicate));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, DBSagasAnimationHandler::attackPredicate));
        controllers.add(new AnimationController<>(this, "tail_controller", 5, DBSagasAnimationHandler::tailPredicate));
        controllers.add(new AnimationController<>(this, "cape_controller", 5, DBSagasAnimationHandler::capePredicate));
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.isCasting() || this.isComboing() || this.isTransforming() || this.isZanzoken()) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            super.travel(Vec3.ZERO);
            return;
        }

        if (this.isEffectiveAi() && this.isInWater()) {
            float waterSpeed = 0.15F;

            this.moveRelative(waterSpeed, pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());

            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));

            if (this.getTarget() != null && this.getTarget().getY() > this.getY()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.02D, 0.0D));
            }
        } else {
            super.travel(pTravelVector);
        }
    }

    public void setCasting(boolean casting) {this.entityData.set(IS_CASTING, casting);}
    public boolean isCasting() {return this.entityData.get(IS_CASTING);}

    public void setFlying(boolean flying) {this.entityData.set(IS_FLYING, flying);}
    public boolean isFlying() {return this.entityData.get(IS_FLYING);}

    public int getSkillType() {return this.entityData.get(SKILL_TYPE);}
    public void setSkillType(int type) {this.entityData.set(SKILL_TYPE, type);}

    public int getBattlePower() {return this.entityData.get(BATTLE_POWER);}
    public void setBattlePower(int type) {this.entityData.set(BATTLE_POWER, type);}

    public int getAuraColor() {return this.entityData.get(AURA_COLOR);}
    public void setAuraColor(int color) {this.entityData.set(AURA_COLOR, color);}

    public boolean isTransforming() {return this.entityData.get(TRANSFORMING);}
    public void setTransforming(boolean transforming) {this.entityData.set(TRANSFORMING, transforming);}

    public boolean isCharge() {return this.entityData.get(KI_CHARGE);}
    public void setKiCharge(boolean charge) {this.entityData.set(KI_CHARGE, charge);}

    public boolean isLightning() {return this.entityData.get(IS_LIGHTNING);}
    public void setLightning(boolean active) {this.entityData.set(IS_LIGHTNING, active);}

    public int getLightningColor() {return this.entityData.get(LIGHTNING_COLOR);}
    public void setLightningColor(int color) {this.entityData.set(LIGHTNING_COLOR, color);}

    public void setEvading(boolean evading) {this.entityData.set(IS_EVADING, evading);}
    public boolean isEvading() {return this.entityData.get(IS_EVADING);}

    public void setComboing(boolean comboing) {this.entityData.set(IS_COMBOING, comboing);}
    public boolean isComboing() {return this.entityData.get(IS_COMBOING);}

    public void setisKid(boolean iskid) {this.entityData.set(IS_KID, iskid);}
    public boolean isKid() {return this.entityData.get(IS_KID);}

    public int getTextureVariant() {return this.entityData.get(TEXTURE_VARIANT);}
    public void setTextureVariant(int variant) {this.entityData.set(TEXTURE_VARIANT, variant);}

    public int getComboId() {
        return this.entityData.get(CURRENT_COMBO_ID);
    }

    public void stopCombo() {
        this.setComboing(false);
        this.entityData.set(CURRENT_COMBO_ID, -1);
        this.comboTimer = 0;
        this.comboTarget = null;

        if (this.isCharge()) {
            this.setKiCharge(false);
        }
    }
    protected boolean spawnsNewFormFullHealth() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    public boolean isBattleDamaged() {
        return this.getHealth() <= this.getMaxHealth() / 2.0F;
    }

    private void performEvasion() {
        this.setEvading(true);
        this.evasionStateTicks = 0;
        this.currentEvadeTimer = this.evadeCooldownMax;

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1, this.getZ(), 5, 0.2, 0.1, 0.2, 0.1);
        }

        Vec3 look = this.getLookAngle();
        this.setDeltaMovement(new Vec3(-look.x * 1.5, 0.3, -look.z * 1.5));
        this.playSound(MainSounds.TP.get(), 1.0F, 1.2F);
    }

    public void spawnPunchParticles(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(MainParticles.PUNCH_PARTICLE.get(), target.getX(),
                    target.getY() + (target.getBbHeight() / 2.0), target.getZ(), 0, 1.0f, 1.0f, 1.0f, 1.0);
        }
    }

    public void performTeleport(LivingEntity target) {
        Vec3 targetLook = target.getLookAngle().normalize();

        double distanceBehind = 1.5D;
        double destX = target.getX() - (targetLook.x * distanceBehind);
        double destZ = target.getZ() - (targetLook.z * distanceBehind);
        double destY = target.getY();

        this.teleportTo(destX, destY, destZ);

        this.playSound(MainSounds.TP.get(), 1.0F, 1.0F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, destX, destY + 1, destZ, 10, 0.2, 0.1, 0.2, 0.1);
        }

        this.lookAt(target, 360, 360);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putDouble("FlySpeed", this.getFlySpeed());
        pCompound.putFloat("KiBlastDamage", this.getKiBlastDamage());
        pCompound.putFloat("KiBlastSpeed", this.getKiBlastSpeed());
        pCompound.putString("AuraType", this.getAuraType());
        pCompound.putInt("DBZStyle", this.getDBZStyle());
        pCompound.putBoolean("isKid", this.isKid());
        pCompound.putInt("TextureVariant", this.getTextureVariant());
        pCompound.putBoolean("CanUseZanzoken", this.canUseZanzoken);
        pCompound.putInt("ZanzokenCooldownMax", this.zanzokenCooldownMax);

        pCompound.putFloat("SkillSize", this.currentPoolSkillSize);
        pCompound.putInt("ColorMain", this.currentPoolColorMain);
        pCompound.putInt("ColorBorder", this.currentPoolColorBorder);
        pCompound.putInt("ColorOutline", this.currentPoolColorOutline);

        pCompound.putInt("CastTimer", this.castTimer);
        pCompound.putInt("TransformTick", this.transformTick);
        pCompound.putInt("ComboTimer", this.comboTimer);
        pCompound.putInt("CurrentComboId", this.getComboId());

        pCompound.putFloat("EntityScale", this.entityData.get(SCALE_VAL));
        pCompound.putBoolean("isKid", this.isKid());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("FlySpeed")) this.setFlySpeed(pCompound.getDouble("FlySpeed"));
        if (pCompound.contains("KiBlastDamage")) this.setKiBlastDamage(pCompound.getFloat("KiBlastDamage"));
        if (pCompound.contains("KiBlastSpeed")) this.setKiBlastSpeed(pCompound.getFloat("KiBlastSpeed"));
        if (pCompound.contains("AuraType")) this.setAuraType(pCompound.getString("AuraType"));
        if (pCompound.contains("DBZStyle")) this.setDBZStyle(pCompound.getInt("DBZStyle"));
        if (pCompound.contains("isKid")) this.setisKid(pCompound.getBoolean("isKid"));
        if (pCompound.contains("CanFly") && pCompound.getBoolean("CanFly") && this.getFlySpeed() <= 0.0D) this.setFlySpeed(0.35D);
        if (pCompound.contains("TextureVariant")) {
            this.setTextureVariant(pCompound.getInt("TextureVariant"));
        }
        if (pCompound.contains("CanUseZanzoken")) this.canUseZanzoken = pCompound.getBoolean("CanUseZanzoken");
        if (pCompound.contains("ZanzokenCooldownMax")) this.zanzokenCooldownMax = pCompound.getInt("ZanzokenCooldownMax");

        if (pCompound.contains("SkillSize")) this.currentPoolSkillSize = pCompound.getFloat("SkillSize");
        if (pCompound.contains("ColorMain")) this.currentPoolColorMain = pCompound.getInt("ColorMain");
        if (pCompound.contains("ColorBorder")) this.currentPoolColorBorder = pCompound.getInt("ColorBorder");
        if (pCompound.contains("ColorOutline")) this.currentPoolColorOutline = pCompound.getInt("ColorOutline");

        if (pCompound.contains("CastTimer")) this.castTimer = pCompound.getInt("CastTimer");
        if (pCompound.contains("TransformTick")) this.transformTick = pCompound.getInt("TransformTick");
        if (pCompound.contains("ComboTimer")) this.comboTimer = pCompound.getInt("ComboTimer");
        if (pCompound.contains("CurrentComboId")) {this.entityData.set(CURRENT_COMBO_ID, pCompound.getInt("CurrentComboId"));}

        if (pCompound.contains("EntityScale")) {this.setScaleVal(pCompound.getFloat("EntityScale"));} else {this.setScaleVal(1.0F);}

        if (pCompound.contains("isKid")) {
            this.setisKid(pCompound.getBoolean("isKid"));
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_CASTING, false);
        this.entityData.define(IS_FLYING, false);
        this.entityData.define(IS_FLYING_FAST, false);
        this.entityData.define(SKILL_TYPE, 0);
        this.entityData.define(BATTLE_POWER, 20);
        this.entityData.define(AURA_COLOR, 0xFFFFFF);
        this.entityData.define(AURA_TYPE, "kakarot");
        this.entityData.define(TRANSFORMING, false);
        this.entityData.define(KI_CHARGE, false);
        this.entityData.define(IS_LIGHTNING, false);
        this.entityData.define(LIGHTNING_COLOR, 0xFFFFFF);
        this.entityData.define(IS_EVADING, false);
        this.entityData.define(IS_COMBOING, false);
        this.entityData.define(CURRENT_COMBO_ID, -1);
        this.entityData.define(DBZ_STYLE, 0);
        this.entityData.define(TEXTURE_VARIANT, 0);
        this.entityData.define(IS_ZANZOKEN, false);
        this.entityData.define(IS_KID, false);
        this.entityData.define(SCALE_VAL, 1.0F);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isTransforming() || this.isZanzoken()) return false;
        if (this.isComboing() && this.entityData.get(CURRENT_COMBO_ID) == 4) return false;

        boolean isAbsoluteDeath = pSource.is(DamageTypes.FELL_OUT_OF_WORLD) || pSource.is(DamageTypes.GENERIC_KILL);

        if (!this.level().isClientSide && pAmount >= this.getHealth()) {
            if (this.hasTransformation() && !isAbsoluteDeath) {
                this.setHealth(1.0F);
                this.startTransformation();
                return false;
            }
        }

        boolean actuallyHurt = super.hurt(pSource, pAmount);

        if (!this.level().isClientSide && this.isComboing() && this.getComboId() == 7) {
            this.stopCombo();
        }

        if (actuallyHurt && !this.level().isClientSide) {

            if (this.getHealth() <= 0.0F && this.hasTransformation() && !isAbsoluteDeath) {
                this.setHealth(1.0F);
                this.deathTime = 0;
                this.startTransformation();
                return false;
            }

            if (!this.isTransforming() && this.getHealth() <= (this.getMaxHealth() / 2.0F)) {
                if (this.hasTransformation()) this.startTransformation();
            }

            Entity attacker = pSource.getEntity();
            if (attacker instanceof LivingEntity livingAttacker) {
                boolean isUntouchablePlayer = livingAttacker instanceof Player player && (player.isCreative() || player.isSpectator());

                if (!this.isCasting() && !this.isComboing() && !isUntouchablePlayer) {
                    if (this.getTarget() != livingAttacker) this.setTarget(livingAttacker);
                }
            }
        }

        return actuallyHurt;
    }

    protected boolean hasTransformation() {
        return false;
    }

    @Override
    public void die(DamageSource pCause) {
        boolean isAbsoluteDeath = pCause.is(DamageTypes.FELL_OUT_OF_WORLD) || pCause.is(DamageTypes.GENERIC_KILL);

        if (this.hasTransformation() && !this.isTransforming() && !isAbsoluteDeath) {
            this.setHealth(1.0F);
            this.deathTime = 0;
            this.startTransformation();
            return;
        }
        super.die(pCause);
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        if (this.isTransforming()) return false;
        if (this.isCasting() || this.isComboing()) return false;
        return super.doHurtTarget(pEntity);
    }

    protected void handleCommonCombatMovement(LivingEntity target, boolean isActionActive) {
        if (this.level().isClientSide) return;

        if (isActionActive) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, 0, 0);
            if (target != null) rotateBodyToTarget(target);
            return;
        }

        if (target != null && target.isAlive()) {
            double yDiff = target.getY() - this.getY();

            if (this.canFly() && yDiff >= 4.0D && !isFlying()) setFlying(true);
            else if (isFlying()) {
                if (!this.canFly() || (yDiff < 1.0D && this.onGround())) {
                    setFlying(false);
                    this.setFlyingFast(false);
                    this.setNoGravity(false);
                }
            }
        } else {
            if (this.onGround() && isFlying()) {
                setFlying(false);
                this.setFlyingFast(false);
                this.setNoGravity(false);
            }
        }

        if (this.isFlying()) {
            this.setNoGravity(true);
            if (target != null) {
                moveTowardsTargetInAir(target);
                rotateBodyToTarget(target);
            } else this.setDeltaMovement(this.getDeltaMovement().add(0, -0.01D, 0));
        } else this.setNoGravity(false);
    }

    public void rotateBodyToTarget(LivingEntity target) {
        double d0 = target.getX() - this.getX();
        double d2 = target.getZ() - this.getZ();
        float targetYaw = (float) (Mth.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
        this.setYRot(targetYaw);
        this.setYBodyRot(targetYaw);
        this.setYHeadRot(targetYaw);
    }

    public void moveTowardsTargetInAir(LivingEntity target) {
        if (this.isCasting() || this.isComboing() || this.isEvading() || this.isZanzoken()) return;
        double flyspeed = this.getFlySpeed();

        double distance = this.distanceTo(target);
        if (distance > 15.0D) {
            if (!this.isFlyingFast()) this.setFlyingFast(true);
        } else if (distance < 7.0D) {
            if (this.isFlyingFast()) this.setFlyingFast(false);
        }

        if (this.isFlyingFast()) {
            flyspeed *= 2.0D;
        }

        double dx = target.getX() - this.getX();
        double dy = (target.getY() + 1.0D) - this.getY();
        double dz = target.getZ() - this.getZ();
        double dist3D = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist3D < 1.0) return;
        Vec3 movement = new Vec3(dx / dist3D * flyspeed, dy / dist3D * flyspeed, dz / dist3D * flyspeed);
        double gravityDrag = (dy < -0.5) ? -0.05D : -0.03D;
        this.setDeltaMovement(movement.add(0, gravityDrag, 0));
    }

    public void startCasting(int type) {
        this.setCasting(true);
        this.setSkillType(type);
        this.castTimer = 0;

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);
    }

    public void stopCasting() {
        this.setCasting(false);
        this.castTimer = 0;
        this.setSkillType(0);

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(this.defaultMovementSpeed);
    }

    protected boolean handleTransformationLogic(int transformTick, int duration) {
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);

        if (this.isCasting()) this.stopCasting();

        return transformTick >= duration;
    }

    protected void finishTransformationSpawn(DBSagasEntity newEntity, boolean fullHealth) {
        if (this.level().isClientSide || newEntity == null) return;

        Level level = this.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1, this.getZ(), 1, 0, 0, 0, 0);

            newEntity.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            newEntity.setTarget(this.getTarget());

            if (fullHealth) {
                newEntity.setHealth(newEntity.getMaxHealth());
            } else {
                newEntity.setHealth(newEntity.getMaxHealth() / 2.0F);
            }

            newEntity.setKiBlastDamage(this.getKiBlastDamage() * 1.5F);
            if (newEntity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                newEntity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * 1.5D);
            }

			if (this.getPersistentData().contains("dmz_is_hardmode")) {
				boolean isHardMode = this.getPersistentData().getBoolean("dmz_is_hardmode");
				newEntity.getPersistentData().putBoolean("dmz_is_hardmode", isHardMode);
			}

			if (this.getPersistentData().contains("dmz_saga_id")) {
				newEntity.getPersistentData().putString("dmz_saga_id", this.getPersistentData().getString("dmz_saga_id"));
			}
			if (this.getPersistentData().contains("dmz_quest_owner")) {
				newEntity.getPersistentData().putString("dmz_quest_owner", this.getPersistentData().getString("dmz_quest_owner"));
			}
			if (this.getPersistentData().contains(QuestService.QUEST_KEY_TAG)) {
				newEntity.getPersistentData().putString(QuestService.QUEST_KEY_TAG, this.getPersistentData().getString(QuestService.QUEST_KEY_TAG));
			}
			if (this.getPersistentData().contains(QuestService.QUEST_OBJECTIVE_INDEX_TAG)) {
				newEntity.getPersistentData().putInt(QuestService.QUEST_OBJECTIVE_INDEX_TAG, this.getPersistentData().getInt(QuestService.QUEST_OBJECTIVE_INDEX_TAG));
			}

			level.addFreshEntity(newEntity);
			this.discard();
		}
    }

    public EntityType<? extends DBSagasEntity> getNextTransform() {
        return null;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor pLevel, MobSpawnType reason) {
        return pLevel.getDifficulty() != Difficulty.PEACEFUL && this.checkSpawnObstruction(pLevel);
    }

    public static boolean canSpawnHere(EntityType<? extends DBSagasEntity> entity, ServerLevelAccessor world, MobSpawnType spawn, BlockPos pos, RandomSource random) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (random.nextFloat() < 0.65f) return false;

        if (world.getBrightness(LightLayer.BLOCK, pos) > 7) return false;

        boolean solidGround = world.getBlockState(pos.below()).isSolidRender(world, pos.below());
        boolean noCollision = world.isUnobstructed(world.getBlockState(pos), pos, CollisionContext.empty());

        return solidGround && noCollision;
    }

    protected void startTransformation() {
        this.setTransforming(true);
        this.playSound(MainSounds.KI_CHARGE_LOOP.get(), 1.0F, 1.2F);}

    public String getGeckolibModelName() {return ForgeRegistries.ENTITY_TYPES.getKey(this.getType()).getPath();}
}
