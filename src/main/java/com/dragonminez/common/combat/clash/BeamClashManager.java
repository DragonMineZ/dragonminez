package com.dragonminez.common.combat.clash;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.BeamClashStateS2C;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side coordinator for beam clashes. Each server-level tick it detects new
 * head-on beam collisions, advances active clashes (tug-of-war + QTE meters), resolves
 * winners, and keeps both participants invulnerable for the duration.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BeamClashManager {

    /** Beam directions must oppose at least this much (dot < value) to count as head-on. */
    private static final double OPPOSITION_DOT = -0.3;
    /** Clash triggers when the two beam segments come within (combined size × factor + pad). */
    private static final double CLASH_RADIUS_FACTOR = 1.0;
    private static final double CLASH_RADIUS_PAD = 1.5;
    /** A minor attack is shattered when its center is within (combined size × factor + pad) of a major beam. */
    private static final double MINOR_BREAK_FACTOR = 0.7;
    private static final double MINOR_BREAK_PAD = 0.6;

    private static final List<BeamClash> ACTIVE_CLASHES = new ArrayList<>();
    private static final Set<UUID> CLASHING_OWNERS = new HashSet<>();

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;

        advanceActiveClashes();

        List<AbstractKiProjectile> majors = new ArrayList<>();
        List<AbstractKiProjectile> minors = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof AbstractKiProjectile ki) || ki.isRemoved()) continue;
            if (!(ki.getOwner() instanceof LivingEntity)) continue;
            AbstractKiProjectile.ClashRole role = ki.getClashRole();
            if (role == AbstractKiProjectile.ClashRole.MAJOR && ki.isClashableBeam() && !ki.isClashLocked()) {
                majors.add(ki);
            } else if (role == AbstractKiProjectile.ClashRole.MINOR) {
                minors.add(ki);
            }
        }

        detectNewClashes(majors);
        breakMinorAttacks(level, majors, minors);
        rebuildClashingOwners();
        syncParticipants();
    }

    private static void advanceActiveClashes() {
        ACTIVE_CLASHES.removeIf(clash -> {
            BeamClash.Result result = clash.tick();
            switch (result) {
                case A_WINS, B_WINS -> {
                    notifyEnded(clash);
                    clash.resolve(result);
                    return true;
                }
                case DISSOLVED -> {
                    notifyEnded(clash);
                    clash.dissolve();
                    return true;
                }
                default -> {
                    return false;
                }
            }
        });
    }

    private static void detectNewClashes(List<AbstractKiProjectile> majors) {
        for (int i = 0; i < majors.size(); i++) {
            AbstractKiProjectile beamA = majors.get(i);
            if (beamA.isClashLocked()) continue;
            if (!(beamA.getOwner() instanceof LivingEntity ownerA)) continue;
            for (int j = i + 1; j < majors.size(); j++) {
                AbstractKiProjectile beamB = majors.get(j);
                if (beamB.isClashLocked()) continue;
                if (!(beamB.getOwner() instanceof LivingEntity ownerB)) continue;
                if (ownerA == ownerB) continue;

                if (beamsClash(beamA, beamB)) {
                    BeamClash clash = new BeamClash(
                            new ClashParticipant(beamA, ownerA),
                            new ClashParticipant(beamB, ownerB));
                    // Lock immediately so neither beam is re-paired this tick.
                    beamA.setClashLock(beamA.getClashBeamLength(), ownerB.getUUID());
                    beamB.setClashLock(beamB.getClashBeamLength(), ownerA.getUUID());
                    ACTIVE_CLASHES.add(clash);
                    break; // beamA is now taken
                }
            }
        }
    }

    private static boolean beamsClash(AbstractKiProjectile beamA, AbstractKiProjectile beamB) {
        Vec3 dirA = Vec3.directionFromRotation(beamA.getClashPitch(), beamA.getClashYaw());
        Vec3 dirB = Vec3.directionFromRotation(beamB.getClashPitch(), beamB.getClashYaw());
        if (dirA.dot(dirB) > OPPOSITION_DOT) return false;

        Vec3 a0 = beamA.position();
        Vec3 a1 = a0.add(dirA.scale(Math.max(0.1F, beamA.getClashBeamLength())));
        Vec3 b0 = beamB.position();
        Vec3 b1 = b0.add(dirB.scale(Math.max(0.1F, beamB.getClashBeamLength())));

        double threshold = (beamA.getSize() + beamB.getSize()) * CLASH_RADIUS_FACTOR + CLASH_RADIUS_PAD;
        return segmentDistanceSq(a0, a1, b0, b1) <= threshold * threshold;
    }

    private static void breakMinorAttacks(ServerLevel level, List<AbstractKiProjectile> majors,
                                          List<AbstractKiProjectile> minors) {
        if (minors.isEmpty()) return;
        for (AbstractKiProjectile major : majors) {
            if (!(major.getOwner() instanceof LivingEntity majorOwner)) continue;
            Vec3 dir = Vec3.directionFromRotation(major.getClashPitch(), major.getClashYaw());
            Vec3 a0 = major.position();
            Vec3 a1 = a0.add(dir.scale(Math.max(0.1F, major.getClashBeamLength())));

            for (AbstractKiProjectile minor : minors) {
                if (minor.isRemoved()) continue;
                if (minor.getOwner() == majorOwner) continue; // don't shatter your own blasts
                double threshold = (major.getSize() + minor.getSize()) * MINOR_BREAK_FACTOR + MINOR_BREAK_PAD;
                if (pointSegmentDistanceSq(minor.position(), a0, a1) <= threshold * threshold) {
                    shatterMinor(level, minor);
                }
            }
        }
    }

    private static void shatterMinor(ServerLevel level, AbstractKiProjectile minor) {
        Vec3 p = minor.position();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                p.x, p.y, p.z, 6, 0.2, 0.2, 0.2, 0.02);
        level.playSound(null, p.x, p.y, p.z, MainSounds.KI_EXPLOSION_IMPACT.get(),
                SoundSource.PLAYERS, 0.6F, 1.3F);
        minor.discard();
    }

    private static double segmentDistanceSq(Vec3 p1, Vec3 q1, Vec3 p2, Vec3 q2) {
        Vec3 d1 = q1.subtract(p1);
        Vec3 d2 = q2.subtract(p2);
        Vec3 r = p1.subtract(p2);
        double a = d1.dot(d1);
        double e = d2.dot(d2);
        double f = d2.dot(r);
        final double EPS = 1.0e-9;

        double s, t;
        if (a <= EPS && e <= EPS) {
            return r.dot(r);
        }
        if (a <= EPS) {
            s = 0.0;
            t = clamp01(f / e);
        } else {
            double c = d1.dot(r);
            if (e <= EPS) {
                t = 0.0;
                s = clamp01(-c / a);
            } else {
                double b = d1.dot(d2);
                double denom = a * e - b * b;
                s = denom > EPS ? clamp01((b * f - c * e) / denom) : 0.0;
                t = (b * s + f) / e;
                if (t < 0.0) {
                    t = 0.0;
                    s = clamp01(-c / a);
                } else if (t > 1.0) {
                    t = 1.0;
                    s = clamp01((b - c) / a);
                }
            }
        }
        Vec3 c1 = p1.add(d1.scale(s));
        Vec3 c2 = p2.add(d2.scale(t));
        Vec3 diff = c1.subtract(c2);
        return diff.dot(diff);
    }

    private static double pointSegmentDistanceSq(Vec3 p, Vec3 s0, Vec3 s1) {
        Vec3 d = s1.subtract(s0);
        double len2 = d.dot(d);
        if (len2 <= 1.0e-9) return p.subtract(s0).lengthSqr();
        double t = clamp01(p.subtract(s0).dot(d) / len2);
        Vec3 proj = s0.add(d.scale(t));
        return p.subtract(proj).lengthSqr();
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }

    private static void rebuildClashingOwners() {
        CLASHING_OWNERS.clear();
        for (BeamClash clash : ACTIVE_CLASHES) {
            CLASHING_OWNERS.add(clash.a().owner().getUUID());
            CLASHING_OWNERS.add(clash.b().owner().getUUID());
        }
    }

    private static void syncParticipants() {
        for (BeamClash clash : ACTIVE_CLASHES) {
            sendState(clash, clash.a());
            sendState(clash, clash.b());
        }
    }

    private static void sendState(BeamClash clash, ClashParticipant participant) {
        if (!(participant.owner() instanceof ServerPlayer player)) return;
        float advantage = clash.advantageFor(player);
        ClashParticipant opponent = participant == clash.a() ? clash.b() : clash.a();
        NetworkHandler.sendToPlayer(new BeamClashStateS2C(
                true,
                participant.meterPhase(),
                BeamClash.SWEET_LOW,
                BeamClash.SWEET_HIGH,
                advantage,
                participant.beam().getColorBorder(),
                opponent.owner().getId()
        ), player);
    }

    private static void notifyEnded(BeamClash clash) {
        notifyEnded(clash.a());
        notifyEnded(clash.b());
    }

    private static void notifyEnded(ClashParticipant participant) {
        if (participant.owner() instanceof ServerPlayer player) {
            NetworkHandler.sendToPlayer(BeamClashStateS2C.inactive(), player);
        }
    }

    /** Returns true while the entity is locked in an active beam clash (used for invulnerability). */
    public static boolean isClashing(UUID ownerId) {
        return CLASHING_OWNERS.contains(ownerId);
    }

    @SuppressWarnings("unchecked")
    private static final RegistryObject<SoundEvent>[] PUNCH_SOUNDS = new RegistryObject[]{
            MainSounds.GOLPE1, MainSounds.GOLPE2, MainSounds.GOLPE3,
            MainSounds.GOLPE4, MainSounds.GOLPE5, MainSounds.GOLPE6
    };

    /** Routes a player's clash key press to their active clash, scoring the QTE meter. */
    public static void handlePlayerPress(ServerPlayer player) {
        for (BeamClash clash : ACTIVE_CLASHES) {
            ClashParticipant participant = clash.participantFor(player.getUUID());
            if (participant != null) {
                participant.registerPlayerPress();
                playPunchSound(player);
                return;
            }
        }
    }

    private static void playPunchSound(ServerPlayer player) {
        SoundEvent sound = PUNCH_SOUNDS[player.getRandom().nextInt(PUNCH_SOUNDS.length)].get();
        float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, 1.0F, pitch);
    }

    /** Cinematic invulnerability: clashing fighters ignore all other incoming damage. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClashingHurt(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (isClashing(victim.getUUID())) {
            event.setCanceled(true);
        }
    }
}
