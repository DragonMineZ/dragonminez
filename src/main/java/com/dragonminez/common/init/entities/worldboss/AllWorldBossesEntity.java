package com.dragonminez.common.init.entities.worldboss;

import com.dragonminez.common.combat.util.SwordSlashManager;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.entities.ai.AiTier;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.KillDriverEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasPart;
import com.dragonminez.common.init.entities.sagas.helper.DBSagasAnimations;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.BossTelegraphS2C;
import com.dragonminez.common.network.S2C.KiBurstVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import com.dragonminez.server.events.players.combat.HeldVictim;
import com.dragonminez.server.events.players.combat.KnockbackHelper;
import com.dragonminez.server.events.players.combat.MeteorBurstCombo;
import com.dragonminez.server.events.players.combat.MomentumImpactHandler;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import com.dragonminez.server.world.worldboss.WorldBossContribution;
import com.dragonminez.server.world.worldboss.WorldBossManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
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
}
