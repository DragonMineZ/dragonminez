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

    /** Beams must face within this dot-product to count as head-on (dot < -0.5 ≈ >120° apart). */
    private static final double OPPOSITION_DOT = -0.5;
    /** Extra slack (blocks) added to the combined beam radii when testing axis alignment. */
    private static final double ALIGN_TOLERANCE = 1.5;
    /** Trigger the clash slightly before the tips perfectly meet so it feels responsive. */
    private static final double OVERLAP_SLACK = 1.5;

    private static final List<BeamClash> ACTIVE_CLASHES = new ArrayList<>();
    private static final Set<UUID> CLASHING_OWNERS = new HashSet<>();

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;

        advanceActiveClashes();
        detectNewClashes(level);
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

    private static void detectNewClashes(ServerLevel level) {
        List<AbstractKiProjectile> candidates = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AbstractKiProjectile beam
                    && beam.isClashableBeam()
                    && !beam.isClashLocked()
                    && beam.getOwner() instanceof LivingEntity) {
                candidates.add(beam);
            }
        }

        for (int i = 0; i < candidates.size(); i++) {
            AbstractKiProjectile beamA = candidates.get(i);
            if (beamA.isClashLocked()) continue;
            for (int j = i + 1; j < candidates.size(); j++) {
                AbstractKiProjectile beamB = candidates.get(j);
                if (beamB.isClashLocked()) continue;
                if (!(beamA.getOwner() instanceof LivingEntity ownerA)) break;
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
        Vec3 originA = beamA.position();
        Vec3 originB = beamB.position();
        Vec3 dirA = Vec3.directionFromRotation(beamA.getClashPitch(), beamA.getClashYaw());
        Vec3 dirB = Vec3.directionFromRotation(beamB.getClashPitch(), beamB.getClashYaw());

        // Must be roughly facing each other.
        if (dirA.dot(dirB) > OPPOSITION_DOT) return false;

        Vec3 toB = originB.subtract(originA);
        double gap = toB.length();
        if (gap < 1.0e-3) return false;

        // B must lie ahead of A along A's firing axis...
        double projB = toB.dot(dirA);
        if (projB <= 0) return false;

        // ...and close to that axis (the two beams must be roughly collinear).
        double perp = toB.subtract(dirA.scale(projB)).length();
        double alignRadius = (beamA.getSize() + beamB.getSize()) + ALIGN_TOLERANCE;
        if (perp > alignRadius) return false;

        // The two growing beams must have (nearly) spanned the gap between them.
        double reach = beamA.getClashBeamLength() + beamB.getClashBeamLength();
        return reach + OVERLAP_SLACK >= gap;
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
