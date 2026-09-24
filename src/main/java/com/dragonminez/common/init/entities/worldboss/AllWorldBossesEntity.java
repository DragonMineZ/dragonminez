package com.dragonminez.common.init.entities.worldboss;

import com.dragonminez.common.combat.util.SwordSlashManager;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.entities.ai.AiTier;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasPart;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.BossTelegraphS2C;
import com.dragonminez.common.network.S2C.KiBurstVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import com.dragonminez.server.world.worldboss.WorldBossContribution;
import com.dragonminez.server.world.worldboss.WorldBossManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

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
            return WorldBossManager.JANEMBA;
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
        private static final double CUT_HIT_RADIUS = 2.6D;
        private static final float CUT_DAMAGE_RATIO = 0.6F;
        private static final float CUT_SCALE = 3.4F;
        private static final int CUT_COLOR = 0xFF1E2D;

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
            return WorldBossManager.JANEMBA;
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
                    CUT_COLOR, CUT_SPEED, CUT_MAX_DISTANCE, CUT_HIT_RADIUS, victim -> {
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
}
