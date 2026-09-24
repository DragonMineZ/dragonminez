package com.dragonminez.common.init.entities.ai;

import com.dragonminez.common.combat.clash.BeamClashManager;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.ComboType;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.KiSkill;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.KiSkillType;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.LocomotionMode;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.SkillRole;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.Tier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EnemyBrain {

    public static final double MID_RANGE = 12.0D;
    public static final double OUT_RANGE = 28.0D;
    private static final double THREAT_SCAN_RADIUS = 40.0D;
    private static final double ALLY_SCAN_RADIUS = 24.0D;
    private static final double APPROACH_THRESHOLD = 0.05D;
    private static final int[] STUN_COMBOS = {1, 3, 8};
    private static final int[] PRESSURE_COMBOS = {0, 8};
    private static final int[] HEAVY_COMBOS = {9, 11, 3, 1};
    private static final int[] DEFAULT_COMBOS = {0, 1, 8, 2, 5, 6};
    private static final int COMBO_RECOVERY = 7;

    private enum Kind { MELEE, COMBO, CAST, APPROACH, FLANK, STRAFE, KITE, TELEPORT, HOLD }

    private static final class Option {
        final Kind kind;
        KiSkill skill;
        int comboId = -1;
        LocomotionMode mode = LocomotionMode.RUN;
        Vec3 point;
        int dir;
        double score;

        Option(Kind kind, double score) {
            this.kind = kind;
            this.score = score;
        }
    }

    private static final class Situation {
        LivingEntity target;
        double dist;
        double verticalDiff;
        boolean los;
        double closingSpeed;
        float selfHp;
        float targetHp;
        PlayerRead read;
        boolean targetAirborne;
        double meleeRange;
        int engaging;
        int meleeCrowd;
        boolean inFiringLine;
        boolean allyCastingHeavy;
        Vec3 flankPoint;
        boolean tournamentBound;
        boolean giant;
        float heatFrac;
        boolean forcedMelee;
        boolean selfBlinded;
        double targetSpeed;
        int rivalsNear;

        boolean approaching() { return this.closingSpeed > APPROACH_THRESHOLD; }
        boolean retreating() { return this.closingSpeed < -APPROACH_THRESHOLD; }
    }

    private final DBSagasEntity self;
    private final CombatMemory memory = new CombatMemory();
    private final SkillEconomy economy;

    private int decisionCooldown;
    private int threatScanCooldown;
    private int allyRefresh;
    private int targetReviewCooldown;
    private IncomingThreat pendingThreat;
    private int reactionTimer;
    private List<Mob> allies = new ArrayList<>();
    private Vec3 lastTargetPos;
    private Vec3 targetVelocity = Vec3.ZERO;
    private UUID trackedTargetId;
    private float castAimYawError;
    private float castAimPitchError;
    private int queuedComboWindow;

    public EnemyBrain(DBSagasEntity self) {
        this.self = self;
        this.economy = new SkillEconomy(self::getAiProfile);
    }

    public CombatMemory memory() {
        return this.memory;
    }

    public SkillEconomy economy() {
        return this.economy;
    }

    public Vec3 targetVelocity() {
        return this.targetVelocity;
    }

    public void inherit(EnemyBrain other) {
        if (other == null) return;
        this.memory.inherit(other.memory);
        this.economy.inherit(other.economy);
        this.targetVelocity = other.targetVelocity;
        this.lastTargetPos = other.lastTargetPos;
        this.trackedTargetId = other.trackedTargetId;
    }

    private long now() {
        return this.self.level().getGameTime();
    }

    private boolean active() {
        if (!this.self.isAlive() || this.self.isTransforming() || this.self.isStunned()) return false;
        if (this.self.isCombatFrozen() || this.self.isRaidDormant() || this.self.isBossAsleep()) return false;
        return this.self.getBossAbility() < 0;
    }

    public void tick() {
        if (this.self.level().isClientSide) return;
        long now = this.now();
        AiProfile profile = this.self.getAiProfile();
        this.economy.tick(now);
        this.memory.resolve(now);

        LivingEntity target = this.self.getTarget();
        this.trackTargetVelocity(target);

        if (!this.active()) {
            this.pendingThreat = null;
            return;
        }

        if (target == null || !target.isAlive()) {
            this.self.cancelManeuver();
            this.self.setMeleeAllowed(true);
            this.self.setLocomotionMode(LocomotionMode.WALK);
            this.pendingThreat = null;
            return;
        }

        if (--this.targetReviewCooldown <= 0) {
            this.targetReviewCooldown = 20;
            this.reviewTarget(now, profile);
            target = this.self.getTarget();
            if (target == null) return;
        }

        if (--this.allyRefresh <= 0) {
            this.allyRefresh = 10;
            this.allies = SquadCoordinator.allies(this.self, ALLY_SCAN_RADIUS);
        }

        this.observeTarget(target, now);

        if (--this.threatScanCooldown <= 0) {
            this.threatScanCooldown = profile.threatScanInterval;
            this.scanThreats(now, profile);
        }

        if (this.pendingThreat != null && --this.reactionTimer <= 0) {
            IncomingThreat threat = this.pendingThreat;
            this.pendingThreat = null;
            if (threat.stillValid()) this.reactToThreat(threat, now, profile);
        }

        if (this.queuedComboWindow > 0) this.queuedComboWindow--;

        boolean clashing = BeamClashManager.isClashing(this.self.getUUID());
        boolean busy = this.self.isCasting() || this.self.isComboing() || this.self.isZanzoken()
                || this.self.isEvading() || this.self.isManeuvering() || clashing;
        if (busy) return;

        if (--this.decisionCooldown <= 0) {
            this.decisionCooldown = profile.decisionInterval;
            this.decideOffense(target, now, profile);
        }
    }

    private void trackTargetVelocity(LivingEntity target) {
        if (target == null) {
            this.trackedTargetId = null;
            this.lastTargetPos = null;
            this.targetVelocity = Vec3.ZERO;
            return;
        }
        Vec3 pos = target.position();
        if (this.trackedTargetId == null || !this.trackedTargetId.equals(target.getUUID()) || this.lastTargetPos == null) {
            this.trackedTargetId = target.getUUID();
            this.lastTargetPos = pos;
            this.targetVelocity = Vec3.ZERO;
            return;
        }
        Vec3 instant = pos.subtract(this.lastTargetPos);
        if (instant.lengthSqr() > 64.0D) instant = Vec3.ZERO;
        this.targetVelocity = this.targetVelocity.scale(0.6D).add(instant.scale(0.4D));
        this.lastTargetPos = pos;
    }

    private void reviewTarget(long now, AiProfile profile) {
        double range = this.self.getAttributes().hasAttribute(Attributes.FOLLOW_RANGE)
                ? this.self.getAttributeValue(Attributes.FOLLOW_RANGE) : 32.0D;
        LivingEntity chosen = TargetSelector.choose(this.self, profile, this.memory, now, range, this::canTarget);

        if (profile.tier == AiTier.SMART && this.memory.ticksSinceAttacked(now) > 100) {
            LivingEntity focus = SquadCoordinator.sharedFocus(this.allies);
            if (focus != null && focus != this.self.getTarget() && this.canTarget(focus)
                    && SquadCoordinator.engaging(this.allies, focus).size() >= 2
                    && this.self.distanceTo(focus) <= range && this.self.getRandom().nextFloat() < 0.5F) {
                chosen = focus;
            }
        }

        if (chosen != null && chosen != this.self.getTarget()) this.self.setTarget(chosen);
        LivingEntity current = this.self.getTarget();
        this.memory.lockTarget(current != null ? current.getUUID() : null, now);
    }

    private boolean canTarget(LivingEntity candidate) {
        if (candidate == null || candidate == this.self) return false;
        if (this.self.isQuestTeammate(candidate)) return false;
        if (!this.self.isValidRaidTarget(candidate)) return false;
        if (candidate instanceof Mob mob && SquadCoordinator.isAlly(this.self, mob)) return false;
        if (PlayerRead.of(candidate).knockedDown) return false;
        return this.self.brainCanTarget(candidate);
    }

    private void observeTarget(LivingEntity target, long now) {
        if (!(target instanceof ServerPlayer)) return;
        PlayerRead read = PlayerRead.of(target);
        this.memory.observeCharge(target.getUUID(), read.casting, read.chargePercent, now);
    }

    private void scanThreats(long now, AiProfile profile) {
        List<IncomingThreat> threats = ThreatScanner.scan(this.self, THREAT_SCAN_RADIUS,
                id -> this.memory.lastChargeSeen(id, now),
                (projectile, owner) -> {
                    if (this.memory.isSeenProjectile(projectile.getUUID(), now)) return;
                    this.memory.markSeenProjectile(projectile.getUUID(), now);
                    if (projectile.isFiring() || projectile.getDeltaMovement().lengthSqr() > 0.0025D) {
                        this.memory.observeTechniqueFired(owner.getUUID(), projectile.getKiDamage(), now);
                    }
                });
        if (threats.isEmpty()) return;

        float maxHealth = this.self.getMaxHealth();
        for (IncomingThreat threat : threats) {
            boolean pending = threat.isPending();
            if (pending && !profile.prePositionsForCharge) continue;
            if (pending ? this.memory.isKnownPending(threat.id(), now) : this.memory.isKnownThreat(threat.id(), now)) continue;
            if (this.pendingThreat != null && this.pendingThreat.stillValid()
                    && this.pendingThreat.urgency(maxHealth) >= threat.urgency(maxHealth)) {
                return;
            }
            if (pending) this.memory.markPending(threat.id(), now);
            else this.memory.markThreat(threat.id(), now);
            this.pendingThreat = threat;
            int delay = profile.reactionDelay;
            if (threat.eta >= 0) delay = Math.min(delay, Math.max(0, threat.eta - 1));
            this.reactionTimer = delay;
            return;
        }
    }

    private boolean canZanzokenNow(long now, AiProfile profile) {
        return profile.zanzoken && this.self.isZanzokenReady() && this.economy.canUseMobility(now);
    }

    private void zanzoken(long now, AiProfile profile) {
        this.self.cancelManeuver();
        this.self.performZanzoken();
        this.self.scaleZanzokenCooldown(profile.zanzokenCooldownMultiplier);
        this.economy.onMobilityUse(now);
        this.memory.noteZanzoken(now);
    }

    private boolean tryDefensiveBarrier(long now) {
        if (this.memory.ticksSinceDefensiveBarrier(now) < 300) return false;
        for (KiSkill skill : this.self.getSkillPool()) {
            if (skill.role != SkillRole.DEFENSIVE || skill.currentCooldown > 0) continue;
            if (!this.economy.canCast(skill, now, true)) continue;
            this.self.cancelManeuver();
            if (this.self.startSkillReactive(skill)) return true;
        }
        return false;
    }

    private void reactToThreat(IncomingThreat threat, long now, AiProfile profile) {
        RandomSource rnd = this.self.getRandom();
        float frac = threat.damageFraction(this.self.getMaxHealth());
        boolean giant = this.self.hasGiantBody();

        if (threat.isPending()) {
            if (giant || threat.attacker == null) return;
            PlayerRead read = PlayerRead.of(threat.attacker);
            if (read.committedCast) return;
            if (rnd.nextFloat() < 0.5F && this.memory.ticksSinceSidestep(now) > 40) {
                this.self.performSidestep(threat.approachDir);
                this.memory.noteSidestep(now);
            }
            return;
        }

        if (this.self.isCasting() || this.self.isComboing()) {
            if (profile.tier.atLeast(AiTier.ELITE) && frac >= profile.panicDamageFraction && this.canZanzokenNow(now, profile)) {
                if (this.self.isCasting()) this.self.stopCasting();
                if (this.self.isComboing()) this.self.interruptCombo();
                this.zanzoken(now, profile);
            }
            return;
        }

        if (profile.tier == AiTier.NOVICE) {
            if (giant) return;
            if (threat.originDistance >= profile.dodgeMinOriginDistance && threat.charge >= profile.dodgeMinCharge
                    && threat.sidestepWorks && rnd.nextFloat() < profile.dodgeChance) {
                this.self.performSidestep(threat.approachDir);
                this.memory.noteSidestep(now);
            }
            return;
        }

        if (frac < profile.dodgeMinDamageFraction && threat.kind != IncomingThreat.Kind.EXPLOSION) return;
        if (rnd.nextFloat() >= profile.dodgeChance) return;

        UUID attackerId = threat.attacker != null ? threat.attacker.getUUID() : null;
        boolean wantZanzoken = this.canZanzokenNow(now, profile);
        if (wantZanzoken && profile.saveMobility && frac < profile.panicDamageFraction && attackerId != null
                && this.memory.expectsStrongerSoon(attackerId, threat.damage, this.self.getZanzokenCooldownMax(), now)) {
            wantZanzoken = false;
        }
        if (wantZanzoken && profile.tier.atLeast(AiTier.ELITE) && frac < profile.panicDamageFraction
                && threat.sidestepWorks && rnd.nextFloat() < 0.5F) {
            wantZanzoken = false;
        }

        if (wantZanzoken && !giant) {
            this.zanzoken(now, profile);
            return;
        }

        if (profile.defensiveBarrier && threat.kind != IncomingThreat.Kind.EXPLOSION && threat.eta >= 3
                && frac >= profile.dodgeMinDamageFraction * 1.5F && this.tryDefensiveBarrier(now)) {
            return;
        }

        if (giant) return;

        if (threat.kind == IncomingThreat.Kind.EXPLOSION) {
            if (profile.dash && this.self.isDashReady()) this.self.performDashAway(threat.approachDir);
            else this.self.performSidestep(threat.approachDir);
            this.memory.noteSidestep(now);
            return;
        }

        if (threat.sidestepWorks) {
            this.self.performSidestep(threat.approachDir);
            this.memory.noteSidestep(now);
            return;
        }

        if (this.self.isBackstepReady()) {
            this.self.performBackstep();
            this.memory.noteBackstep(now);
        }
    }

    public void onHurt(DamageSource source, float amount) {
        if (this.self.level().isClientSide) return;
        long now = this.now();
        Entity attacker = source.getEntity();
        UUID attackerId = attacker != null ? attacker.getUUID() : null;
        this.memory.recordDamageTaken(amount, attackerId, now);

        if (!this.active()) return;
        if (this.self.isCasting() || this.self.isComboing() || this.self.isZanzoken() || this.self.isEvading()) return;
        if (this.self.hasGiantBody()) return;

        AiProfile profile = this.self.getAiProfile();
        RandomSource rnd = this.self.getRandom();
        float recent = this.memory.damageTakenWithin(60, now);
        float frac = this.self.getMaxHealth() > 0.0F ? recent / this.self.getMaxHealth() : 0.0F;
        boolean directHit = source.getDirectEntity() != null && source.getDirectEntity() == source.getEntity();

        if (frac >= 0.12F && this.canZanzokenNow(now, profile)) {
            float chance = profile.tier.atLeast(AiTier.ELITE) ? 0.6F : 0.35F;
            boolean save = profile.saveMobility && attackerId != null
                    && this.memory.expectsStrongerSoon(attackerId, recent * 2.0F, this.self.getZanzokenCooldownMax(), now);
            if (!save && rnd.nextFloat() < chance) {
                this.zanzoken(now, profile);
                return;
            }
        }

        if (directHit && this.self.isBackstepReady() && this.memory.ticksSinceBackstep(now) > profile.backstepCooldown) {
            float chance = switch (profile.tier) {
                case NOVICE -> 0.35F;
                case SMART -> 0.5F;
                case ELITE -> 0.6F;
                case EXPERT -> 0.7F;
            };
            if (rnd.nextFloat() < chance) {
                this.self.cancelManeuver();
                this.self.performBackstep();
                this.memory.noteBackstep(now);
            }
        }
    }

    public void onDealtDamage(LivingEntity victim, float amount, boolean ki) {
        long now = this.now();
        boolean comboing = this.self.isComboing();
        this.memory.onDealtDamage(now, ki, comboing);
        if (!ki && !comboing) this.economy.onMeleeHit();
        if (victim != null && PlayerRead.of(victim).blocking) this.memory.recordTargetBlocked(now);
    }

    public void onCastStarted(int skillId) {
        long now = this.now();
        AiProfile profile = this.self.getAiProfile();
        RandomSource rnd = this.self.getRandom();
        for (KiSkill skill : this.self.getSkillPool()) {
            if (skill.id == skillId) {
                this.economy.onCast(skill, now);
                break;
            }
        }
        this.memory.recordCast(skillId, now);
        this.castAimYawError = (rnd.nextFloat() * 2.0F - 1.0F) * profile.aimErrorDegrees;
        this.castAimPitchError = (rnd.nextFloat() * 2.0F - 1.0F) * profile.aimErrorDegrees * 0.5F;
    }

    public void onComboStarted(int comboId) {
        long now = this.now();
        this.economy.onCombo(ComboType.tierOf(comboId), now);
        this.memory.recordCombo(comboId, now);
    }

    public void onMeleeResult(boolean hit) {
        this.memory.recordMeleeResult(hit, this.now());
        if (hit) this.economy.onMeleeHit();
    }

    public void onAbsorb(float amount) {
        this.economy.refund(1.5F);
        this.self.refundSkillCooldowns(0.15F);
    }

    public void aimDuringCast(LivingEntity target, int skill) {
        if (target == null) return;
        AiProfile profile = this.self.getAiProfile();
        double projectileSpeed = this.estimateProjectileSpeed(skill);
        Vec3 point = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        if (profile.leadAim && profile.aimLeadFactor > 0.0F) {
            double dist = this.self.distanceTo(target);
            double lead = Mth.clamp(dist / Math.max(0.3D, projectileSpeed), 0.0D, 25.0D) * profile.aimLeadFactor;
            point = point.add(this.targetVelocity.scale(lead));
        }
        if (this.castAimYawError != 0.0F || this.castAimPitchError != 0.0F) {
            Vec3 eye = this.self.getEyePosition();
            Vec3 rel = point.subtract(eye);
            double len = rel.length();
            if (len > 1.0E-3D) {
                double yaw = Math.atan2(rel.z, rel.x) + Math.toRadians(this.castAimYawError);
                double horiz = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
                double pitch = Math.atan2(rel.y, horiz) + Math.toRadians(this.castAimPitchError);
                double cosP = Math.cos(pitch);
                point = eye.add(Math.cos(yaw) * cosP * len, Math.sin(pitch) * len, Math.sin(yaw) * cosP * len);
            }
        }
        AimHelper.face(this.self, point);
    }

    private double estimateProjectileSpeed(int skill) {
        SkillRole role = KiSkillType.roleOf(skill);
        double base = Math.max(0.3D, this.self.getKiBlastSpeed());
        return switch (role) {
            case HITSCAN -> 6.0D;
            case RANGED_TRAVEL -> base * 2.5D;
            case PROJECTILE_FAST, ZONING -> base * 1.5D;
            default -> base;
        };
    }

    private Situation snapshot(LivingEntity target, long now) {
        Situation s = new Situation();
        s.target = target;
        s.dist = this.self.distanceTo(target);
        s.verticalDiff = target.getY() - this.self.getY();
        s.los = this.self.getSensing().hasLineOfSight(target);
        Vec3 toSelf = this.self.position().subtract(target.position());
        Vec3 toSelfNorm = toSelf.lengthSqr() > 1.0E-6D ? toSelf.normalize() : Vec3.ZERO;
        s.closingSpeed = this.targetVelocity.dot(toSelfNorm);
        s.targetSpeed = this.targetVelocity.horizontalDistance();
        s.selfHp = this.self.getMaxHealth() > 0.0F ? this.self.getHealth() / this.self.getMaxHealth() : 0.0F;
        s.targetHp = target.getMaxHealth() > 0.0F ? target.getHealth() / target.getMaxHealth() : 0.0F;
        s.read = PlayerRead.of(target);
        s.targetAirborne = !target.onGround() && s.verticalDiff > 3.0D;
        s.meleeRange = this.self.getMeleeReach() + target.getBbWidth() * 0.5D;
        s.engaging = SquadCoordinator.engaging(this.allies, target).size();
        s.meleeCrowd = SquadCoordinator.meleeCrowd(this.allies, target);
        s.inFiringLine = SquadCoordinator.inFiringLine(this.self, target, this.allies);
        s.allyCastingHeavy = SquadCoordinator.allyCastingHeavy(this.allies, target);
        s.flankPoint = s.engaging > 0 ? SquadCoordinator.flankPoint(this.self, target, this.allies, Math.max(2.5D, s.meleeRange - 1.0D)) : null;
        s.tournamentBound = this.self.isTournamentBound();
        s.giant = this.self.hasGiantBody();
        s.heatFrac = this.economy.heatFraction();
        s.forcedMelee = this.economy.inForcedMelee(now);
        s.selfBlinded = this.self.isBlindedByTaiyoken();
        s.rivalsNear = 0;
        for (ServerPlayer rival : TargetSelector.rivalParty(target)) {
            if (rival.isAlive() && rival.distanceTo(this.self) <= MID_RANGE) s.rivalsNear++;
        }
        return s;
    }

    private void decideOffense(LivingEntity target, long now, AiProfile profile) {
        Situation s = this.snapshot(target, now);
        RandomSource rnd = this.self.getRandom();
        List<Option> options = new ArrayList<>();

        if (s.selfBlinded) {
            this.self.cancelManeuver();
            this.self.setMeleeAllowed(true);
            if (!s.giant && rnd.nextFloat() < 0.3F) this.self.performSidestep(this.self.getLookAngle());
            return;
        }

        boolean blindOpening = this.memory.blindedRecently(now) && this.memory.ticksSinceLastCast(now) < 120
                && this.memory.lastCastId() == 25;

        if (s.dist <= s.meleeRange + 1.5D) {
            Option melee = new Option(Kind.MELEE, 1.0D);
            if (s.read.helpless()) melee.score += 0.8D;
            if (s.read.blocking && profile.readsGuard) melee.score -= 0.7D;
            if (s.forcedMelee) melee.score += 0.6D;
            if (s.meleeCrowd >= 2 && profile.flank) melee.score -= 0.5D;
            if (s.targetAirborne && !this.self.canFly()) melee.score -= 0.8D;
            if (this.memory.meleeWhiffStreak() >= 3) melee.score -= 0.4D;
            if (blindOpening) melee.score += 0.4D;
            options.add(melee);
        }

        boolean grace = this.self.isInSkillGracePeriod();
        int[] allowedCombos = this.self.getAllowedCombos();
        if (!grace && allowedCombos != null && allowedCombos.length > 0 && this.self.isComboReady()) {
            if (s.selfHp < 0.25F && !s.approaching() && s.dist > s.meleeRange && hasCombo(allowedCombos, COMBO_RECOVERY)
                    && this.economy.canCombo(ComboType.tierOf(COMBO_RECOVERY), now)) {
                Option recover = new Option(Kind.COMBO, 1.4D);
                recover.comboId = COMBO_RECOVERY;
                options.add(recover);
            }
            if (s.dist <= s.meleeRange) {
                int comboId = this.pickCombo(allowedCombos, s, profile, rnd);
                if (comboId >= 0 && comboId != COMBO_RECOVERY) {
                    Tier tier = ComboType.tierOf(comboId);
                    if (this.economy.canCombo(tier, now)) {
                        Option combo = new Option(Kind.COMBO, profile.tier == AiTier.NOVICE ? 1.0D : 1.15D);
                        combo.comboId = comboId;
                        if (s.read.helpless()) combo.score += 0.6D;
                        if (s.read.casting) combo.score += 0.5D;
                        if (s.read.blocking && profile.readsGuard) combo.score += 0.3D;
                        if (tier == Tier.STRONG && !s.read.exposed() && profile.tier.atLeast(AiTier.ELITE)) combo.score -= 0.3D;
                        if (profile.memory) {
                            CombatMemory.Outcome last = this.memory.lastOutcome(comboId, true);
                            if (last == CombatMemory.Outcome.HIT) combo.score += 0.15D;
                            else if (last == CombatMemory.Outcome.MISS) combo.score -= 0.2D;
                        }
                        if (this.queuedComboWindow > 0) combo.score += 0.8D;
                        if (blindOpening) combo.score += 0.4D;
                        options.add(combo);
                    }
                }
            }
        }

        if (!grace && this.self.isSkillCastReady() && !s.selfBlinded) {
            int maxCooldown = 1;
            for (KiSkill skill : this.self.getSkillPool()) maxCooldown = Math.max(maxCooldown, skill.cooldownMax);
            for (KiSkill skill : this.self.getSkillPool()) {
                if (skill.currentCooldown > 0) continue;
                if (!this.economy.canCast(skill, now)) continue;
                double fit = this.skillScore(skill, s, profile, now, maxCooldown);
                if (fit <= 0.0D) continue;
                Option cast = new Option(Kind.CAST, fit);
                cast.skill = skill;
                options.add(cast);
            }
        }

        if (s.dist > s.meleeRange) {
            Option approach = new Option(Kind.APPROACH, 0.75D);
            approach.mode = LocomotionMode.RUN;
            if (profile.dash && this.self.isDashReady() && s.dist >= 7.0D && s.dist <= 22.0D
                    && (s.retreating() || s.read.committedCast || s.heatFrac > 0.6F)) {
                approach.mode = LocomotionMode.DASH;
                approach.score += 0.25D;
            }
            if (s.read.committedCast && profile.punishCommittedCast) approach.score += 0.6D;
            if (s.targetAirborne && !this.self.canFly()) approach.score = 0.3D;
            options.add(approach);

            if (profile.flank && s.flankPoint != null && s.meleeCrowd >= 1 && !s.giant) {
                Option flank = new Option(Kind.FLANK, 0.9D);
                flank.point = s.flankPoint;
                options.add(flank);
            }
        }

        if (profile.proactiveTeleport && this.self.isWildSenseReady() && this.economy.canUseMobility(now)
                && this.memory.ticksSinceTeleport(now) > 100 && !s.giant) {
            double score = 0.0D;
            if (s.dist > OUT_RANGE) score = s.retreating() ? 1.0D : 0.6D;
            if (profile.punishCommittedCast && s.read.committedCast && s.dist > s.meleeRange) score = Math.max(score, 1.3D);
            if (profile.tier == AiTier.EXPERT && s.meleeCrowd >= 1 && s.dist > s.meleeRange) score = Math.max(score, 0.9D);
            if (score > 0.0D) options.add(new Option(Kind.TELEPORT, score));
        }

        if (profile.strafe && !s.giant && s.dist <= s.meleeRange + 2.5D && !s.forcedMelee) {
            Option strafe = new Option(Kind.STRAFE, 0.55D);
            if (s.heatFrac > 0.5F) strafe.score += 0.3D;
            if (this.memory.ticksSinceMeleeHit(now) < 10) strafe.score += 0.3D;
            if (s.read.blocking && profile.readsGuard) strafe.score += 0.25D;
            if (s.meleeCrowd >= 2) strafe.score -= 0.3D;
            strafe.dir = rnd.nextBoolean() ? 1 : -1;
            options.add(strafe);
        }

        if (profile.kite && !s.giant && !s.tournamentBound && s.dist <= MID_RANGE) {
            double score = 0.0D;
            if (s.selfHp < 0.35F && s.approaching()) score = 0.9D;
            if (this.memory.ticksSinceComboHit(now) < 30 && this.hasReadyRole(SkillRole.RANGED_TRAVEL, SkillRole.HITSCAN)) score = Math.max(score, 1.1D);
            if (s.heatFrac > 0.75F && s.dist <= s.meleeRange && s.selfHp < 0.6F) score = Math.max(score, 0.6D);
            if (profile.tier == AiTier.EXPERT && s.read.casting && !s.read.committedCast && s.dist <= s.meleeRange) score = 0.0D;
            if (score > 0.0D) options.add(new Option(Kind.KITE, score));
        }

        options.add(new Option(Kind.HOLD, 0.1D));

        Option best = null;
        for (Option option : options) {
            option.score += (rnd.nextDouble() * 2.0D - 1.0D) * profile.decisionNoise;
            if (best == null || option.score > best.score) best = option;
        }
        if (best == null) return;
        this.execute(best, s, profile, rnd, now);
    }

    private boolean hasReadyRole(SkillRole... roles) {
        if (!this.self.isSkillCastReady()) return false;
        for (KiSkill skill : this.self.getSkillPool()) {
            if (skill.currentCooldown > 0) continue;
            for (SkillRole role : roles) if (skill.role == role) return true;
        }
        return false;
    }

    private static boolean hasCombo(int[] allowed, int id) {
        for (int a : allowed) if (a == id) return true;
        return false;
    }

    private static int firstAllowed(int[] allowed, int[] preferred) {
        for (int pref : preferred) if (hasCombo(allowed, pref)) return pref;
        return -1;
    }

    private int pickCombo(int[] allowed, Situation s, AiProfile profile, RandomSource rnd) {
        if (profile.tier == AiTier.NOVICE) {
            int pick = allowed[rnd.nextInt(allowed.length)];
            return pick == COMBO_RECOVERY && allowed.length > 1 ? allowed[(rnd.nextInt(allowed.length))] : pick;
        }
        int pick = -1;
        if (s.read.casting) pick = firstAllowed(allowed, STUN_COMBOS);
        if (pick < 0 && s.read.exposed()) pick = firstAllowed(allowed, HEAVY_COMBOS);
        if (pick < 0 && s.read.blocking && profile.readsGuard) pick = firstAllowed(allowed, PRESSURE_COMBOS);
        if (pick < 0) {
            if (profile.memory) {
                double bestWeight = -1.0D;
                for (int candidate : allowed) {
                    if (candidate == COMBO_RECOVERY) continue;
                    double weight = this.memory.hitRate(candidate, true) * (0.7D + rnd.nextDouble() * 0.6D);
                    if (ComboType.tierOf(candidate) == Tier.STRONG && !s.read.exposed()) weight *= 0.6D;
                    if (weight > bestWeight) {
                        bestWeight = weight;
                        pick = candidate;
                    }
                }
            } else {
                pick = firstAllowed(allowed, DEFAULT_COMBOS);
                if (pick < 0 || rnd.nextFloat() < 0.4F) {
                    int candidate = allowed[rnd.nextInt(allowed.length)];
                    if (candidate != COMBO_RECOVERY) pick = candidate;
                }
            }
        }
        return pick;
    }

    private double skillScore(KiSkill skill, Situation s, AiProfile profile, long now, int maxCooldown) {
        SkillRole role = skill.role;
        KiSkillType type = KiSkillType.fromId(skill.id);
        Tier tier = type != null ? type.getTier() : Tier.MEDIUM;
        boolean directional = role == SkillRole.RANGED_TRAVEL || role == SkillRole.HITSCAN
                || role == SkillRole.PROJECTILE_FAST || role == SkillRole.ZONING || role == SkillRole.GUARD_BREAK;
        if (directional && !s.los) return 0.0D;
        if (directional && s.inFiringLine) return 0.0D;
        if (s.allyCastingHeavy && tier != Tier.WEAK && profile.tier.atLeast(AiTier.SMART)) return 0.0D;
        double burstRange = burstRange(skill);

        if (profile.skillPick == AiProfile.SkillPick.HIGHEST_COOLDOWN) {
            if (role == SkillRole.DEFENSIVE) return 0.2D;
            if (role == SkillRole.AOE_BURST) return s.dist <= burstRange ? 1.1D : 0.0D;
            double score = 0.9D + 0.6D * ((double) skill.cooldownMax / maxCooldown);
            if (s.dist <= s.meleeRange * 0.8D) score -= 0.3D;
            return score;
        }

        double score;
        switch (role) {
            case RANGED_TRAVEL -> {
                if (s.dist > OUT_RANGE) score = 0.85D;
                else if (s.dist > MID_RANGE) score = 1.0D;
                else if (s.dist > s.meleeRange) score = 0.8D;
                else score = 0.35D;
                if (s.read.exposed()) score += 0.5D;
                if (s.approaching() && s.dist > MID_RANGE) score += 0.25D;
                if (s.read.blocking && profile.readsGuard) score -= 0.6D;
                if (!profile.leadAim && s.targetSpeed > 0.25D) score -= 0.3D;
            }
            case HITSCAN -> {
                if (s.dist > OUT_RANGE) score = 0.75D;
                else if (s.dist > s.meleeRange) score = 1.0D;
                else score = 0.6D;
                if (s.read.exposed()) score += 0.6D;
                if (s.read.blocking && profile.readsGuard) score -= 0.5D;
            }
            case PROJECTILE_FAST -> {
                if (s.dist > OUT_RANGE) score = 0.3D;
                else if (s.dist > MID_RANGE) score = 0.6D;
                else score = 0.9D;
                if (s.heatFrac > 0.3F && s.heatFrac < 0.7F) score += 0.15D;
            }
            case ZONING -> {
                score = (s.retreating() || (s.dist > MID_RANGE && !s.approaching())) ? 1.0D : 0.45D;
                if (s.targetAirborne) score += 0.3D;
            }
            case GUARD_BREAK -> {
                if (s.read.blocking && profile.readsGuard) score = 1.35D;
                else if (s.read.exposed()) score = 0.7D;
                else score = 0.25D + (s.dist <= MID_RANGE ? 0.2D : 0.0D);
            }
            case BLIND -> {
                if (s.dist > MID_RANGE || this.memory.blindedRecently(now) || s.read.exposed()) return 0.0D;
                score = 0.85D + (s.selfHp < 0.4F ? 0.3D : 0.0D);
            }
            case AOE_BURST -> {
                if (s.dist > burstRange) return 0.0D;
                score = 1.2D + (s.rivalsNear >= 2 ? 0.3D : 0.0D) + (s.meleeCrowd == 0 && s.dist <= s.meleeRange ? 0.1D : 0.0D);
            }
            case DEFENSIVE -> score = (s.selfHp < 0.3F && s.dist > s.meleeRange) ? 0.6D : 0.15D;
            default -> score = 0.5D;
        }

        if (profile.memory) {
            if (this.memory.consecutiveKiMisses() >= 2 && directional) score -= 0.35D;
            CombatMemory.Outcome last = this.memory.lastOutcome(skill.id, false);
            if (last == CombatMemory.Outcome.HIT) score += 0.15D;
        }
        if (profile.tier.atLeast(AiTier.ELITE)) {
            if (tier == Tier.STRONG) score += s.read.exposed() ? 0.35D : -0.25D;
            else if (tier == Tier.MEDIUM) score -= 0.05D;
        }
        if (profile.tier.atLeast(AiTier.SMART)) score *= 1.0D - 0.35D * s.heatFrac;
        return score;
    }

    private double burstRange(KiSkill skill) {
        return switch (skill.id) {
            case 7 -> Math.min(16.0D, Math.max(8.0D, 5.0D * (Math.max(this.self.getBbWidth(), this.self.getBbHeight()) / 1.8D)));
            case 5 -> 5.0D + Math.max(1.0F, skill.size) * 1.5D;
            case 12 -> 7.0D;
            default -> 6.0D;
        };
    }

    private void execute(Option option, Situation s, AiProfile profile, RandomSource rnd, long now) {
        LivingEntity target = s.target;
        switch (option.kind) {
            case MELEE -> {
                this.self.cancelManeuver();
                this.self.setMeleeAllowed(true);
                this.self.setLocomotionMode(LocomotionMode.RUN);
                this.self.restoreMovementSpeed();
            }
            case COMBO -> {
                this.self.cancelManeuver();
                this.queuedComboWindow = 0;
                this.self.startCombo(option.comboId);
            }
            case CAST -> {
                if (rnd.nextFloat() < profile.castCommitChance) {
                    this.self.cancelManeuver();
                    this.self.startSkill(option.skill);
                } else {
                    this.self.cancelManeuver();
                    this.self.setMeleeAllowed(true);
                    this.self.setLocomotionMode(LocomotionMode.RUN);
                    this.self.restoreMovementSpeed();
                }
            }
            case APPROACH -> {
                this.self.cancelManeuver();
                this.self.applyApproach(option.mode, target);
            }
            case FLANK -> this.self.beginFlank(option.point, 30);
            case STRAFE -> this.self.beginStrafe(12 + rnd.nextInt(10), option.dir);
            case KITE -> {
                this.self.beginKite(25);
                this.memory.noteKite(now);
            }
            case TELEPORT -> {
                this.self.cancelManeuver();
                this.self.performProactiveTeleport(target);
                this.economy.onMobilityUse(now);
                this.memory.noteTeleport(now);
                if (profile.tier.atLeast(AiTier.ELITE) && this.self.isComboReady()) {
                    this.queuedComboWindow = 12;
                    this.decisionCooldown = 1;
                }
            }
            case HOLD -> {
                this.self.cancelManeuver();
                this.self.setMeleeAllowed(true);
                this.self.setLocomotionMode(LocomotionMode.IDLE);
            }
        }
    }
}
