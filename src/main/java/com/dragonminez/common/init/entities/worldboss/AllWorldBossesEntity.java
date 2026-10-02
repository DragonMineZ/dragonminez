package com.dragonminez.common.init.entities.worldboss;

import com.dragonminez.common.combat.clash.BeamClash;
import com.dragonminez.common.combat.clash.BeamClashManager;
import com.dragonminez.common.combat.clash.ClashMeter;
import com.dragonminez.common.combat.clash.ClashParticipant;
import com.dragonminez.common.combat.util.SwordSlashManager;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.entities.ai.AiTier;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KillDriverEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasPart;
import com.dragonminez.common.init.entities.sagas.helper.DBSagasAnimations;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.BeamClashStateS2C;
import com.dragonminez.common.network.S2C.BossTelegraphS2C;
import com.dragonminez.common.network.S2C.KiBurstVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.combat.HeldVictim;
import com.dragonminez.server.events.players.combat.KnockbackHelper;
import com.dragonminez.server.events.players.combat.MeteorBurstCombo;
import com.dragonminez.server.events.players.combat.MomentumImpactHandler;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import com.dragonminez.server.world.structure.BossStructures.GeteStarShape;
import com.dragonminez.server.world.worldboss.WorldBossContribution;
import com.dragonminez.server.world.worldboss.WorldBossManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AllWorldBossesEntity {

    private static double groundHeight(ServerLevel level, double x, double z, double fromY) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int ix = Mth.floor(x);
        int iz = Mth.floor(z);
        for (int y = Mth.floor(fromY) + 2; y >= level.getMinBuildHeight(); y--) {
            cursor.set(ix, y, iz);
            if (!level.getBlockState(cursor).isAir()) return y + 1.0D;
        }
        return fromY;
    }

    public static class JanembaFat extends WorldBossEntity {

        public static final float BASE_HEALTH = 2688000.0F;
        public static final float BASE_MELEE = 84240.0F;
        public static final float BASE_KI = 76500.0F;

        public static final int ABILITY_STOMP = 2;
        public static final int ABILITY_SUMMON = 3;

        private static final int ROAR_COOLDOWN = 225;
        private static final int PUNCH_COOLDOWN = 125;
        private static final float PUNCH_RADIUS = 3.5F;
        private static final int DESTRUCTION_COOLDOWN = 200;
        private static final float DESTRUCTION_SIZE = 3.0F;

        private static final int METEOR_COOLDOWN = 400;
        private static final int METEOR_CHARGE_TICKS = 100;
        private static final int METEOR_COLOR_MAIN = 0xFFF6A8;
        private static final int METEOR_COLOR_BORDER = 0xFFD700;
        private static final int METEOR_COLOR_OUTLINE = 0xC89B00;

        private static final int SUMMON_SPAWN_TICK = 35;
        private static final int SUMMON_DURATION = 72;
        private static final int MINI_COUNT = 10;
        private static final int DOWNED_TICKS = 100;
        private static final float SUMMON_FIRST_THRESHOLD = 0.80F;
        private static final float SUMMON_SECOND_THRESHOLD = 0.60F;

        private static final int STOMP_LAUNCH = 15;
        private static final int STOMP_APEX = 21;
        private static final int STOMP_DROP = 46;
        private static final int STOMP_FORCE_IMPACT = 62;
        private static final int STOMP_DURATION = 78;
        private static final float STOMP_RADIUS = 14.0F;
        private static final float STOMP_RISE = 4.0F;
        private static final float STOMP_FALL = -3.6F;
        private static final int STOMP_COOLDOWN = 600;
        private static final int STOMP_TELEGRAPH_COLOR = 0xFF2A2A;
        private static final double STOMP_TRIGGER_RANGE = 26.0D;

        private final List<UUID> minis = new ArrayList<>();
        private double stompX;
        private double stompZ;
        private boolean stompImpacted;
        private int stompCooldown;
        private boolean summonedFirst;
        private boolean summonedSecond;
        private int downedTicks;

        public JanembaFat(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(false);
            this.setDBZStyle(6);
            this.setAuraColor(0xFFD700);
            this.setKiBlastSpeed(1.8F);
            this.setScaleVal(8.0F);
            this.setAllowedCombos(100, ComboType.GUM_PUNCH);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, ROAR_COOLDOWN, 15.5F);
            this.addKiSkill(KiSkillType.DIMENSIONAL_PUNCH, PUNCH_COOLDOWN, PUNCH_RADIUS);
            this.addKiSkill(KiSkillType.DESTRUCTION_BALLS, DESTRUCTION_COOLDOWN, DESTRUCTION_SIZE);
            this.addKiSkill(KiSkillType.BLASTER_METEOR, METEOR_COOLDOWN, 1.0F, METEOR_COLOR_MAIN, METEOR_COLOR_BORDER, METEOR_COLOR_OUTLINE);
            this.setBlasterMeteorCastTicks(METEOR_CHARGE_TICKS);

            this.applyFixedStats();
            this.getPersistentData().putBoolean("dmz_stats_configured", true);
            this.fallAsleep();
        }

        @Override
        public String getWorldBossKey() {
            return WorldBossEntity.JANEMBA;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_janemba_fat";
        }

        @Override
        public String getGeckolibTextureName() {
            return "saga_janemba_fat";
        }

        @Override
        protected void applyFixedStats() {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BASE_HEALTH);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BASE_MELEE);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.14D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(3.0D);
            this.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get()).setBaseValue(BASE_KI);
            this.setKiBlastDamage(BASE_KI);
            this.setHealth(BASE_HEALTH);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.WORLDBOSS_SUPER_JANEMBA.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

        @Override
        protected void finishTransformationSpawn(DBSagasEntity newEntity, boolean fullHealth) {
            clearMinis();
            super.finishTransformationSpawn(newEntity, fullHealth);
        }

        @Override
        public boolean hasHitboxParts() {
            return true;
        }

        @Override
        protected EntityDimensions getCoreDimensions() {
            return EntityDimensions.scalable(3.5F, 5.0F);
        }

        @Override
        protected DBSagasPart[] createHitboxParts() {
            return new DBSagasPart[] {
                    new DBSagasPart(this, "legs", 7.5F, 5.0F, 0.0F, 0.0F, 2.5F),
                    new DBSagasPart(this, "torso", 8.5F, 5.0F, 0.0F, 0.0F, 7.0F),
                    new DBSagasPart(this, "head", 6.5F, 4.0F, 0.5F, 0.0F, 11.0F)
            };
        }

        @Override
        protected int getBossAbilityDuration(int ability) {
            if (ability == ABILITY_STOMP) return STOMP_DURATION;
            if (ability == ABILITY_SUMMON) return SUMMON_DURATION;
            return 0;
        }

        @Override
        public boolean hurt(DamageSource pSource, float pAmount) {
            boolean absolute = pSource.is(DamageTypes.FELL_OUT_OF_WORLD) || pSource.is(DamageTypes.GENERIC_KILL);

            if (!this.level().isClientSide && !absolute && !this.minis.isEmpty()) {
                playGuardFeedback();
                return false;
            }
            return super.hurt(pSource, pAmount);
        }

        private void playGuardFeedback() {
            if (!(this.level() instanceof ServerLevel serverLevel)) return;
            if (this.tickCount % 4 != 0) return;

            RegistryObject<SoundEvent> sound = switch (this.random.nextInt(3)) {
                case 0 -> MainSounds.BLOCK1;
                case 1 -> MainSounds.BLOCK2;
                default -> MainSounds.BLOCK3;
            };

            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    sound.get(), SoundSource.HOSTILE, 1.6F, 0.8F);
            serverLevel.sendParticles(MainParticles.GUARD_BLOCK.get(),
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), 6, 1.2D, 1.2D, 1.2D, 0.02D);
        }

        @Override
        protected boolean onReturnToSleep() {
            clearMinis();
            this.downedTicks = 0;
            setBossResting(false);
            WorldBossContribution.clear(getWorldBossKey());
            this.summonedFirst = false;
            this.summonedSecond = false;
            this.stompCooldown = 0;
            return false;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.level().isClientSide || !this.isAlive()) return;

            if (this.stompCooldown > 0) this.stompCooldown--;
            tickMinis();

            if (this.downedTicks > 0) this.downedTicks--;
            setBossResting(this.getBossAbility() < 0 && !this.isBossAsleep()
                    && (!this.minis.isEmpty() || this.downedTicks > 0));

            if (this.getBossAbility() >= 0 || this.isBossAsleep() || this.isBossResting()) return;
            if (this.isStunned() || this.isCasting() || this.isComboing()) return;
            if (trySummonMinis()) return;

            LivingEntity target = this.getTarget();
            if (target == null || !this.onGround() || this.stompCooldown > 0) return;
            if (this.distanceTo(target) > STOMP_TRIGGER_RANGE) return;

            if (startBossAbility(ABILITY_STOMP)) {
                this.stompCooldown = STOMP_COOLDOWN;
                this.stompImpacted = false;
            }
        }

        @Override
        protected void tickBossAbility(int ability, int tick) {
            if (!(this.level() instanceof ServerLevel serverLevel)) return;

            if (ability == ABILITY_SUMMON) {
                if (tick == SUMMON_SPAWN_TICK) spawnMinis(serverLevel);
                return;
            }
            if (ability != ABILITY_STOMP) return;

            this.fallDistance = 0.0F;
            LivingEntity target = this.getTarget();

            if (tick == 1 && target != null) {
                NetworkHandler.sendToTrackingEntity(new BossTelegraphS2C(target.getX(), target.getY(), target.getZ(),
                        STOMP_RADIUS, STOMP_TELEGRAPH_COLOR, STOMP_APEX - 1, target.getId()), this);
            }

            if (tick == STOMP_LAUNCH) {
                this.setNoGravity(true);
                this.setDeltaMovement(0.0D, STOMP_RISE, 0.0D);
                this.hasImpulse = true;
                this.playSound(MainSounds.OOZARU_GROWL_PLAYER.get(), 2.0F, 1.4F);
                return;
            }

            if (tick > STOMP_LAUNCH && tick < STOMP_APEX) {
                this.setDeltaMovement(0.0D, STOMP_RISE, 0.0D);
                this.hasImpulse = true;
                return;
            }

            if (tick == STOMP_APEX) {
                this.setNoGravity(true);
                this.setDeltaMovement(0.0D, 0.0D, 0.0D);

                this.stompX = target != null ? target.getX() : this.getX();
                this.stompZ = target != null ? target.getZ() : this.getZ();
                double fromY = target != null ? target.getY() : this.getY();

                double markY = groundHeight(serverLevel, this.stompX, this.stompZ, fromY);
                NetworkHandler.sendToTrackingEntity(new BossTelegraphS2C(this.stompX, markY, this.stompZ,
                        STOMP_RADIUS, STOMP_TELEGRAPH_COLOR, STOMP_DROP - STOMP_APEX, -1), this);
                return;
            }

            if (tick > STOMP_APEX && tick < STOMP_DROP) {
                this.setDeltaMovement(0.0D, 0.0D, 0.0D);
                return;
            }

            if (tick == STOMP_DROP) {
                this.setNoGravity(false);
                this.teleportTo(this.stompX, this.getY(), this.stompZ);
                this.setDeltaMovement(0.0D, STOMP_FALL, 0.0D);
                this.hasImpulse = true;
                return;
            }

            if (tick > STOMP_DROP && !this.stompImpacted && (this.onGround() || tick >= STOMP_FORCE_IMPACT)) {
                this.stompImpacted = true;
                doStompImpact(serverLevel);
            }
        }

        private void doStompImpact(ServerLevel serverLevel) {
            double cx = this.getX();
            double cy = this.getY();
            double cz = this.getZ();

            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2F;
            AABB area = new AABB(cx - STOMP_RADIUS, cy - 3.0D, cz - STOMP_RADIUS,
                    cx + STOMP_RADIUS, cy + 5.0D, cz + STOMP_RADIUS);

            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                if (victim == this || victim.isAlliedTo(this) || victim instanceof WorldBossEntity) continue;
                if (victim instanceof MiniJanemba) continue;

                double dx = victim.getX() - cx;
                double dz = victim.getZ() - cz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > STOMP_RADIUS) continue;

                victim.invulnerableTime = 0;
                victim.hurt(this.damageSources().mobAttack(this), damage);

                double push = dist < 0.1D ? 0.0D : 0.9D * (1.0D - dist * 0.03D);
                victim.setDeltaMovement(victim.getDeltaMovement().add(dx * push * 0.2D, 0.75D, dz * push * 0.2D));
                victim.hasImpulse = true;
                victim.hurtMarked = true;
            }

            serverLevel.sendParticles(MainParticles.ROCK.get(), cx, cy + 0.3D, cz, 140, STOMP_RADIUS * 0.4D, 0.4D, STOMP_RADIUS * 0.4D, 0.35D);
            serverLevel.sendParticles(MainParticles.DUST.get(), cx, cy + 0.3D, cz, 180, STOMP_RADIUS * 0.5D, 0.6D, STOMP_RADIUS * 0.5D, 0.15D);
            serverLevel.playSound(null, cx, cy, cz, MainSounds.ANCHOR_SLAM.get(), SoundSource.HOSTILE, 4.0F, 0.55F);
            serverLevel.playSound(null, cx, cy, cz, MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.HOSTILE, 3.0F, 0.7F);

            NetworkHandler.sendToTrackingEntity(new ShockwaveVfxS2C(cx, cy + 0.2D, cz, STOMP_RADIUS * 1.4F, 0xFFD700, 16), this);
            NetworkHandler.sendToTrackingEntity(new KiBurstVfxS2C(this.getId(), true, 22.0F), this);
        }

        private void tickMinis() {
            if (this.minis.isEmpty()) return;
            if (!(this.level() instanceof ServerLevel serverLevel)) return;

            this.minis.removeIf(id -> {
                Entity mini = serverLevel.getEntity(id);
                return !(mini instanceof MiniJanemba alive) || !alive.isAlive();
            });
            if (!this.minis.isEmpty()) return;

            this.addEffect(new MobEffectInstance(MainEffects.STUN.get(), DOWNED_TICKS, 0, false, false, true));
            this.downedTicks = DOWNED_TICKS;
            serverLevel.sendParticles(MainParticles.DUST.get(), this.getX(), this.getY() + 1.0D, this.getZ(),
                    70, 2.2D, 1.0D, 2.2D, 0.05D);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    MainSounds.KNOCKBACK_CHARACTER.get(), SoundSource.HOSTILE, 3.0F, 0.6F);
        }

        private boolean trySummonMinis() {
            float ratio = this.getHealth() / this.getMaxHealth();

            if (!this.summonedFirst && ratio <= SUMMON_FIRST_THRESHOLD) {
                if (!startBossAbility(ABILITY_SUMMON)) return false;
                this.summonedFirst = true;
                return true;
            }
            if (!this.summonedSecond && ratio <= SUMMON_SECOND_THRESHOLD) {
                if (!startBossAbility(ABILITY_SUMMON)) return false;
                this.summonedSecond = true;
                return true;
            }
            return false;
        }

        private void spawnMinis(ServerLevel serverLevel) {
            this.minis.clear();
            double bellyY = this.getY() + this.getBbHeight() * 0.45D;

            for (int i = 0; i < MINI_COUNT; i++) {
                MiniJanemba mini = MainEntities.WORLDBOSS_MINI_JANEMBA.get().create(serverLevel);
                if (mini == null) continue;

                double angle = (Math.PI * 2.0D / MINI_COUNT) * i;
                double spread = 3.0D + this.random.nextDouble() * 2.0D;
                double mx = this.getX() + Math.cos(angle) * spread;
                double mz = this.getZ() + Math.sin(angle) * spread;

                mini.moveTo(mx, this.getY(), mz, (float) Math.toDegrees(angle), 0.0F);
                mini.setTarget(this.getTarget());

                if (!serverLevel.addFreshEntity(mini)) continue;
                this.minis.add(mini.getUUID());

                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, mx, bellyY, mz, 30, 0.4D, 0.5D, 0.4D, 0.03D);
            }

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), bellyY, this.getZ(), 120, 2.0D, 1.2D, 2.0D, 0.05D);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    MainSounds.OOZARU_GROWL_PLAYER.get(), SoundSource.HOSTILE, 3.0F, 1.6F);
        }

        public void clearMinis() {
            if (!(this.level() instanceof ServerLevel serverLevel)) return;
            for (UUID id : this.minis) {
                if (serverLevel.getEntity(id) instanceof MiniJanemba mini) mini.vanishInSmoke();
            }
            this.minis.clear();
        }
    }

    public static class SuperJanemba extends WorldBossEntity {

        public static final float BASE_HEALTH = 2940000.0F;
        public static final float BASE_MELEE = 92880.0F;
        public static final float BASE_KI = 84456.0F;

        public static final int ABILITY_CUTS = 5;

        private static final int ROAR_COOLDOWN = 200;
        private static final int MOUTH_BLAST_COOLDOWN = 260;
        private static final int DESTRUCTION_COOLDOWN = 240;
        private static final float DESTRUCTION_SIZE = 3.0F;
        private static final int WARP_COOLDOWN = 120;

        private static final int CUTS_DURATION = 96;
        private static final int CUTS_FIRST_TICK = 10;
        private static final int CUTS_INTERVAL = 12;
        private static final int CUTS_COUNT = 6;
        private static final int CUTS_COOLDOWN = 340;
        private static final double CUTS_MIN_RANGE = 5.0D;
        private static final double CUT_SPEED = 1.1D;
        private static final double CUT_MAX_DISTANCE = 48.0D;
        private static final double CUT_HIT_RADIUS = 7.8D;
        private static final float CUT_DAMAGE_RATIO = 0.6F;
        private static final float CUT_SCALE = 10.2F;
        private static final int CUT_COLOR = 0x7A0000;
        private static final int CUT_CORE_COLOR = 0xFF5A4A;

        private static final int MOUTH_BLAST_SKILL = 9;
        private static final int MOUTH_BLAST_FIRE_TICK = 37;
        private static final float MOUTH_BLAST_MARK_RADIUS = 5.0F;
        private static final int MOUTH_BLAST_MARK_COLOR = 0xFF2A2A;

        private int cutsCooldown;

        public SuperJanemba(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(5);
            this.setAuraColor(0xC22948);
            this.setKiBlastSpeed(2.5F);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK, ComboType.GUM_PUNCH);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, ROAR_COOLDOWN, 15.5F);
            this.addKiSkill(KiSkillType.OOZARU_BEAM, MOUTH_BLAST_COOLDOWN, 1.6F, 0xC22948, 0x8C1B32);
            this.addKiSkill(KiSkillType.DESTRUCTION_BALLS, DESTRUCTION_COOLDOWN, DESTRUCTION_SIZE);

            this.setWildSense(true, 50);
            this.setDimensionalZanzoken(true, WARP_COOLDOWN);

            this.applyFixedStats();
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.DIMENSIONAL_SWORD.get()));
            this.getPersistentData().putBoolean("dmz_stats_configured", true);
        }

        @Override
        public String getWorldBossKey() {
            return WorldBossEntity.JANEMBA;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_super_janemba";
        }

        @Override
        public String getGeckolibTextureName() {
            return "saga_super_janemba";
        }

        @Override
        protected void applyFixedStats() {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BASE_HEALTH);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BASE_MELEE);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.42D);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(9.0D);
            this.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get()).setBaseValue(BASE_KI);
            this.setKiBlastDamage(BASE_KI);
            this.setHealth(BASE_HEALTH);
        }

        @Override
        public void startCasting(int type) {
            super.startCasting(type);
            if (this.level().isClientSide || !this.isCasting()) return;
            if (type != MOUTH_BLAST_SKILL) return;

            LivingEntity target = this.getTarget();
            if (!(this.level() instanceof ServerLevel serverLevel) || target == null) return;

            double markY = groundHeight(serverLevel, target.getX(), target.getZ(), target.getY());
            NetworkHandler.sendToTrackingEntity(new BossTelegraphS2C(target.getX(), markY, target.getZ(),
                    MOUTH_BLAST_MARK_RADIUS, MOUTH_BLAST_MARK_COLOR, MOUTH_BLAST_FIRE_TICK, -1), this);
        }

        @Override
        protected int getBossAbilityDuration(int ability) {
            return ability == ABILITY_CUTS ? CUTS_DURATION : 0;
        }

        @Override
        protected void tickBossAbility(int ability, int tick) {
            if (ability != ABILITY_CUTS || !(this.level() instanceof ServerLevel serverLevel)) return;

            LivingEntity target = this.getTarget();
            if (target != null) this.lookAt(target, 60.0F, 60.0F);

            int elapsed = tick - CUTS_FIRST_TICK;
            if (elapsed < 0 || elapsed % CUTS_INTERVAL != 0) return;
            if (elapsed / CUTS_INTERVAL >= CUTS_COUNT || target == null) return;

            Vec3 origin = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.65D, this.getZ());
            Vec3 aim = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ())
                    .subtract(origin).normalize();

            int index = elapsed / CUTS_INTERVAL;
            float damage = this.getKiBlastDamage() * CUT_DAMAGE_RATIO;
            SwordSlashManager.launch(serverLevel, this, origin, aim, SwordSlashManager.rollFor(index), CUT_SCALE,
                    CUT_COLOR, CUT_CORE_COLOR, CUT_SPEED, CUT_MAX_DISTANCE, CUT_HIT_RADIUS, victim -> {
                        if (victim.isAlliedTo(this) || victim instanceof WorldBossEntity || victim instanceof MiniJanemba) return false;
                        victim.invulnerableTime = 0;
                        victim.hurt(this.damageSources().mobAttack(this), damage);
                        victim.hurtMarked = true;
                        return true;
                    });
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    MainSounds.KI_BEAM_FIRE.get(), SoundSource.HOSTILE, 1.8F, 1.7F);
        }

        @Override
        public void tick() {
            super.tick();
            if (this.level().isClientSide || !this.isAlive()) return;

            if (this.cutsCooldown > 0) this.cutsCooldown--;

            if (this.getBossAbility() >= 0 || this.isBossAsleep() || this.isStunned()) return;
            if (this.isCasting() || this.isComboing()) return;

            LivingEntity target = this.getTarget();
            if (target == null || this.cutsCooldown > 0) return;
            if (this.distanceTo(target) < CUTS_MIN_RANGE) return;

            if (startBossAbility(ABILITY_CUTS)) {
                this.cutsCooldown = CUTS_COOLDOWN;
            }
        }

        @Override
        protected boolean onReturnToSleep() {
            if (!(this.level() instanceof ServerLevel serverLevel)) return false;

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 60, 1.2D, 1.6D, 1.2D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 25, 1.0D, 1.0D, 1.0D, 0.01D);

            JanembaFat revert = MainEntities.WORLDBOSS_JANEMBA_FAT.get().create(serverLevel);
            if (revert == null) return false;

            BlockPos anchor = this.getAnchor();
            revert.setAnchor(anchor);
            revert.moveTo(anchor.getX() + 0.5D, anchor.getY() + 1.0D, anchor.getZ() + 0.5D, this.getYRot(), 0.0F);
            revert.fallAsleep();

            if (!serverLevel.addFreshEntity(revert)) return false;

            WorldBossContribution.clear(getWorldBossKey());
            WorldBossManager.onBossTransformed(revert);
            this.discard();
            return true;
        }
    }

    public static class MiniJanemba extends DBSagasEntity {

        public static final float STAT_RATIO = 0.03F;

        private static final int RETREAT_TICKS = 26;
        private static final double RETREAT_SPEED = 1.6D;
        private static final double RETREAT_DISTANCE = 9.0D;
        private static final double HIT_KNOCKBACK = 1.35D;

        private int retreatTicks;

        public MiniJanemba(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(false);
            this.setDBZStyle(6);
            this.setAuraColor(0xFFD700);
            this.setScaleVal(0.8F);
            this.setKiBlastSpeed(1.2F);

            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(JanembaFat.BASE_HEALTH * STAT_RATIO);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(JanembaFat.BASE_MELEE * STAT_RATIO);
            this.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get()).setBaseValue(JanembaFat.BASE_KI * STAT_RATIO);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.46D);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(9.0D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0D);
            this.setKiBlastDamage(JanembaFat.BASE_KI * STAT_RATIO);
            this.setHealth(this.getMaxHealth());
            this.setAiTier(AiTier.ELITE);

            this.getPersistentData().putBoolean("dmz_stats_configured", true);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.goalSelector.addGoal(1, new RetreatGoal());
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_janemba_fat";
        }

        @Override
        public String getGeckolibTextureName() {
            return "saga_janemba_fat";
        }

        @Override
        public boolean isMeleeAllowed() {
            return this.retreatTicks <= 0 && super.isMeleeAllowed();
        }

        @Override
        protected boolean brainMovementAllowed() {
            return this.retreatTicks <= 0 && super.brainMovementAllowed();
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (!hit) return false;

            if (target instanceof LivingEntity victim) {
                Vec3 push = victim.position().subtract(this.position());
                double length = push.horizontalDistance();
                if (length > 1.0E-4D) {
                    push = push.scale(HIT_KNOCKBACK / length);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.42D, push.z));
                    victim.hasImpulse = true;
                    victim.hurtMarked = true;
                }
            }

            this.retreatTicks = RETREAT_TICKS;
            return true;
        }

        @Override
        public void tick() {
            super.tick();
            if (!this.level().isClientSide && this.retreatTicks > 0) this.retreatTicks--;
        }

        public void vanishInSmoke() {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.getX(), this.getY() + 0.6D, this.getZ(), 22, 0.35D, 0.5D, 0.35D, 0.02D);
            }
            this.discard();
        }

        private class RetreatGoal extends Goal {

            private RetreatGoal() {
                this.setFlags(EnumSet.of(Goal.Flag.MOVE));
            }

            @Override
            public boolean canUse() {
                return MiniJanemba.this.retreatTicks > 0 && MiniJanemba.this.getTarget() != null;
            }

            @Override
            public boolean canContinueToUse() {
                return canUse();
            }

            @Override
            public void stop() {
                MiniJanemba.this.getNavigation().stop();
            }

            @Override
            public void tick() {
                LivingEntity target = MiniJanemba.this.getTarget();
                if (target == null) return;

                Vec3 away = MiniJanemba.this.position().subtract(target.position());
                double length = away.horizontalDistance();
                if (length < 1.0E-4D) away = MiniJanemba.this.getLookAngle().reverse();
                else away = away.scale(1.0D / length);

                Vec3 goal = MiniJanemba.this.position().add(away.scale(RETREAT_DISTANCE));
                MiniJanemba.this.getNavigation().moveTo(goal.x, goal.y, goal.z, RETREAT_SPEED);
                MiniJanemba.this.lookAt(target, 30.0F, 30.0F);
            }
        }
    }

    public static class Turles extends WorldBossEntity {

        public static final float BASE_HEALTH = 15600.0F;
        public static final float BASE_MELEE = 655.2F;
        public static final float BASE_KI = 655.2F;

        public static final int ABILITY_MIGHT_FRUIT = 6;
        public static final int ABILITY_KILL_DRIVER = 7;
        public static final int ABILITY_METEOR_BURST = 8;
        public static final int ABILITY_GRAB = 9;
        public static final int KILL_DRIVER_THROW_TICK = 43;

        private static final SupervillainPalette MIGHT_PALETTE = new SupervillainPalette(0xB00000, 0.45F, 0xE01010, 0x0A0000);
        private static final EntityDataAccessor<Boolean> MIGHT_EMPOWERED =
                SynchedEntityData.defineId(Turles.class, EntityDataSerializers.BOOLEAN);

        private static final UUID MIGHT_DAMAGE_ID = UUID.fromString("5f0b7a54-2c1e-4f5b-9a57-7d1c1f2a9e11");
        private static final double MIGHT_DAMAGE_BONUS = 0.5D;
        private static final int MIGHT_DURATION = 600;
        private static final int MIGHT_FIRST_DELAY = 900;
        private static final int MIGHT_COOLDOWN = 1800;
        private static final int MIGHT_MELEE_INTERVAL = 6;
        private static final Vector3f MIGHT_DUST = new Vector3f(0.55F, 0.0F, 0.0F);
        private static final Vector3f MIGHT_BURST_DUST = new Vector3f(0.85F, 0.05F, 0.05F);

        private static final int FRUIT_DURATION = 20;
        private static final int FRUIT_EAT_START = 4;
        private static final int FRUIT_EAT_END = 11;
        private static final int FRUIT_GONE_TICK = 12;
        private static final int FRUIT_BURST_TICK = 15;

        private static final int VOLLEY_COOLDOWN = 120;
        private static final int ZANZOKEN_COOLDOWN = 100;
        private static final int ABILITY_GAP = 50;

        private static final int GRAB_DURATION = 35;
        private static final int GRAB_COOLDOWN = 260;
        private static final int GRAB_FIRST_DELAY = 60;
        private static final double GRAB_RANGE = 3.0D;
        private static final double GRAB_DASH_RANGE = 8.0D;
        private static final double GRAB_GAP = 0.35D;
        private static final float GRAB_DAMAGE_RATIO = 1.6F;

        private static final int KILL_DRIVER_DURATION = 60;
        private static final int KILL_DRIVER_COOLDOWN = 380;
        private static final int KILL_DRIVER_FIRST_DELAY = 100;
        private static final int KILL_DRIVER_FORM_TICK = 17;
        private static final int KILL_DRIVER_RAISE_START = 32;
        private static final int KILL_DRIVER_RAISE_END = 40;
        private static final double KILL_DRIVER_MIN_RANGE = 6.0D;
        private static final double KILL_DRIVER_MAX_RANGE = 40.0D;
        private static final float KILL_DRIVER_SPEED = 1.15F;
        private static final float KILL_DRIVER_DAMAGE_RATIO = 1.75F;

        private static final int METEOR_COOLDOWN = 900;
        private static final int METEOR_FIRST_DELAY = 400;
        private static final float METEOR_STRIKE_BUDGET = 3.05F;
        private static final float METEOR_KI_BUDGET = 1.6F;

        private int mightTicks;
        private int mightCooldown = MIGHT_FIRST_DELAY;
        private int mightMeleeTimer;
        private int grabCooldown = GRAB_FIRST_DELAY;
        private int killDriverCooldown = KILL_DRIVER_FIRST_DELAY;
        private int meteorCooldown = METEOR_FIRST_DELAY;
        private int abilityGap;

        private float lockedYaw;
        private HeldVictim grabVictim;
        private KillDriverEntity heldRing;
        private final MeteorBurstCombo.State meteorState = new MeteorBurstCombo.State();
        private final MeteorBurstCombo.Performer meteorPerformer = new MeteorBurstCombo.Performer() {
            @Override
            public LivingEntity self() {
                return Turles.this;
            }

            @Override
            public LivingEntity target() {
                return Turles.this.getTarget();
            }

            @Override
            public void place(Vec3 position, float yaw) {
                Turles.this.lockedYaw = yaw;
                Turles.this.setPos(position.x, position.y, position.z);
                Turles.this.faceLocked();
            }

            @Override
            public boolean isInterrupted() {
                return Turles.this.isStunned();
            }

            @Override
            public float strikeBudget() {
                return (float) Turles.this.getAttributeValue(Attributes.ATTACK_DAMAGE) * METEOR_STRIKE_BUDGET;
            }

            @Override
            public float kiBudget() {
                return Turles.this.getKiBlastDamage() * METEOR_KI_BUDGET;
            }

            @Override
            public void strikeHit(LivingEntity victim, float amount, boolean lethal) {
                victim.hurt(MainDamageTypes.strikeAttack(Turles.this.level(), Turles.this, MeteorBurstCombo.TECHNIQUE_ID), amount);
            }

            @Override
            public void kiHit(LivingEntity victim, float amount, boolean lethal) {
                victim.hurt(MainDamageTypes.kiblast(Turles.this.level(), Turles.this, Turles.this), amount);
            }

            @Override
            public boolean canHitArea(LivingEntity entity) {
                return !entity.isAlliedTo(Turles.this) && !(entity instanceof WorldBossEntity);
            }
        };

        public Turles(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xF52727);
            this.setKiBlastSpeed(2.0F);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, VOLLEY_COOLDOWN, 1.0F,
                    MeteorBurstCombo.COLOR_MAIN, MeteorBurstCombo.COLOR_BORDER, MeteorBurstCombo.COLOR_OUTLINE);
            this.setWildSense(true, 100);
            this.setZanzoken(3, ZANZOKEN_COOLDOWN);
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

            this.applyFixedStats();
            this.getPersistentData().putBoolean("dmz_stats_configured", true);
            this.fallAsleep();
        }

        @Override
        protected void defineSynchedData() {
            super.defineSynchedData();
            this.entityData.define(MIGHT_EMPOWERED, false);
        }

        @Override
        public String getWorldBossKey() {
            return WorldBossEntity.TURLES;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_turles";
        }

        @Override
        public String getGeckolibTextureName() {
            return "saga_turles";
        }

        @Override
        public RawAnimation getSleepAnimation() {
            return DBSagasAnimations.ANIM_BOSS2_SLEEP;
        }

        @Override
        public boolean usesFullVolleyPalette() {
            return true;
        }

        public boolean isMightEmpowered() {
            return this.entityData.get(MIGHT_EMPOWERED);
        }

        @Override
        public boolean showsSupervillainAura() {
            return super.showsSupervillainAura() || this.isMightEmpowered();
        }

        @Override
        public SupervillainPalette getSupervillainPalette() {
            return this.isMightEmpowered() ? MIGHT_PALETTE : super.getSupervillainPalette();
        }

        @Override
        protected BossEvent.BossBarColor getBossBarColor() {
            return BossEvent.BossBarColor.RED;
        }

        @Override
        public boolean isMeleeAllowed() {
            return this.getBossAbility() < 0 && super.isMeleeAllowed();
        }

        @Override
        public boolean isZanzokenReady() {
            return this.getBossAbility() < 0 && super.isZanzokenReady();
        }

        @Override
        public boolean isWildSenseReady() {
            return this.getBossAbility() < 0 && super.isWildSenseReady();
        }

        @Override
        protected void applyFixedStats() {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BASE_HEALTH);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BASE_MELEE);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.0D);
            this.setDefaultAttackSpeed(6.0D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5D);
            this.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get()).setBaseValue(BASE_KI);
            this.setKiBlastDamage(BASE_KI);
            this.setHealth(BASE_HEALTH);
        }

        @Override
        protected int getBossAbilityDuration(int ability) {
            return switch (ability) {
                case ABILITY_MIGHT_FRUIT -> FRUIT_DURATION;
                case ABILITY_KILL_DRIVER -> KILL_DRIVER_DURATION;
                case ABILITY_METEOR_BURST -> MeteorBurstCombo.DURATION;
                case ABILITY_GRAB -> GRAB_DURATION;
                default -> 0;
            };
        }

        @Override
        public void tick() {
            super.tick();
            if (this.level().isClientSide || !this.isAlive()) return;

            this.tickMight();
            if (this.isBossAsleep()) return;

            LivingEntity target = this.getTarget();
            boolean fighting = target != null && target.isAlive();
            if (fighting) {
                if (this.mightTicks <= 0 && this.mightCooldown > 0) this.mightCooldown--;
                if (this.grabCooldown > 0) this.grabCooldown--;
                if (this.killDriverCooldown > 0) this.killDriverCooldown--;
                if (this.meteorCooldown > 0) this.meteorCooldown--;
            }
            if (this.abilityGap > 0) this.abilityGap--;

            if (!fighting || this.getBossAbility() >= 0 || this.abilityGap > 0) return;
            if (this.isStunned() || this.isCasting() || this.isComboing() || this.isTransforming() || this.isZanzoken()) return;

            if (this.mightTicks <= 0 && this.mightCooldown <= 0 && this.startBossAbility(ABILITY_MIGHT_FRUIT)) return;

            double distance = this.distanceTo(target);
            boolean inSight = this.hasLineOfSight(target);

            if (this.meteorCooldown <= 0 && distance <= MeteorBurstCombo.RANGE && inSight && HeldVictim.canHold(this, target)
                    && this.startBossAbility(ABILITY_METEOR_BURST)) {
                this.meteorCooldown = METEOR_COOLDOWN;
                return;
            }
            if (this.grabCooldown <= 0 && distance <= GRAB_DASH_RANGE + target.getBbWidth() * 0.5D
                    && HeldVictim.canHold(this, target) && this.startBossAbility(ABILITY_GRAB)) {
                this.grabCooldown = GRAB_COOLDOWN;
                return;
            }
            if (this.killDriverCooldown <= 0 && distance >= KILL_DRIVER_MIN_RANGE && distance <= KILL_DRIVER_MAX_RANGE
                    && inSight && this.startBossAbility(ABILITY_KILL_DRIVER)) {
                this.killDriverCooldown = KILL_DRIVER_COOLDOWN;
            }
        }

        @Override
        public void stopBossAbility() {
            int ability = this.getBossAbility();
            super.stopBossAbility();
            if (ability < 0) return;

            this.abilityGap = ABILITY_GAP;
            if (ability == ABILITY_MIGHT_FRUIT) this.clearHeldFruit();
            this.discardHeldRing();
            this.releaseGrab();
            MeteorBurstCombo.release(this.meteorState);
        }

        @Override
        protected void tickBossAbility(int ability, int tick) {
            if (!(this.level() instanceof ServerLevel serverLevel)) return;

            switch (ability) {
                case ABILITY_MIGHT_FRUIT -> this.tickMightFruit(serverLevel, tick);
                case ABILITY_KILL_DRIVER -> this.tickKillDriver(serverLevel, tick);
                case ABILITY_METEOR_BURST -> this.tickMeteorBurst(serverLevel, tick);
                case ABILITY_GRAB -> this.tickGrab(serverLevel, tick);
                default -> {
                }
            }
        }

        private void tickMightFruit(ServerLevel level, int tick) {
            LivingEntity target = this.getTarget();
            if (target != null) this.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (tick == 1) {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.MIGHT_TREE_FRUIT.get()));
                Vec3 hand = this.handPosition();
                level.sendParticles(ParticleTypes.GLOW, hand.x, hand.y, hand.z, 10, 0.15D, 0.15D, 0.15D, 0.02D);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.4F, 0.6F);
            }

            if (tick >= FRUIT_EAT_START && tick <= FRUIT_EAT_END && (tick - FRUIT_EAT_START) % 3 == 0) {
                Vec3 mouth = this.getEyePosition().add(this.getLookAngle().scale(0.35D)).subtract(0.0D, 0.15D, 0.0D);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(MainItems.MIGHT_TREE_FRUIT.get())),
                        mouth.x, mouth.y, mouth.z, 6, 0.1D, 0.1D, 0.1D, 0.05D);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE,
                        1.0F, 0.8F + this.random.nextFloat() * 0.2F);
            }

            if (tick == FRUIT_GONE_TICK) this.clearHeldFruit();
            if (tick == FRUIT_BURST_TICK) this.empower(level);
        }

        private Vec3 handPosition() {
            Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
            Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
            return this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D).add(forward.scale(0.35D)).add(right.scale(0.4D));
        }

        private void empower(ServerLevel level) {
            this.mightTicks = MIGHT_DURATION;
            this.mightMeleeTimer = 0;
            this.entityData.set(MIGHT_EMPOWERED, true);
            this.applyMightModifier(true);

            double cy = this.getY() + this.getBbHeight() * 0.5D;
            level.sendParticles(new DustParticleOptions(MIGHT_BURST_DUST, 2.0F), this.getX(), cy, this.getZ(), 60, 0.7D, 0.9D, 0.7D, 0.05D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), cy, this.getZ(), 40, 0.6D, 0.9D, 0.6D, 0.04D);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.AURA_START.get(), SoundSource.HOSTILE, 2.0F, 0.7F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.HOSTILE, 1.5F, 0.6F);
            NetworkHandler.sendToTrackingEntity(new ShockwaveVfxS2C(this.getX(), this.getY() + 0.2D, this.getZ(), 9.0F, 0xC00000, 16), this);
            NetworkHandler.sendToTrackingEntity(new KiBurstVfxS2C(this.getId(), true, 6.0F), this);
        }

        private void tickMight() {
            if (this.mightTicks <= 0) return;
            if (--this.mightTicks <= 0) {
                this.endMight(true);
                return;
            }
            if (this.mightMeleeTimer > 0) this.mightMeleeTimer--;

            if (this.level() instanceof ServerLevel level && this.tickCount % 4 == 0) {
                level.sendParticles(new DustParticleOptions(MIGHT_DUST, 1.2F), this.getX(), this.getY() + this.getBbHeight() * 0.5D,
                        this.getZ(), 3, 0.35D, 0.6D, 0.35D, 0.0D);
            }

            if (this.mightMeleeTimer > 0 || this.isBossAsleep() || this.getBossAbility() >= 0) return;
            if (this.isCasting() || this.isComboing() || this.isStunned() || this.isTransforming() || this.isZanzoken()) return;
            if (!this.isMeleeAllowed() || this.isCombatFrozen()) return;

            LivingEntity target = this.getTarget();
            if (target == null || !target.isAlive()) return;
            double reach = this.getMeleeReach() + target.getBbWidth() * 0.5D;
            if (this.distanceToSqr(target) > reach * reach || !this.hasLineOfSight(target)) return;

            this.mightMeleeTimer = MIGHT_MELEE_INTERVAL;
            this.lookAt(target, 30.0F, 30.0F);
            this.swing(InteractionHand.MAIN_HAND);
            target.invulnerableTime = 0;
            this.doHurtTarget(target);
        }

        private void endMight(boolean startCooldown) {
            this.mightTicks = 0;
            this.entityData.set(MIGHT_EMPOWERED, false);
            this.applyMightModifier(false);
            if (startCooldown) this.mightCooldown = MIGHT_COOLDOWN;
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                        30, 0.4D, 0.7D, 0.4D, 0.02D);
            }
        }

        private void applyMightModifier(boolean active) {
            AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage == null) return;
            damage.removeModifier(MIGHT_DAMAGE_ID);
            if (active) {
                damage.addTransientModifier(new AttributeModifier(MIGHT_DAMAGE_ID, "Fruit of Might", MIGHT_DAMAGE_BONUS,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        private void clearHeldFruit() {
            if (this.getItemBySlot(EquipmentSlot.MAINHAND).is(MainItems.MIGHT_TREE_FRUIT.get())) {
                this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }

        private void tickGrab(ServerLevel level, int tick) {
            if (tick == 1) {
                LivingEntity target = this.getTarget();
                if (!HeldVictim.canHold(this, target) || this.distanceTo(target) > GRAB_DASH_RANGE + target.getBbWidth() * 0.5D) {
                    this.stopBossAbility();
                    return;
                }
                if (this.distanceTo(target) > GRAB_RANGE + target.getBbWidth() * 0.5D) {
                    double gap = this.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + GRAB_GAP;
                    HeldVictim.Approach approach = HeldVictim.approach(level, this, target, gap);
                    this.teleportTo(approach.destination().x, approach.destination().y, approach.destination().z);
                    this.lockedYaw = approach.yaw();
                } else {
                    this.lockedYaw = this.yawTowards(target);
                }
                this.faceLocked();
                this.grabVictim = HeldVictim.capture(target, GRAB_DURATION + 10);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), MainSounds.GOLPE2.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
            }

            if (this.grabVictim == null) return;
            this.faceLocked();
            if (!this.grabVictim.isValid(this, 48.0D) || this.isStunned()) {
                this.stopBossAbility();
                return;
            }

            LivingEntity victim = this.grabVictim.entity();
            if (tick < StrikeAttackHandler.GRAB_HOLD_TICK) {
                double gap = this.getBbWidth() * 0.5D + victim.getBbWidth() * 0.5D + GRAB_GAP;
                this.grabVictim.hold(this.position().add(this.facing().scale(gap)), this.lockedYaw + 180.0F);
                return;
            }
            if (tick < StrikeAttackHandler.GRAB_RELEASE_TICK) {
                if (tick == StrikeAttackHandler.GRAB_HOLD_TICK) this.grabVictim.grabbed();
                victim.invulnerableTime = 20;
                victim.fallDistance = 0.0F;
                StrikeAttackHandler.holdGrabVictim(this, victim, tick);
                return;
            }
            if (tick == StrikeAttackHandler.GRAB_RELEASE_TICK) this.throwVictim(level);
        }

        private void throwVictim(ServerLevel level) {
            LivingEntity target = this.grabVictim.entity();
            Vec3 dir = this.facing();
            this.releaseGrab();

            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * GRAB_DAMAGE_RATIO;
            target.invulnerableTime = 0;
            target.hurt(MainDamageTypes.strikeAttack(level, this, null), damage);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), MainSounds.CRITICO1.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
            if (!target.isAlive()) return;

            KnockbackHelper.apply(target, new Vec3(dir.x * StrikeAttackHandler.GRAB_THROW_FORCE, StrikeAttackHandler.GRAB_THROW_LIFT,
                    dir.z * StrikeAttackHandler.GRAB_THROW_FORCE));
            MomentumImpactHandler.registerCollisionImpact(target, MomentumImpactHandler.CollisionImpactType.WALL, damage * 0.2F, dir);
        }

        private void releaseGrab() {
            if (this.grabVictim == null) return;
            this.grabVictim.release();
            this.grabVictim = null;
        }

        private void tickKillDriver(ServerLevel level, int tick) {
            LivingEntity target = this.getTarget();
            if (tick < KILL_DRIVER_THROW_TICK) {
                if (target != null) this.lockedYaw = this.yawTowards(target);
                else if (tick == 1) this.lockedYaw = this.getYRot();
                this.faceLocked();
            }

            if (tick == 1) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.HOSTILE, 1.6F, 0.8F);
            }

            if (tick == KILL_DRIVER_FORM_TICK) {
                KillDriverEntity ring = new KillDriverEntity(level, this);
                ring.setup(this, this.getKiBlastDamage() * KILL_DRIVER_DAMAGE_RATIO,
                        KillDriverEntity.COLOR_MAIN, KillDriverEntity.COLOR_BORDER, KillDriverEntity.COLOR_OUTLINE);
                ring.setRadius(KillDriverEntity.RADIUS * KillDriverEntity.FORM_SCALE);
                ring.hold(this.ringHoldPoint(tick, ring.getSize()), this.facing());
                ring.setTechniqueId(KillDriverEntity.TECHNIQUE_ID);
                if (level.addFreshEntity(ring)) this.heldRing = ring;
                level.playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_CHARGE_LOOP.get(), SoundSource.HOSTILE, 1.4F, 1.3F);
            }

            KillDriverEntity ring = this.heldRing;
            if (ring == null) return;
            if (!ring.isAlive()) {
                this.heldRing = null;
                return;
            }

            if (tick < KILL_DRIVER_THROW_TICK) {
                ring.setRadius(KillDriverEntity.RADIUS * Mth.lerp(this.ringRaise(tick), KillDriverEntity.FORM_SCALE, 1.0F));
                ring.hold(this.ringHoldPoint(tick, ring.getSize()), this.facing());
                return;
            }

            if (tick == KILL_DRIVER_THROW_TICK) {
                ring.setRadius(KillDriverEntity.RADIUS);
                Vec3 aim = target != null && target.isAlive()
                        ? target.getBoundingBox().getCenter().subtract(ring.position())
                        : this.facing();
                ring.launch(aim, KILL_DRIVER_SPEED);
                this.heldRing = null;
            }
        }

        private float ringRaise(int tick) {
            return Mth.clamp((tick - KILL_DRIVER_RAISE_START) / (float) (KILL_DRIVER_RAISE_END - KILL_DRIVER_RAISE_START), 0.0F, 1.0F);
        }

        private Vec3 ringHoldPoint(int tick, float radius) {
            return KillDriverEntity.holdPoint(this, this.facing(), this.ringRaise(tick), radius);
        }

        private void discardHeldRing() {
            if (this.heldRing != null && !this.heldRing.isFiring()) this.heldRing.discard();
            this.heldRing = null;
        }

        private void tickMeteorBurst(ServerLevel level, int tick) {
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
            if (!MeteorBurstCombo.tick(level, this.meteorPerformer, this.meteorState, tick)) this.stopBossAbility();
        }

        private Vec3 facing() {
            return Vec3.directionFromRotation(0.0F, this.lockedYaw);
        }

        private float yawTowards(Entity target) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            if (dx * dx + dz * dz < 1.0E-6D) return this.getYRot();
            return (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        }

        private void faceLocked() {
            this.setYRot(this.lockedYaw);
            this.setYBodyRot(this.lockedYaw);
            this.setYHeadRot(this.lockedYaw);
        }

        @Override
        protected boolean onReturnToSleep() {
            if (this.mightTicks > 0) this.endMight(false);
            this.mightCooldown = MIGHT_FIRST_DELAY;
            this.grabCooldown = GRAB_FIRST_DELAY;
            this.killDriverCooldown = KILL_DRIVER_FIRST_DELAY;
            this.meteorCooldown = METEOR_FIRST_DELAY;
            this.abilityGap = 0;
            this.clearHeldFruit();
            this.releaseGrab();
            MeteorBurstCombo.release(this.meteorState);
            WorldBossContribution.clear(getWorldBossKey());
            return false;
        }

        @Override
        public void die(DamageSource pCause) {
            this.releaseGrab();
            MeteorBurstCombo.release(this.meteorState);
            this.clearHeldFruit();
            super.die(pCause);
        }

        @Override
        public void remove(RemovalReason pReason) {
            this.releaseGrab();
            MeteorBurstCombo.release(this.meteorState);
            this.discardHeldRing();
            super.remove(pReason);
        }

        @Override
        public void addAdditionalSaveData(CompoundTag pCompound) {
            super.addAdditionalSaveData(pCompound);
            pCompound.putInt("MightTicks", this.mightTicks);
            pCompound.putInt("MightCooldown", this.mightCooldown);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag pCompound) {
            super.readAdditionalSaveData(pCompound);
            this.mightTicks = Math.max(0, pCompound.getInt("MightTicks"));
            if (pCompound.contains("MightCooldown")) this.mightCooldown = pCompound.getInt("MightCooldown");
            boolean empowered = this.mightTicks > 0;
            this.entityData.set(MIGHT_EMPOWERED, empowered);
            this.applyMightModifier(empowered);
            this.clearHeldFruit();
        }
    }

    public static class MetalCoolerCore extends WorldBossEntity {

        public static final float BASE_HEALTH = 187200.0F;
        public static final float BASE_MELEE = 6660.0F;
        public static final float BASE_KI = 5580.0F;

        public static final int ABILITY_ORBITAL_LASERS = 10;
        public static final int ABILITY_CABLES = 11;
        public static final int ABILITY_OVERLOAD = 12;
        public static final int ABILITY_SUPERNOVA = 13;
        public static final int SUPERNOVA_CHANNEL = 200;

        private static final int SKILL_COLOR_MAIN = 0xAA00FF;
        private static final int SKILL_COLOR_BORDER = 0x6C00A3;
        private static final int METEOR_COLOR_MAIN = 0xE3A6FF;
        private static final int METEOR_COLOR_BORDER = 0xAA00FF;
        private static final int METEOR_COLOR_OUTLINE = 0x6C00A3;
        private static final int METEOR_CHARGE_TICKS = 80;
        private static final int STRIKE_COLOR_MAIN = 0xFFD6D6;
        private static final int STRIKE_COLOR_BORDER = 0xFF2A2A;
        private static final int STRIKE_COLOR_OUTLINE = 0x8A0010;
        private static final int STRIKE_TELEGRAPH_COLOR = 0xFF2A2A;
        private static final int BIND_TELEGRAPH_COLOR = 0xA020F0;
        private static final int SUPERNOVA_MAIN = 0xFF3866;
        private static final int SUPERNOVA_BORDER = 0xA3143A;
        private static final int SUPERNOVA_OUTLINE = 0x4A0316;

        private static final double WALK_SPEED = 0.13D;
        private static final float PHASE_TWO = 0.70F;
        private static final float PHASE_THREE = 0.35F;
        private static final int ABILITY_GAP = 40;
        private static final double CHAMBER_RADIUS = GeteStarShape.CHAMBER_RADIUS;
        private static final double CHAMBER_REACH = CHAMBER_RADIUS + 3.0D;
        private static final double CHAMBER_PLAYER_RADIUS = CHAMBER_RADIUS + 1.0D;
        private static final double ROAM_RADIUS = CHAMBER_RADIUS - 5.0D;
        private static final double CHAMBER_HEIGHT = 48.0D;
        private static final double HELPER_MARGIN = 16.0D;

        private static final int STRIKE_WAVES = 3;
        private static final int STRIKE_POINTS = 5;
        private static final int STRIKE_WAVE_INTERVAL = 25;
        private static final int STRIKE_WARNING = 30;
        private static final int STRIKE_DURATION = 1 + STRIKE_WAVE_INTERVAL * (STRIKE_WAVES - 1) + STRIKE_WARNING + 15;
        private static final float STRIKE_RADIUS = 4.0F;
        private static final float STRIKE_SIZE = 30.0F;
        private static final float STRIKE_SPEED = 10.0F;
        private static final int STRIKE_LIFE = 22;
        private static final double STRIKE_HEIGHT = 44.0D;
        private static final float STRIKE_DAMAGE_RATIO = 0.9F;
        private static final double STRIKE_PLAYER_SCATTER = 2.0D;
        private static final double STRIKE_MIN_SPACING = 6.0D;
        private static final int STRIKE_COOLDOWN = 340;
        private static final int STRIKE_FIRST_DELAY = 100;

        private static final int BIND_DURATION = 44;
        private static final int BIND_WARNING = 30;
        private static final float BIND_RADIUS = 5.0F;
        private static final int BIND_MAX_TARGETS = 3;
        private static final int BIND_COOLDOWN = 500;
        private static final int BIND_FIRST_DELAY = 100;

        private static final int OVERLOAD_DURATION = 240;
        private static final float OVERLOAD_DAMAGE_BONUS = 1.5F;

        private static final int SUPERNOVA_STRUGGLE_TICKS = 400;
        private static final int SUPERNOVA_REFLECT_TICKS = 12;
        private static final int SUPERNOVA_DURATION = SUPERNOVA_CHANNEL + SUPERNOVA_STRUGGLE_TICKS + SUPERNOVA_REFLECT_TICKS + 20;
        private static final int SUPERNOVA_COOLDOWN = 1200;
        private static final int SUPERNOVA_FIRST_DELAY = 200;
        private static final int SUPERNOVA_MARK_REFRESH = 100;
        private static final float SUPERNOVA_DAMAGE_RATIO = 3.0F;
        private static final float SUPERNOVA_REFLECT_RATIO = 0.08F;
        private static final float SUPERNOVA_START_SIZE = 2.5F;
        private static final float SUPERNOVA_END_SIZE = 12.0F;
        private static final double SUPERNOVA_MIN_TRAVEL = 18.0D;

        private static final int PRODUCTION_INTERVAL = 600;
        private static final int MAX_GUARDIANS = 3;
        private static final int SWARM_FIRST = 8;
        private static final int SWARM_SECOND = 6;
        private static final float SWARM_SECOND_THRESHOLD = 0.15F;
        private static final float COPY_DRAIN_RATIO = 0.02F;
        private static final int COPY_SLOW_AMPLIFIER = 3;

        private BlockPos chamberCenter;
        private final List<BlockPos> capsuleSpots = new ArrayList<>();
        private boolean swarmFirst;
        private boolean swarmSecond;
        private int phase = 1;
        private float overload;
        private ServerBossEvent overloadBar;

        private int abilityGap;
        private int strikeCooldown = STRIKE_FIRST_DELAY;
        private int bindCooldown = BIND_FIRST_DELAY;
        private int supernovaCooldown = SUPERNOVA_FIRST_DELAY;
        private int productionTimer = PRODUCTION_INTERVAL / 2;
        private final List<List<Vec3>> strikeWaves = new ArrayList<>();
        private final List<UUID> bindTargets = new ArrayList<>();
        private final List<Vec3> bindSpots = new ArrayList<>();
        private KiBlastEntity supernovaBall;
        private SupernovaClash supernovaClash;
        private int reflectTick = -1;
        private Vec3 reflectFrom;

        private List<MetalCoolerCopy> copyCache = new ArrayList<>();

        public MetalCoolerCore(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(false);
            this.setDBZStyle(2);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.2F);
            this.setScaleVal(6.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 140, 1.4F, SKILL_COLOR_MAIN, SKILL_COLOR_BORDER);
            this.addKiSkill(KiSkillType.BLASTER_METEOR, 420, 1.0F, METEOR_COLOR_MAIN, METEOR_COLOR_BORDER, METEOR_COLOR_OUTLINE);
            this.setBlasterMeteorCastTicks(METEOR_CHARGE_TICKS);
            this.addKiSkill(KiSkillType.OOZARU_SLAM, 320);
            this.addKiSkill(KiSkillType.OOZARU_FIST, 260);

            this.applyFixedStats();
            this.getPersistentData().putBoolean("dmz_stats_configured", true);
            this.fallAsleep();
        }

        @Override
        public String getWorldBossKey() {
            return WorldBossEntity.METAL_COOLER_CORE;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_metal_cooler_core";
        }

        @Override
        public String getGeckolibTextureName() {
            return "saga_metal_cooler_core";
        }

        @Override
        public RawAnimation getSleepAnimation() {
            return DBSagasAnimations.ANIM_IDLE;
        }

        @Override
        protected BossEvent.BossBarColor getBossBarColor() {
            return BossEvent.BossBarColor.PURPLE;
        }

        @Override
        protected void applyFixedStats() {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BASE_HEALTH);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BASE_MELEE);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(WALK_SPEED);
            this.setDefaultMovementSpeed(WALK_SPEED);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
            this.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get()).setBaseValue(BASE_KI);
            this.setKiBlastDamage(BASE_KI);
            this.setHealth(BASE_HEALTH);
        }

        @Override
        public boolean hasHitboxParts() {
            return true;
        }

        @Override
        protected EntityDimensions getCoreDimensions() {
            return EntityDimensions.scalable(2.5F, 5.0F);
        }

        @Override
        protected DBSagasPart[] createHitboxParts() {
            return new DBSagasPart[] {
                    new DBSagasPart(this, "legs", 4.0F, 5.0F, 0.0F, 0.0F, 2.5F),
                    new DBSagasPart(this, "torso", 4.5F, 5.0F, 0.0F, 0.0F, 7.0F),
                    new DBSagasPart(this, "head", 4.0F, 4.0F, 0.5F, 0.0F, 11.0F)
            };
        }

        @Override
        public boolean isPushable() {
            return false;
        }

        @Override
        public void knockback(double strength, double x, double z) {
        }

        @Override
        public boolean isDashReady() {
            return false;
        }

        public void setChamber(BlockPos center, List<BlockPos> capsules) {
            this.chamberCenter = center;
            this.capsuleSpots.clear();
            this.capsuleSpots.addAll(capsules);
        }

        public int getPhase() {
            return this.phase;
        }

        public float getOverload() {
            return this.overload;
        }

        private BlockPos center() {
            return this.chamberCenter != null ? this.chamberCenter : this.getAnchor().above();
        }

        private AABB chamberBox() {
            return new AABB(this.center()).inflate(CHAMBER_REACH, 0.0D, CHAMBER_REACH).expandTowards(0.0D, CHAMBER_HEIGHT, 0.0D)
                    .expandTowards(0.0D, -3.0D, 0.0D);
        }

        private static boolean eligible(Player player) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) return false;
            return !StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> data.getStatus().isKnockedDown()).orElse(false);
        }

        private boolean inChamber(Vec3 pos) {
            BlockPos center = this.center();
            double dx = pos.x - (center.getX() + 0.5D);
            double dz = pos.z - (center.getZ() + 0.5D);
            double dy = pos.y - center.getY();
            return dx * dx + dz * dz <= CHAMBER_PLAYER_RADIUS * CHAMBER_PLAYER_RADIUS && dy >= -2.0D && dy <= CHAMBER_HEIGHT;
        }

        private List<ServerPlayer> participants(ServerLevel level) {
            List<ServerPlayer> result = new ArrayList<>();
            for (ServerPlayer player : level.players()) {
                if (eligible(player) && this.inChamber(player.position())) result.add(player);
            }
            return result;
        }

        private void announce(ServerLevel level, String key) {
            Component message = Component.translatable(key);
            for (ServerPlayer player : this.participants(level)) player.displayClientMessage(message, true);
        }

        private void stayInChamber() {
            if (this.chamberCenter == null) return;
            double cx = this.chamberCenter.getX() + 0.5D;
            double cz = this.chamberCenter.getZ() + 0.5D;
            double dx = this.getX() - cx;
            double dz = this.getZ() - cz;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance <= ROAM_RADIUS || distance < 1.0E-4D) return;
            double scale = ROAM_RADIUS / distance;
            this.setPos(cx + dx * scale, this.getY(), cz + dz * scale);
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            this.getNavigation().stop();
        }

        @Override
        public void tick() {
            super.tick();
            if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;

            this.stayInChamber();

            if (this.isBossAsleep()) {
                this.clearOverloadBar();
                if (this.tickCount % 20 == 0) {
                    List<ServerPlayer> intruders = this.participants(level);
                    if (!intruders.isEmpty()) this.wakeUp(intruders.get(0));
                }
                return;
            }

            float ratio = this.getHealth() / this.getMaxHealth();
            int nextPhase = ratio > PHASE_TWO ? 1 : ratio > PHASE_THREE ? 2 : 3;
            if (nextPhase > this.phase) this.enterPhase(level, nextPhase);

            this.tickCopies(level);
            this.updateOverloadBar(level);

            LivingEntity target = this.getTarget();
            boolean fighting = target != null && target.isAlive();
            if (fighting) {
                if (this.strikeCooldown > 0) this.strikeCooldown--;
                if (this.phase >= 2 && this.bindCooldown > 0) this.bindCooldown--;
                if (this.phase >= 3 && this.supernovaCooldown > 0) this.supernovaCooldown--;
                if (this.phase < 3 && --this.productionTimer <= 0) {
                    this.productionTimer = PRODUCTION_INTERVAL;
                    this.produceGuardian(level);
                }
            }
            if (this.abilityGap > 0) this.abilityGap--;

            if (!fighting || this.getBossAbility() >= 0 || this.abilityGap > 0) return;
            if (this.isStunned() || this.isCasting() || this.isComboing()) return;

            if (this.phase >= 3 && this.supernovaCooldown <= 0 && this.startBossAbility(ABILITY_SUPERNOVA)) {
                this.supernovaCooldown = SUPERNOVA_COOLDOWN;
                return;
            }
            if (this.phase >= 2 && this.bindCooldown <= 0 && !this.bindCandidates(level).isEmpty() && this.startBossAbility(ABILITY_CABLES)) {
                this.bindCooldown = BIND_COOLDOWN;
                return;
            }
            if (this.strikeCooldown <= 0 && this.startBossAbility(ABILITY_ORBITAL_LASERS)) {
                this.strikeCooldown = STRIKE_COOLDOWN;
            }
        }

        private void enterPhase(ServerLevel level, int nextPhase) {
            this.phase = nextPhase;
            if (nextPhase == 2) {
                this.bindCooldown = BIND_FIRST_DELAY;
                this.announce(level, "worldboss.dragonminez.metal_cooler_core.phase2");
            } else if (nextPhase == 3) {
                this.supernovaCooldown = SUPERNOVA_FIRST_DELAY;
                this.announce(level, "worldboss.dragonminez.metal_cooler_core.phase3");
                if (!this.swarmFirst) {
                    this.swarmFirst = true;
                    this.releaseSwarm(level, SWARM_FIRST);
                }
            }
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 0.5F);
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (this.phase >= 3 && !this.swarmSecond && this.getHealth() / this.getMaxHealth() <= SWARM_SECOND_THRESHOLD
                    && this.level() instanceof ServerLevel level && !this.isBossAsleep()) {
                this.swarmSecond = true;
                this.releaseSwarm(level, SWARM_SECOND);
            }
        }

        @Override
        public boolean isAlliedTo(Entity pEntity) {
            return GeteStarEntities.sameFaction(this, pEntity) || super.isAlliedTo(pEntity);
        }

        @Override
        public boolean hurt(DamageSource pSource, float pAmount) {
            if (GeteStarEntities.sameFaction(this, pSource.getEntity())) return false;
            boolean absolute = pSource.is(DamageTypes.FELL_OUT_OF_WORLD) || pSource.is(DamageTypes.GENERIC_KILL);
            if (!this.level().isClientSide && !absolute && this.getBossAbility() == ABILITY_OVERLOAD) pAmount *= OVERLOAD_DAMAGE_BONUS;
            return super.hurt(pSource, pAmount);
        }

        public void addOverload(float amount) {
            if (this.isBossAsleep() || this.phase < 2) return;
            int ability = this.getBossAbility();
            if (ability == ABILITY_OVERLOAD || ability == ABILITY_SUPERNOVA) return;
            this.overload = Math.min(1.0F, this.overload + amount);
            if (this.overload >= 1.0F && this.level() instanceof ServerLevel level) this.triggerOverload(level, true);
        }

        private void triggerOverload(ServerLevel level, boolean announce) {
            this.overload = 0.0F;
            this.removeEffect(MainEffects.STUN.get());
            if (this.isCasting()) this.stopCasting();
            if (this.isComboing()) this.interruptCombo();
            this.stopBossAbility();
            for (GeteStarEntities.Bind bind : this.binds(level)) bind.release(false);
            this.startBossAbility(ABILITY_OVERLOAD);
            Vec3 torso = this.position().add(0.0D, 7.0D, 0.0D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, torso.x, torso.y, torso.z, 120, 2.2D, 3.0D, 2.2D, 0.4D);
            level.sendParticles(ParticleTypes.EXPLOSION, torso.x, torso.y, torso.z, 4, 1.5D, 2.0D, 1.5D, 0.0D);
            level.playSound(null, torso.x, torso.y, torso.z, MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.HOSTILE, 3.0F, 1.4F);
            level.playSound(null, torso.x, torso.y, torso.z, SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 3.0F, 0.5F);
            NetworkHandler.sendToTrackingEntity(new KiBurstVfxS2C(this.getId(), true, 10.0F), this);
            if (announce) this.announce(level, "worldboss.dragonminez.metal_cooler_core.overload");
        }

        private void updateOverloadBar(ServerLevel level) {
            if (this.phase < 2) {
                this.clearOverloadBar();
                return;
            }
            if (this.overloadBar == null) {
                this.overloadBar = new ServerBossEvent(Component.translatable("worldboss.dragonminez.metal_cooler_core.overload_bar"),
                        BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
            }
            this.overloadBar.setProgress(this.getBossAbility() == ABILITY_OVERLOAD
                    ? 1.0F - this.getBossAbilityTicks() / (float) OVERLOAD_DURATION : this.overload);
            syncBar(this.overloadBar, this.participants(level));
        }

        private static void syncBar(ServerBossEvent bar, List<ServerPlayer> shown) {
            for (ServerPlayer player : new ArrayList<>(bar.getPlayers())) {
                if (!shown.contains(player)) bar.removePlayer(player);
            }
            for (ServerPlayer player : shown) bar.addPlayer(player);
        }

        private void clearOverloadBar() {
            if (this.overloadBar == null) return;
            this.overloadBar.removeAllPlayers();
            this.overloadBar.setVisible(false);
            this.overloadBar = null;
        }

        @Override
        protected int getBossAbilityDuration(int ability) {
            return switch (ability) {
                case ABILITY_ORBITAL_LASERS -> STRIKE_DURATION;
                case ABILITY_CABLES -> BIND_DURATION;
                case ABILITY_OVERLOAD -> OVERLOAD_DURATION;
                case ABILITY_SUPERNOVA -> SUPERNOVA_DURATION;
                default -> 0;
            };
        }

        @Override
        public void stopBossAbility() {
            int ability = this.getBossAbility();
            super.stopBossAbility();
            if (ability < 0) return;
            this.abilityGap = ABILITY_GAP;
            this.strikeWaves.clear();
            this.bindTargets.clear();
            this.bindSpots.clear();
            this.endSupernovaClash(null);
            this.discardSupernovaBall();
            this.reflectTick = -1;
        }

        @Override
        protected void tickBossAbility(int ability, int tick) {
            if (!(this.level() instanceof ServerLevel level)) return;
            switch (ability) {
                case ABILITY_ORBITAL_LASERS -> this.tickOrbitalLasers(level, tick);
                case ABILITY_CABLES -> this.tickBinds(level, tick);
                case ABILITY_OVERLOAD -> this.tickOverload(level, tick);
                case ABILITY_SUPERNOVA -> this.tickSupernova(level, tick);
                default -> {
                }
            }
        }

        private void tickOrbitalLasers(ServerLevel level, int tick) {
            LivingEntity target = this.getTarget();
            if (target != null) this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            for (int wave = 0; wave < STRIKE_WAVES; wave++) {
                int start = 1 + wave * STRIKE_WAVE_INTERVAL;
                if (tick == start) this.markStrikeWave(level);
                if (tick == start + STRIKE_WARNING && wave < this.strikeWaves.size()) this.fireStrikeWave(level, this.strikeWaves.get(wave));
            }
        }

        private void markStrikeWave(ServerLevel level) {
            BlockPos center = this.center();
            double floor = center.getY();
            List<Vec3> points = new ArrayList<>();
            List<ServerPlayer> players = this.participants(level);
            Collections.shuffle(players, new Random(this.random.nextLong()));
            for (ServerPlayer player : players) {
                if (points.size() >= 2) break;
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double scatter = this.random.nextDouble() * STRIKE_PLAYER_SCATTER;
                this.addStrikePoint(points, player.getX() + Math.cos(angle) * scatter, floor, player.getZ() + Math.sin(angle) * scatter);
            }
            for (int attempt = 0; attempt < 40 && points.size() < STRIKE_POINTS; attempt++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double distance = Math.sqrt(this.random.nextDouble()) * (CHAMBER_RADIUS - 4.0D);
                this.addStrikePoint(points, center.getX() + 0.5D + Math.cos(angle) * distance, floor,
                        center.getZ() + 0.5D + Math.sin(angle) * distance);
            }
            for (Vec3 point : points) {
                NetworkHandler.sendToTrackingEntity(new BossTelegraphS2C(point.x, point.y, point.z, STRIKE_RADIUS,
                        STRIKE_TELEGRAPH_COLOR, STRIKE_WARNING + 2, -1), this);
            }
            this.strikeWaves.add(points);
            level.playSound(null, this.getX(), this.getEyeY(), this.getZ(), MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.HOSTILE, 3.0F, 0.6F);
        }

        private void addStrikePoint(List<Vec3> points, double x, double y, double z) {
            Vec3 candidate = new Vec3(x, y, z);
            BlockPos center = this.center();
            double dx = x - (center.getX() + 0.5D);
            double dz = z - (center.getZ() + 0.5D);
            if (dx * dx + dz * dz > (CHAMBER_RADIUS - 2.0D) * (CHAMBER_RADIUS - 2.0D)) return;
            for (Vec3 point : points) {
                if (point.distanceToSqr(candidate) < STRIKE_MIN_SPACING * STRIKE_MIN_SPACING) return;
            }
            points.add(candidate);
        }

        private void fireStrikeWave(ServerLevel level, List<Vec3> points) {
            float damage = this.getKiBlastDamage() * STRIKE_DAMAGE_RATIO;
            for (Vec3 point : points) {
                Vec3 origin = point.add(0.0D, 0.05D, 0.0D);
                KiLaserEntity laser = new KiLaserEntity(level, this);
                laser.setupStrike(this, origin, origin.add(0.0D, STRIKE_HEIGHT, 0.0D), damage, STRIKE_SPEED, STRIKE_SIZE, STRIKE_RADIUS,
                        STRIKE_COLOR_MAIN, STRIKE_COLOR_BORDER, STRIKE_COLOR_OUTLINE, STRIKE_LIFE);
            }
            level.playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_BEAM_FIRE.get(), SoundSource.HOSTILE, 4.0F, 0.6F);
        }

        private List<GeteStarEntities.Bind> binds(ServerLevel level) {
            return GeteStarEntities.owned(level, GeteStarEntities.Bind.class, this.chamberBox().inflate(HELPER_MARGIN), this.getUUID());
        }

        private List<ServerPlayer> bindCandidates(ServerLevel level) {
            List<ServerPlayer> candidates = this.participants(level);
            for (GeteStarEntities.Bind bind : this.binds(level)) candidates.removeIf(player -> bind.holds(player.getUUID()));
            return candidates;
        }

        private void tickBinds(ServerLevel level, int tick) {
            LivingEntity target = this.getTarget();
            if (target != null) this.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (tick == 1) {
                List<ServerPlayer> candidates = this.bindCandidates(level);
                Collections.shuffle(candidates, new Random(this.random.nextLong()));
                double floor = this.center().getY();
                for (int i = 0; i < Math.min(BIND_MAX_TARGETS, candidates.size()); i++) {
                    ServerPlayer victim = candidates.get(i);
                    Vec3 spot = new Vec3(victim.getX(), floor, victim.getZ());
                    this.bindTargets.add(victim.getUUID());
                    this.bindSpots.add(spot);
                    NetworkHandler.sendToTrackingEntity(new BossTelegraphS2C(spot.x, spot.y, spot.z, BIND_RADIUS,
                            BIND_TELEGRAPH_COLOR, BIND_WARNING, -1), this);
                }
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 3.0F, 0.5F);
                return;
            }
            if (tick != 1 + BIND_WARNING) return;

            for (int i = 0; i < this.bindSpots.size(); i++) {
                Vec3 spot = this.bindSpots.get(i);
                ServerPlayer victim = level.getPlayerByUUID(this.bindTargets.get(i)) instanceof ServerPlayer player ? player : null;
                boolean caught = victim != null && eligible(victim) && Math.abs(victim.getY() - spot.y) <= 3.0D
                        && victim.position().subtract(spot).horizontalDistanceSqr() <= BIND_RADIUS * BIND_RADIUS;
                float yaw = (float) (Mth.atan2(this.getZ() - spot.z, this.getX() - spot.x) * (180.0D / Math.PI)) - 90.0F;
                GeteStarEntities.Bind bind = GeteStarEntities.Bind.create(level, this, spot, yaw, caught ? victim : null);
                if (bind != null) level.addFreshEntity(bind);
            }
        }

        private void tickOverload(ServerLevel level, int tick) {
            if (tick % 3 != 0) return;
            Vec3 torso = this.position().add(0.0D, 5.5D, 0.0D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, torso.x, torso.y, torso.z, 10, 2.0D, 2.6D, 2.0D, 0.25D);
            if (tick % 30 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, torso.x, torso.y, torso.z, 12, 1.5D, 2.0D, 1.5D, 0.02D);
                level.playSound(null, torso.x, torso.y, torso.z, MainSounds.KI_SPARKS.get(), SoundSource.HOSTILE, 2.0F, 0.6F);
            }
        }

        private void tickSupernova(ServerLevel level, int tick) {
            LivingEntity target = this.getTarget();
            if (target != null && tick < SUPERNOVA_CHANNEL) this.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (tick == 1) {
                KiBlastEntity ball = new KiBlastEntity(level, this);
                ball.setupKiDeathBall(this, 0.0F, 0.5F, SUPERNOVA_MAIN, SUPERNOVA_BORDER, SUPERNOVA_OUTLINE, SUPERNOVA_DURATION + 100);
                ball.setSize(SUPERNOVA_START_SIZE);
                this.supernovaBall = ball;
                this.markSupernovaTarget(SUPERNOVA_CHANNEL);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.HOSTILE, 4.0F, 0.4F);
                this.announce(level, "worldboss.dragonminez.metal_cooler_core.supernova");
            }

            KiBlastEntity ball = this.supernovaBall;
            if (ball == null || !ball.isAlive()) {
                if (tick > 1) this.stopBossAbility();
                return;
            }

            if (tick < SUPERNOVA_CHANNEL) {
                ball.setSize(Mth.lerp(tick / (float) SUPERNOVA_CHANNEL, SUPERNOVA_START_SIZE, SUPERNOVA_END_SIZE));
                if (tick % 40 == 0) {
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_CHARGE_LOOP.get(), SoundSource.HOSTILE, 3.0F, 0.5F);
                }
                return;
            }

            if (tick == SUPERNOVA_CHANNEL) {
                ball.setDetached(true);
                this.supernovaClash = new SupernovaClash(this, ball, ball.position(), this.supernovaEnd(ball.position()));
                BeamClashManager.registerExternal(this.supernovaClash);
                level.playSound(null, ball.getX(), ball.getY(), ball.getZ(), MainSounds.KI_BEAM_FIRE.get(), SoundSource.HOSTILE, 4.0F, 0.5F);
                this.announce(level, "worldboss.dragonminez.metal_cooler_core.supernova_clash");
            }

            if (this.reflectTick >= 0) {
                this.tickReflect(level, ball);
                return;
            }

            SupernovaClash clash = this.supernovaClash;
            if (clash == null) return;
            if ((tick - SUPERNOVA_CHANNEL) % SUPERNOVA_MARK_REFRESH == 0) this.markSupernovaTarget(SUPERNOVA_MARK_REFRESH + 2);

            SupernovaClash.Result result = clash.tick(level, this.participants(level));
            if (result == SupernovaClash.Result.PLAYERS) {
                this.endSupernovaClash(true);
                this.reflectTick = 0;
                this.reflectFrom = ball.position();
                this.announce(level, "worldboss.dragonminez.metal_cooler_core.supernova_reflected");
            } else if (result == SupernovaClash.Result.COOLER) {
                Vec3 impact = ball.position();
                this.endSupernovaClash(false);
                this.discardSupernovaBall();
                this.detonateSupernova(level, impact);
                this.stopBossAbility();
            }
        }

        private Vec3 supernovaEnd(Vec3 start) {
            BlockPos center = this.center();
            Vec3 middle = Vec3.atBottomCenterOf(center).add(0.0D, 2.0D, 0.0D);
            LivingEntity target = this.getTarget();
            Vec3 aim = target != null && target.isAlive() ? target.position().add(0.0D, 1.0D, 0.0D) : middle;
            Vec3 flat = new Vec3(aim.x - start.x, 0.0D, aim.z - start.z);
            if (flat.lengthSqr() < 1.0E-4D) flat = new Vec3(middle.x - start.x, 0.0D, middle.z - start.z);
            if (flat.lengthSqr() < 1.0E-4D) flat = new Vec3(1.0D, 0.0D, 0.0D);
            double reach = Math.max(SUPERNOVA_MIN_TRAVEL, flat.length());
            Vec3 end = new Vec3(start.x, 0.0D, start.z).add(flat.normalize().scale(reach));
            double dx = end.x - middle.x;
            double dz = end.z - middle.z;
            double limit = CHAMBER_RADIUS - 4.0D;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > limit) {
                dx *= limit / distance;
                dz *= limit / distance;
            }
            return new Vec3(middle.x + dx, Math.max(middle.y, aim.y), middle.z + dz);
        }

        private void markSupernovaTarget(int lifetime) {
            BlockPos center = this.center();
            NetworkHandler.sendToTrackingEntity(new BossTelegraphS2C(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D,
                    (float) CHAMBER_RADIUS, SUPERNOVA_MAIN, lifetime, -1), this);
        }

        private void tickReflect(ServerLevel level, KiBlastEntity ball) {
            this.reflectTick++;
            Vec3 torso = this.position().add(0.0D, 7.0D, 0.0D);
            float progress = Math.min(1.0F, this.reflectTick / (float) SUPERNOVA_REFLECT_TICKS);
            Vec3 pos = this.reflectFrom.lerp(torso, progress * progress);
            ball.setPos(pos.x, pos.y, pos.z);
            if (this.reflectTick < SUPERNOVA_REFLECT_TICKS) return;

            this.discardSupernovaBall();
            KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), level);
            visual.setPos(torso.x, torso.y, torso.z);
            visual.setupExplosion(SUPERNOVA_MAIN, SUPERNOVA_BORDER, SUPERNOVA_OUTLINE, 14.0F);
            level.addFreshEntity(visual);
            level.playSound(null, torso.x, torso.y, torso.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 6.0F, 0.5F);

            List<ServerPlayer> heroes = this.participants(level);
            float total = this.getMaxHealth() * SUPERNOVA_REFLECT_RATIO;
            if (heroes.isEmpty()) {
                this.invulnerableTime = 0;
                this.hurt(this.damageSources().explosion(null, null), total);
            } else {
                float share = total / heroes.size();
                for (ServerPlayer hero : heroes) {
                    this.invulnerableTime = 0;
                    this.hurt(MainDamageTypes.kiblast(level, hero, hero), share);
                }
            }
            this.stopBossAbility();
            if (this.isAlive()) this.triggerOverload(level, false);
        }

        private void endSupernovaClash(Boolean playersWon) {
            SupernovaClash clash = this.supernovaClash;
            if (clash == null) return;
            this.supernovaClash = null;
            BeamClashManager.unregisterExternal(clash);
            clash.end(playersWon);
        }

        private void detonateSupernova(ServerLevel level, Vec3 impact) {
            KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), level);
            visual.setPos(impact.x, impact.y, impact.z);
            visual.setupExplosion(SUPERNOVA_MAIN, SUPERNOVA_BORDER, SUPERNOVA_OUTLINE, 30.0F);
            level.addFreshEntity(visual);

            float damage = this.getKiBlastDamage() * SUPERNOVA_DAMAGE_RATIO;
            for (ServerPlayer player : this.participants(level)) {
                player.invulnerableTime = 0;
                player.hurt(MainDamageTypes.kiblast(level, this, this), damage);
                player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.6D, 0.0D));
                player.hurtMarked = true;
            }
            level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 6.0F, 0.5F);
            level.playSound(null, impact.x, impact.y, impact.z, MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.HOSTILE, 6.0F, 0.6F);
            BlockPos center = this.center();
            NetworkHandler.sendToTrackingEntity(new ShockwaveVfxS2C(impact.x, center.getY() + 0.2D, impact.z, 44.0F, SUPERNOVA_MAIN, 20), this);
        }

        private void discardSupernovaBall() {
            if (this.supernovaBall != null && this.supernovaBall.isAlive()) this.supernovaBall.discard();
            this.supernovaBall = null;
        }

        private void produceGuardian(ServerLevel level) {
            if (this.capsuleSpots.isEmpty()) return;
            int alive = GeteStarEntities.owned(level, MetalCooler.class, this.chamberBox(), this.getUUID()).size();
            int pending = GeteStarEntities.owned(level, GeteStarEntities.Scrap.class, this.chamberBox(), this.getUUID()).size();
            if (alive + pending >= MAX_GUARDIANS) return;
            BlockPos capsule = this.capsuleSpots.get(this.random.nextInt(this.capsuleSpots.size()));
            GeteStarEntities.Scrap assembly = GeteStarEntities.Scrap.create(level, this, Vec3.atBottomCenterOf(capsule));
            if (assembly != null) level.addFreshEntity(assembly);
        }

        public void spawnGuardian(Vec3 position) {
            if (!(this.level() instanceof ServerLevel level)) return;
            MetalCooler guardian = MainEntities.WORLDBOSS_METAL_COOLER.get().create(level);
            if (guardian == null) return;
            GeteStarEntities.setOwner(guardian, this.getUUID());
            guardian.moveTo(position.x, position.y, position.z, this.random.nextFloat() * 360.0F, 0.0F);
            guardian.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(position)), MobSpawnType.MOB_SUMMONED, null, null);
            guardian.setPersistenceRequired();
            List<ServerPlayer> players = this.participants(level);
            if (!players.isEmpty()) guardian.setTarget(players.get(this.random.nextInt(players.size())));
            if (!level.addFreshEntity(guardian)) return;
            level.sendParticles(MainParticles.STARDUST.get(), position.x, position.y + 1.0D, position.z, 60, 0.6D, 1.0D, 0.6D, 0.2D);
            level.sendParticles(ParticleTypes.FLASH, position.x, position.y + 1.0D, position.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, position.x, position.y, position.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 0.8F);
        }

        private void releaseSwarm(ServerLevel level, int count) {
            if (this.capsuleSpots.isEmpty()) return;
            List<ServerPlayer> players = this.participants(level);
            for (int i = 0; i < count; i++) {
                BlockPos capsule = this.capsuleSpots.get(i % this.capsuleSpots.size());
                MetalCoolerCopy copy = MainEntities.WORLDBOSS_METAL_COOLER_COPY.get().create(level);
                if (copy == null) continue;
                GeteStarEntities.setOwner(copy, this.getUUID());
                double x = capsule.getX() + 0.5D + (this.random.nextDouble() - 0.5D);
                double z = capsule.getZ() + 0.5D + (this.random.nextDouble() - 0.5D);
                copy.moveTo(x, capsule.getY(), z, this.random.nextFloat() * 360.0F, 0.0F);
                copy.finalizeSpawn(level, level.getCurrentDifficultyAt(capsule), MobSpawnType.MOB_SUMMONED, null, null);
                copy.setPersistenceRequired();
                if (!players.isEmpty()) copy.setTarget(players.get(this.random.nextInt(players.size())));
                if (!level.addFreshEntity(copy)) continue;
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, capsule.getY() + 1.0D, z, 16, 0.4D, 0.6D, 0.4D, 0.02D);
            }
            for (BlockPos capsule : this.capsuleSpots) {
                level.playSound(null, capsule.getX(), capsule.getY(), capsule.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.HOSTILE, 2.5F, 0.5F);
            }
        }

        private void tickCopies(ServerLevel level) {
            if (this.tickCount % 5 == 0) {
                this.copyCache = GeteStarEntities.owned(level, MetalCoolerCopy.class, this.chamberBox().inflate(HELPER_MARGIN), this.getUUID());
            }

            Map<Integer, Integer> latched = new HashMap<>();
            for (MetalCoolerCopy copy : this.copyCache) {
                if (copy.isAlive() && copy.isLatched()) latched.merge(copy.latchedId(), 1, Integer::sum);
            }

            for (Map.Entry<Integer, Integer> entry : latched.entrySet()) {
                if (!(level.getEntity(entry.getKey()) instanceof ServerPlayer victim)) continue;
                int count = entry.getValue();
                if (count >= 2) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, COPY_SLOW_AMPLIFIER, false, false, true));
                }
                if (this.tickCount % 20 != 0) continue;
                StatsProvider.get(StatsCapability.INSTANCE, victim).ifPresent(data -> {
                    float ratio = COPY_DRAIN_RATIO * count;
                    data.getResources().removeEnergy(data.getMaxEnergy() * ratio, false);
                    data.getResources().removeStamina(data.getMaxStamina() * ratio);
                    NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(victim), victim);
                });
            }
        }

        private void clearHelpers(ServerLevel level) {
            AABB box = this.chamberBox().inflate(HELPER_MARGIN);
            UUID id = this.getUUID();
            for (GeteStarEntities.Bind bind : GeteStarEntities.owned(level, GeteStarEntities.Bind.class, box, id)) bind.discard();
            for (GeteStarEntities.Scrap scrap : GeteStarEntities.owned(level, GeteStarEntities.Scrap.class, box, id)) scrap.discard();
            for (MetalCooler guardian : GeteStarEntities.owned(level, MetalCooler.class, box, id)) guardian.vanish();
            for (MetalCoolerCopy copy : GeteStarEntities.owned(level, MetalCoolerCopy.class, box, id)) copy.vanish();
            this.copyCache = new ArrayList<>();
        }

        @Override
        public void wakeUp(Player trigger) {
            if (trigger != null && this.chamberCenter != null && !this.inChamber(trigger.position())) return;
            super.wakeUp(trigger);
        }

        @Override
        protected void onWakeUp(Player trigger) {
            this.strikeCooldown = STRIKE_FIRST_DELAY;
            this.bindCooldown = BIND_FIRST_DELAY;
            this.supernovaCooldown = SUPERNOVA_FIRST_DELAY;
            this.productionTimer = PRODUCTION_INTERVAL / 2;
            this.overload = 0.0F;
            this.phase = 1;
            if (this.level() instanceof ServerLevel level) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4.0F, 0.4F);
                this.announce(level, "worldboss.dragonminez.metal_cooler_core.awake");
            }
        }

        @Override
        protected boolean onReturnToSleep() {
            this.clearOverloadBar();
            this.endSupernovaClash(null);
            this.discardSupernovaBall();
            if (this.level() instanceof ServerLevel level) this.clearHelpers(level);
            this.swarmFirst = false;
            this.swarmSecond = false;
            this.phase = 1;
            this.overload = 0.0F;
            this.abilityGap = 0;
            WorldBossContribution.clear(getWorldBossKey());
            return false;
        }

        @Override
        public void die(DamageSource pCause) {
            this.clearOverloadBar();
            this.endSupernovaClash(null);
            this.discardSupernovaBall();
            if (this.level() instanceof ServerLevel level) this.clearHelpers(level);
            super.die(pCause);
        }

        @Override
        public void remove(RemovalReason pReason) {
            this.clearOverloadBar();
            this.endSupernovaClash(null);
            this.discardSupernovaBall();
            super.remove(pReason);
        }

        @Override
        public void addAdditionalSaveData(CompoundTag pCompound) {
            super.addAdditionalSaveData(pCompound);
            if (this.chamberCenter != null) pCompound.putLong("ChamberCenter", this.chamberCenter.asLong());
            pCompound.putLongArray("CapsuleSpots", this.capsuleSpots.stream().mapToLong(BlockPos::asLong).toArray());
            pCompound.putBoolean("SwarmFirst", this.swarmFirst);
            pCompound.putBoolean("SwarmSecond", this.swarmSecond);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag pCompound) {
            super.readAdditionalSaveData(pCompound);
            if (pCompound.contains("ChamberCenter")) this.chamberCenter = BlockPos.of(pCompound.getLong("ChamberCenter"));
            this.capsuleSpots.clear();
            for (long packed : pCompound.getLongArray("CapsuleSpots")) this.capsuleSpots.add(BlockPos.of(packed));
            this.swarmFirst = pCompound.getBoolean("SwarmFirst");
            this.swarmSecond = pCompound.getBoolean("SwarmSecond");
        }

        static final class SupernovaClash implements BeamClashManager.ExternalClash {

            enum Result { ONGOING, PLAYERS, COOLER }

            private static final float DRIFT = 0.010F;
            private static final float START_BIAS = 0.7F;
            private static final float WIN_BIAS = 0.8F;
            private static final float LOSE_BIAS = 0.2F;
            private static final float COOLER_PUSH = 0.17F;
            private static final float COOLER_PUSH_PER_FIGHTER = 0.08F;
            private static final float FIGHTER_PUSH = 1.5F;
            private static final float SMALL_HIT_PUSH = 0.012F;
            private static final double JOIN_PAD = 2.5D;
            private static final double SEARCH_RANGE = 80.0D;
            private static final int KEEP_ALIVE_TICKS = 40;
            private static final int BREAKTHROUGH_TICKS = 60;
            private static final int EXHAUST_TICKS = 70;

            private final MetalCoolerCore core;
            private final KiBlastEntity ball;
            private final Vec3 start;
            private final Vec3 end;
            private final Map<UUID, Fighter> fighters = new LinkedHashMap<>();
            private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("worldboss.dragonminez.metal_cooler_core.clash_bar", 0),
                    BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
            private float bias = START_BIAS;
            private int age;
            private int shownFighters;

            private record Fighter(ServerPlayer player, ClashParticipant participant, long joinGameTime, long joinNanos) {}

            SupernovaClash(MetalCoolerCore core, KiBlastEntity ball, Vec3 start, Vec3 end) {
                this.core = core;
                this.ball = ball;
                this.start = start;
                this.end = end;
            }

            private float visual() {
                return BeamClash.visualBias(this.bias);
            }

            private double ballRadius() {
                return Math.max(1.5D, this.ball.getSize() * 0.5D);
            }

            Result tick(ServerLevel level, List<ServerPlayer> audience) {
                this.age++;
                Vec3 pos = this.start.lerp(this.end, 1.0D - this.visual());
                this.ball.setPos(pos.x, pos.y, pos.z);

                this.fighters.values().removeIf(fighter -> {
                    if (fighter.participant().isStillFiring() && fighter.player().isAlive()) return false;
                    fighter.participant().beam().clearClashLock();
                    NetworkHandler.sendToPlayer(BeamClashStateS2C.inactive(0), fighter.player());
                    return true;
                });
                this.absorbProjectiles(level, pos);

                float push = 0.0F;
                for (Fighter fighter : this.fighters.values()) {
                    fighter.participant().tickMeter((int) (level.getGameTime() - fighter.joinGameTime()));
                    push += fighter.participant().momentum() * FIGHTER_PUSH;
                }
                float resist = COOLER_PUSH + COOLER_PUSH_PER_FIGHTER * this.fighters.size();
                this.bias = Mth.clamp(this.bias + DRIFT * (push - resist), 0.0F, 1.0F);

                for (Fighter fighter : this.fighters.values()) this.lockBeam(fighter, pos);
                this.sync(audience, pos);

                if (this.bias >= WIN_BIAS) return Result.PLAYERS;
                if (this.bias <= LOSE_BIAS) return Result.COOLER;
                if (this.age >= SUPERNOVA_STRUGGLE_TICKS) return this.bias > 0.5F ? Result.PLAYERS : Result.COOLER;
                return Result.ONGOING;
            }

            private void absorbProjectiles(ServerLevel level, Vec3 pos) {
                double radius = this.ballRadius();
                AABB area = new AABB(pos, pos).inflate(SEARCH_RANGE);
                for (AbstractKiProjectile projectile : level.getEntitiesOfClass(AbstractKiProjectile.class, area)) {
                    if (projectile == this.ball || projectile.isRemoved()) continue;
                    if (!(projectile.getOwner() instanceof ServerPlayer owner)) continue;
                    AbstractKiProjectile.ClashRole role = projectile.getClashRole();
                    if (role == AbstractKiProjectile.ClashRole.MAJOR) {
                        if (!projectile.isClashableBeam() || projectile.isClashLocked() || this.fighters.containsKey(owner.getUUID())) continue;
                        if (distanceToBeam(projectile, pos) > radius + projectile.getSize() + JOIN_PAD) continue;
                        this.join(level, owner, projectile);
                    } else if (projectile.position().distanceTo(pos) <= radius + 1.0D) {
                        this.bias = Mth.clamp(this.bias + SMALL_HIT_PUSH, 0.0F, 1.0F);
                        level.sendParticles(ParticleTypes.POOF, projectile.getX(), projectile.getY(), projectile.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.02D);
                        projectile.discard();
                    }
                }
            }

            private static double distanceToBeam(AbstractKiProjectile beam, Vec3 point) {
                Vec3 origin = beam.position();
                Vec3 dir = Vec3.directionFromRotation(beam.getClashPitch(), beam.getClashYaw());
                double along = Mth.clamp(point.subtract(origin).dot(dir), 0.0D, Math.max(0.1D, beam.getClashBeamLength()));
                return origin.add(dir.scale(along)).distanceTo(point);
            }

            private void join(ServerLevel level, ServerPlayer player, AbstractKiProjectile beam) {
                Fighter fighter = new Fighter(player, new ClashParticipant(beam, player), level.getGameTime(), System.nanoTime());
                this.fighters.put(player.getUUID(), fighter);
                player.displayClientMessage(Component.translatable("worldboss.dragonminez.metal_cooler_core.clash_joined"), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.PLAYERS, 1.5F, 1.2F);
            }

            private void lockBeam(Fighter fighter, Vec3 pos) {
                AbstractKiProjectile beam = fighter.participant().beam();
                Vec3 dir = Vec3.directionFromRotation(beam.getClashPitch(), beam.getClashYaw());
                double reach = pos.subtract(beam.position()).dot(dir) - this.ballRadius() * 0.6D;
                beam.setClashLock((float) Math.max(0.5D, reach), this.core.getUUID());
                beam.setMaxLife(beam.tickCount + KEEP_ALIVE_TICKS);
            }

            private void sync(List<ServerPlayer> audience, Vec3 pos) {
                float advantage = this.visual();
                for (Fighter fighter : this.fighters.values()) {
                    NetworkHandler.sendToPlayer(new BeamClashStateS2C(true, fighter.joinGameTime(), fighter.participant().meterSeed(), advantage,
                            fighter.participant().beam().getColorBorder(), SUPERNOVA_BORDER, this.core.getId(), pos.x, pos.y, pos.z, 0),
                            fighter.player());
                }
                if (this.shownFighters != this.fighters.size()) {
                    this.shownFighters = this.fighters.size();
                    this.bar.setName(Component.translatable("worldboss.dragonminez.metal_cooler_core.clash_bar", this.shownFighters));
                }
                this.bar.setProgress(advantage);
                syncBar(this.bar, audience);
            }

            void end(Boolean playersWon) {
                this.bar.removeAllPlayers();
                this.bar.setVisible(false);
                for (Fighter fighter : this.fighters.values()) {
                    AbstractKiProjectile beam = fighter.participant().beam();
                    beam.clearClashLock();
                    boolean lost = Boolean.FALSE.equals(playersWon);
                    if (Boolean.TRUE.equals(playersWon)) beam.setMaxLife(beam.tickCount + BREAKTHROUGH_TICKS);
                    else if (lost && !beam.isRemoved()) beam.discard();
                    if (lost) exhaust(fighter.player());
                    NetworkHandler.sendToPlayer(BeamClashStateS2C.inactive(lost ? EXHAUST_TICKS : 0), fighter.player());
                }
                this.fighters.clear();
            }

            private static void exhaust(ServerPlayer player) {
                player.removeEffect(MainEffects.STUN.get());
                player.addEffect(new MobEffectInstance(MainEffects.STUN.get(), EXHAUST_TICKS, 0, false, false, true));
                StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                    data.getStatus().setStunEffect(true);
                    data.getStatus().setBlocking(false);
                });
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            }

            @Override
            public boolean handlePress(ServerPlayer player, float pressTime, float marker) {
                Fighter fighter = this.fighters.get(player.getUUID());
                if (fighter == null) return false;
                float serverTime = (float) (player.level().getGameTime() - fighter.joinGameTime());
                float realElapsed = (System.nanoTime() - fighter.joinNanos()) / 50_000_000.0F;
                ClashMeter.Grade grade = fighter.participant().registerPlayerPress(pressTime, marker, serverTime, realElapsed, player.latency);
                BeamClashManager.playPressFeedback(player, grade);
                return true;
            }

            @Override
            public boolean involves(UUID ownerId) {
                return this.fighters.containsKey(ownerId);
            }
        }
    }

    public static class MetalCooler extends DBSagasEntity {

        public static final float GUARDIAN_HEALTH = 9360.0F;
        public static final float GUARDIAN_MELEE = 1080.0F;
        public static final float GUARDIAN_KI = 936.0F;
        private static final float SCOUT_RATIO = 0.75F;
        private static final int REGEN_DELAY = 80;
        private static final int REGEN_INTERVAL = 20;
        private static final float REGEN_RATIO = 0.015F;

        private boolean scout;
        private int calmTicks;

        public MetalCooler(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.2F);
            this.addKiSkill(KiSkillType.KI_LASER, 70, 1.0F, 0xFF59FF, 0xD859FF, 0x9238F2);
            this.setZanzoken(1, 60);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.34D);
            this.setDefaultMovementSpeed(0.34D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.4D);
            this.applyStats(1.0F);
            this.setAiTier(AiTier.ELITE);
            this.getPersistentData().putBoolean("dmz_stats_configured", true);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_cooler";
        }

        @Override
        public String getGeckolibTextureName() {
            return "saga_metal_cooler";
        }

        private void applyStats(float ratio) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(GUARDIAN_HEALTH * ratio);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(GUARDIAN_MELEE * ratio);
            this.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get()).setBaseValue(GUARDIAN_KI * ratio);
            this.setKiBlastDamage(GUARDIAN_KI * ratio);
            this.setHealth(this.getMaxHealth());
        }

        public void makeScout() {
            this.scout = true;
            this.applyStats(SCOUT_RATIO);
            this.setAiTier(AiTier.SMART);
        }

        public boolean isScout() {
            return this.scout;
        }

        @Override
        public boolean removeWhenFarAway(double distanceToClosestPlayer) {
            return this.scout && super.removeWhenFarAway(distanceToClosestPlayer);
        }

        @Override
        public boolean isAlliedTo(Entity pEntity) {
            return GeteStarEntities.sameFaction(this, pEntity) || super.isAlliedTo(pEntity);
        }

        @Override
        public boolean hurt(DamageSource pSource, float pAmount) {
            if (GeteStarEntities.sameFaction(this, pSource.getEntity())) return false;
            boolean hurt = super.hurt(pSource, pAmount);
            if (hurt) this.calmTicks = 0;
            return hurt;
        }

        @Override
        public void tick() {
            super.tick();
            if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;
            if (++this.calmTicks < REGEN_DELAY || this.tickCount % REGEN_INTERVAL != 0) return;
            if (this.getHealth() >= this.getMaxHealth()) return;
            this.heal(this.getMaxHealth() * REGEN_RATIO);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0D, this.getZ(), 4, 0.3D, 0.6D, 0.3D, 0.02D);
        }

        public void vanish() {
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(MainParticles.STARDUST.get(), this.getX(), this.getY() + 1.0D, this.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.1D);
            }
            this.discard();
        }

        @Override
        public void die(DamageSource pCause) {
            super.die(pCause);
            if (!(this.level() instanceof ServerLevel level) || this.scout) return;
            MetalCoolerCore core = GeteStarEntities.coreOf(this);
            if (core == null || core.isBossAsleep()) return;
            GeteStarEntities.Scrap scrap = GeteStarEntities.Scrap.create(level, core, this.position());
            if (scrap != null) level.addFreshEntity(scrap);
        }

        @Override
        protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit) {
            super.dropCustomDeathLoot(pSource, pLooting, pRecentlyHit);
            if (this.scout) this.spawnAtLocation(new ItemStack(MainItems.GETE_SCRAP.get(), 1 + this.random.nextInt(2 + pLooting)));
        }

        @Override
        public void addAdditionalSaveData(CompoundTag pCompound) {
            super.addAdditionalSaveData(pCompound);
            pCompound.putBoolean("GeteScout", this.scout);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag pCompound) {
            super.readAdditionalSaveData(pCompound);
            this.scout = pCompound.getBoolean("GeteScout");
            if (this.scout) this.setAiTier(AiTier.SMART);
        }
    }

    public static class MetalCoolerCopy extends DBSagasEntity {

        public static final float STAT_RATIO = 0.03F;
        private static final float SAGA_HEALTH = 156000.0F;
        private static final float SAGA_MELEE = 5400.0F;
        private static final float SAGA_KI = 4680.0F;
        private static final EntityDataAccessor<Integer> LATCHED =
                SynchedEntityData.defineId(MetalCoolerCopy.class, EntityDataSerializers.INT);
        private static final RawAnimation LATCH_ANIMATION = RawAnimation.begin().thenPlayAndHold("skp.grab");
        private static final int LATCH_TICKS = 120;
        private static final int LATCH_COOLDOWN = 100;
        private static final double LATCH_RANGE = 2.2D;
        private static final double LATCH_RADIUS = 0.75D;
        private static final int MAX_PER_VICTIM = 3;

        private int latchTicks;
        private int latchCooldown;

        public MetalCoolerCopy(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.0F);
            this.setScaleVal(0.9F);
            this.addKiSkill(KiSkillType.KI_LASER, 220, 0.6F, 0xAA00FF, 0x6C00A3);
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(SAGA_HEALTH * STAT_RATIO);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(SAGA_MELEE * STAT_RATIO);
            this.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get()).setBaseValue(SAGA_KI * STAT_RATIO);
            this.setKiBlastDamage(SAGA_KI * STAT_RATIO);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.38D);
            this.setDefaultMovementSpeed(0.38D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0D);
            this.setHealth(this.getMaxHealth());
            this.setAiTier(AiTier.ELITE);
            this.getPersistentData().putBoolean("dmz_stats_configured", true);
        }

        @Override
        protected void defineSynchedData() {
            super.defineSynchedData();
            this.entityData.define(LATCHED, -1);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_cooler";
        }

        @Override
        public String getGeckolibTextureName() {
            return "saga_metal_cooler";
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            super.registerControllers(controllers);
            controllers.add(new AnimationController<>(this, "latch_controller", 3, state ->
                    this.isLatched() ? state.setAndContinue(LATCH_ANIMATION) : PlayState.STOP));
        }

        public int latchedId() {
            return this.entityData.get(LATCHED);
        }

        public boolean isLatched() {
            return this.latchedId() >= 0;
        }

        @Override
        public boolean isMeleeAllowed() {
            return !this.isLatched() && super.isMeleeAllowed();
        }

        @Override
        protected boolean brainMovementAllowed() {
            return !this.isLatched() && super.brainMovementAllowed();
        }

        @Override
        public boolean isSkillCastReady() {
            return !this.isLatched() && super.isSkillCastReady();
        }

        @Override
        public boolean isComboReady() {
            return !this.isLatched() && super.isComboReady();
        }

        @Override
        public boolean isAlliedTo(Entity pEntity) {
            return GeteStarEntities.sameFaction(this, pEntity) || super.isAlliedTo(pEntity);
        }

        @Override
        public boolean hurt(DamageSource pSource, float pAmount) {
            if (GeteStarEntities.sameFaction(this, pSource.getEntity())) return false;
            return super.hurt(pSource, pAmount);
        }

        @Override
        public void tick() {
            super.tick();
            if (this.level().isClientSide || !this.isAlive()) return;
            if (this.latchCooldown > 0) this.latchCooldown--;

            if (this.isLatched()) {
                this.tickLatch();
                return;
            }

            if (this.latchCooldown > 0 || !(this.getTarget() instanceof ServerPlayer victim)) return;
            if (this.distanceTo(victim) > LATCH_RANGE + victim.getBbWidth() * 0.5D || !HeldVictim.canHold(this, victim)) return;
            if (this.latchedOn(victim) >= MAX_PER_VICTIM) return;
            this.entityData.set(LATCHED, victim.getId());
            this.latchTicks = 0;
            this.setNoGravity(true);
            this.getNavigation().stop();
            this.playSound(MainSounds.GOLPE2.get(), 1.0F, 0.7F);
        }

        private int latchedOn(ServerPlayer victim) {
            int count = 0;
            for (MetalCoolerCopy other : this.level().getEntitiesOfClass(MetalCoolerCopy.class, victim.getBoundingBox().inflate(3.0D))) {
                if (other != this && other.latchedId() == victim.getId()) count++;
            }
            return count;
        }

        private void tickLatch() {
            Entity found = this.level().getEntity(this.latchedId());
            if (!(found instanceof ServerPlayer victim) || !victim.isAlive() || ++this.latchTicks > LATCH_TICKS || !HeldVictim.canHold(this, victim)
                    || StatsProvider.get(StatsCapability.INSTANCE, victim).map(data -> data.getStatus().isKnockedDown()).orElse(false)) {
                this.release(found instanceof LivingEntity living ? living : null);
                return;
            }
            double angle = Math.floorMod(this.getUUID().hashCode(), 6) * (Math.PI / 3.0D) + this.latchTicks * 0.03D;
            double x = victim.getX() + Math.cos(angle) * LATCH_RADIUS;
            double z = victim.getZ() + Math.sin(angle) * LATCH_RADIUS;
            this.setPos(x, victim.getY() + 0.15D, z);
            this.setDeltaMovement(Vec3.ZERO);
            float yaw = (float) (Mth.atan2(victim.getZ() - z, victim.getX() - x) * (180.0D / Math.PI)) - 90.0F;
            this.setYRot(yaw);
            this.setYBodyRot(yaw);
            this.setYHeadRot(yaw);
        }

        private void release(LivingEntity victim) {
            this.entityData.set(LATCHED, -1);
            this.setNoGravity(false);
            this.latchCooldown = LATCH_COOLDOWN;
            if (victim == null) return;
            Vec3 away = this.position().subtract(victim.position());
            if (away.horizontalDistanceSqr() < 1.0E-4D) away = this.getLookAngle().reverse();
            away = new Vec3(away.x, 0.0D, away.z).normalize();
            this.setDeltaMovement(away.x * 0.9D, 0.35D, away.z * 0.9D);
            this.hasImpulse = true;
        }

        public void vanish() {
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(MainParticles.STARDUST.get(), this.getX(), this.getY() + 1.0D, this.getZ(), 20, 0.3D, 0.6D, 0.3D, 0.1D);
            }
            this.discard();
        }
    }
}
