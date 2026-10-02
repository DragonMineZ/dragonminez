package com.dragonminez.common.init.entities.worldboss;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public final class GeteStarEntities {

    public static final String OWNER_TAG = "dmz_gete_core";

    private static final Vector3f DRAIN_DUST = new Vector3f(0.62F, 0.12F, 0.95F);
    private static final Vector3f OVERLOAD_DUST = new Vector3f(1.0F, 0.85F, 0.25F);

    private GeteStarEntities() {}

    public static void setOwner(Entity entity, UUID core) {
        entity.getPersistentData().putUUID(OWNER_TAG, core);
    }

    public static UUID ownerOf(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.hasUUID(OWNER_TAG) ? data.getUUID(OWNER_TAG) : null;
    }

    public static boolean isOwnedBy(Entity entity, UUID core) {
        return core != null && core.equals(ownerOf(entity));
    }

    public static UUID factionOf(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof AllWorldBossesEntity.MetalCoolerCore) return entity.getUUID();
        return ownerOf(entity);
    }

    public static boolean sameFaction(Entity a, Entity b) {
        if (a == null || b == null) return false;
        UUID faction = factionOf(a);
        return faction != null && faction.equals(factionOf(b));
    }

    public static <T extends Entity> List<T> owned(Level level, Class<T> type, AABB area, UUID core) {
        return level.getEntitiesOfClass(type, area, entity -> entity.isAlive() && isOwnedBy(entity, core));
    }

    public static AllWorldBossesEntity.MetalCoolerCore coreOf(Entity entity) {
        UUID id = ownerOf(entity);
        if (id == null || !(entity.level() instanceof ServerLevel level)) return null;
        return level.getEntity(id) instanceof AllWorldBossesEntity.MetalCoolerCore core && core.isAlive() ? core : null;
    }

    private static void flow(ServerLevel level, Vec3 from, Vec3 to, Vector3f color, int points) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.3F);
        for (int i = 0; i < points; i++) {
            Vec3 point = from.lerp(to, level.random.nextDouble());
            level.sendParticles(dust, point.x, point.y, point.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    public static class Bind extends Entity {

        private static final EntityDataAccessor<Integer> VICTIM_ID = SynchedEntityData.defineId(Bind.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Integer> RETRACT_AT = SynchedEntityData.defineId(Bind.class, EntityDataSerializers.INT);

        public static final int EMERGE_TICKS = 6;
        public static final int RETRACT_TICKS = 8;
        public static final String POSE = "skp.gete_bind";
        private static final String ESCAPE_KEY = "key.sneak";
        private static final String GIVE_KEY = "key.dragonminez.ki_charge";
        private static final String TITLE = "worldboss.dragonminez.metal_cooler_core.bind.title";
        private static final String SUBTITLE = "worldboss.dragonminez.metal_cooler_core.bind.subtitle";
        private static final String PROGRESS = "worldboss.dragonminez.metal_cooler_core.bind.progress";
        private static final String ESCAPED = "worldboss.dragonminez.metal_cooler_core.bind.escaped";
        private static final int MISS_TICKS = 16;
        private static final int MAX_BIND_TICKS = 160;
        private static final int ESCAPE_PRESSES = 10;
        private static final int PROMPT_INTERVAL = 20;
        private static final double LIFT = 0.35D;
        private static final float DRAIN_RATIO = 0.05F;
        private static final float HEAL_RATIO = 0.005F;
        private static final float OVERLOAD_RATE = 0.004F;

        private UUID victimUuid;
        private int boundTicks;
        private int presses;
        private boolean shiftWasDown;
        private boolean released;

        public Bind(EntityType<? extends Bind> type, Level level) {
            super(type, level);
            this.noPhysics = true;
        }

        public static Bind create(ServerLevel level, AllWorldBossesEntity.MetalCoolerCore core, Vec3 spot, float yaw, ServerPlayer victim) {
            Bind bind = MainEntities.GETE_CABLE.get().create(level);
            if (bind == null) return null;
            setOwner(bind, core.getUUID());
            bind.moveTo(spot.x, spot.y, spot.z, yaw, 0.0F);
            if (victim != null) {
                bind.victimUuid = victim.getUUID();
                bind.entityData.set(VICTIM_ID, victim.getId());
            }
            return bind;
        }

        @Override
        protected void defineSynchedData() {
            this.entityData.define(VICTIM_ID, -1);
            this.entityData.define(RETRACT_AT, -1);
        }

        public int victimId() {
            return this.entityData.get(VICTIM_ID);
        }

        public int retractAt() {
            return this.entityData.get(RETRACT_AT);
        }

        public boolean isRetracting() {
            return this.retractAt() >= 0;
        }

        public boolean holds(UUID player) {
            return !this.released && this.victimUuid != null && this.victimUuid.equals(player);
        }

        private ServerPlayer victim(ServerLevel level) {
            if (this.victimUuid == null) return null;
            Entity entity = level.getEntity(this.victimId());
            return entity instanceof ServerPlayer player && player.getUUID().equals(this.victimUuid) ? player : null;
        }

        @Override
        public void tick() {
            super.tick();
            if (!(this.level() instanceof ServerLevel level)) return;

            if (this.isRetracting()) {
                if (this.tickCount - this.retractAt() >= RETRACT_TICKS) this.discard();
                return;
            }
            if (this.tickCount == 1) this.emerge(level);

            if (this.victimUuid == null) {
                if (this.tickCount >= MISS_TICKS) this.retract();
                return;
            }

            ServerPlayer victim = this.victim(level);
            AllWorldBossesEntity.MetalCoolerCore core = coreOf(this);
            if (victim == null || core == null || core.isBossAsleep() || ++this.boundTicks > MAX_BIND_TICKS || isDown(victim)
                    || core.getBossAbility() == AllWorldBossesEntity.MetalCoolerCore.ABILITY_OVERLOAD) {
                this.release(false);
                return;
            }

            StrikeAttackHandler.placeHeldVictim(victim, this.getX(), this.getY() + LIFT, this.getZ(), this.getYRot());
            victim.fallDistance = 0.0F;
            if (this.boundTicks == 1 || this.boundTicks % PROMPT_INTERVAL == 0) this.prompt(victim);
            if (this.tickEscape(level, victim)) return;
            this.tickEnergy(level, core, victim);
        }

        private void emerge(ServerLevel level) {
            BlockState floor = level.getBlockState(BlockPos.containing(this.getX(), this.getY() - 0.5D, this.getZ()));
            if (!floor.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), this.getX(), this.getY() + 0.1D, this.getZ(),
                        40, 1.2D, 0.1D, 1.2D, 0.15D);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.3D, this.getZ(), 20, 1.0D, 0.3D, 1.0D, 0.2D);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 2.0F, 0.5F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 2.0F, 0.6F);
        }

        private void prompt(ServerPlayer victim) {
            NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(victim.getUUID(),
                    TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, POSE), victim);
            victim.connection.send(new ClientboundSetTitlesAnimationPacket(0, PROMPT_INTERVAL + 10, 6));
            victim.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable(SUBTITLE, Component.keybind(GIVE_KEY)).withStyle(ChatFormatting.YELLOW)));
            victim.connection.send(new ClientboundSetTitleTextPacket(
                    Component.translatable(TITLE, Component.keybind(ESCAPE_KEY)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        }

        private boolean tickEscape(ServerLevel level, ServerPlayer victim) {
            boolean shift = victim.isShiftKeyDown();
            boolean pressed = shift && !this.shiftWasDown;
            this.shiftWasDown = shift;
            if (!pressed) return false;

            this.presses++;
            victim.displayClientMessage(Component.translatable(PROGRESS, this.progressBar()), true);
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS,
                    1.0F, 0.8F + this.presses * 0.05F);
            if (this.presses < ESCAPE_PRESSES) return false;

            victim.displayClientMessage(Component.translatable(ESCAPED).withStyle(ChatFormatting.GREEN), true);
            this.release(true);
            return true;
        }

        private Component progressBar() {
            String filled = "|".repeat(this.presses);
            String empty = "|".repeat(Math.max(0, ESCAPE_PRESSES - this.presses));
            return Component.literal(filled).withStyle(ChatFormatting.GREEN).append(Component.literal(empty).withStyle(ChatFormatting.DARK_GRAY));
        }

        private void tickEnergy(ServerLevel level, AllWorldBossesEntity.MetalCoolerCore core, ServerPlayer victim) {
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, victim).orElse(null);
            if (data == null) return;
            Vec3 center = victim.position().add(0.0D, victim.getBbHeight() * 0.55D, 0.0D);
            Vec3 torso = core.position().add(0.0D, 7.0D, 0.0D);

            if (data.getStatus().isChargingKi()) {
                core.addOverload(OVERLOAD_RATE);
                if (this.tickCount % 3 == 0) flow(level, center, torso, OVERLOAD_DUST, 6);
                return;
            }
            if (this.tickCount % 20 != 0) return;
            data.getResources().removeEnergy(data.getMaxEnergy() * DRAIN_RATIO, false);
            data.getResources().removeStamina(data.getMaxStamina() * DRAIN_RATIO);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(victim), victim);
            core.heal(core.getMaxHealth() * HEAL_RATIO);
            flow(level, center, torso, DRAIN_DUST, 14);
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), MainSounds.KI_CHARGE_LOOP.get(), SoundSource.HOSTILE, 0.6F, 0.6F);
        }

        private static boolean isDown(ServerPlayer victim) {
            if (victim.isCreative() || victim.isSpectator()) return true;
            return StatsProvider.get(StatsCapability.INSTANCE, victim).map(data -> data.getStatus().isKnockedDown()).orElse(false);
        }

        public void release(boolean escaped) {
            if (this.released) return;
            this.released = true;
            if (this.victimUuid != null && this.level() instanceof ServerLevel level) {
                ServerPlayer victim = this.victim(level);
                if (victim != null) {
                    NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(victim.getUUID(),
                            TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), victim);
                    victim.connection.send(new ClientboundClearTitlesPacket(true));
                    if (escaped) {
                        victim.setDeltaMovement(victim.getDeltaMovement().add(0.0D, 0.5D, 0.0D));
                        victim.hurtMarked = true;
                    }
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0D, this.getZ(), 24, 0.6D, 0.8D, 0.6D, 0.25D);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.6F, 0.7F);
            }
            this.retract();
        }

        private void retract() {
            if (!this.isRetracting()) this.entityData.set(RETRACT_AT, this.tickCount);
        }

        @Override
        public void remove(RemovalReason reason) {
            if (!this.level().isClientSide) this.release(false);
            super.remove(reason);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public boolean isPickable() {
            return false;
        }

        @Override
        public boolean isAttackable() {
            return false;
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }
    }

    public static class Scrap extends Entity {

        private static final EntityDataAccessor<Integer> TIMER = SynchedEntityData.defineId(Scrap.class, EntityDataSerializers.INT);

        public static final int ASSEMBLE_TICKS = 160;
        public static final int COLLAPSE_TICKS = 24;
        public static final double SPHERE_RADIUS = 3.5D;
        public static final double SPHERE_LIFT = 1.6D;
        private static final int SHELL_PARTICLES = 14;
        private static final String STOPPED = "worldboss.dragonminez.metal_cooler_core.reassembly_stopped";

        public Scrap(EntityType<? extends Scrap> type, Level level) {
            super(type, level);
        }

        public static Scrap create(ServerLevel level, AllWorldBossesEntity.MetalCoolerCore core, Vec3 position) {
            Scrap scrap = MainEntities.GETE_SCRAP.get().create(level);
            if (scrap == null) return null;
            setOwner(scrap, core.getUUID());
            scrap.moveTo(position.x, position.y, position.z, level.random.nextFloat() * 360.0F, 0.0F);
            return scrap;
        }

        @Override
        protected void defineSynchedData() {
            this.entityData.define(TIMER, 0);
        }

        public int timer() {
            return this.entityData.get(TIMER);
        }

        public float progress(float partialTick) {
            return Math.min(1.0F, (this.timer() + partialTick) / ASSEMBLE_TICKS);
        }

        public Vec3 sphereCenter() {
            return this.position().add(0.0D, SPHERE_LIFT, 0.0D);
        }

        private double shellRadius(float partialTick) {
            float collapse = (this.timer() + partialTick - (ASSEMBLE_TICKS - COLLAPSE_TICKS)) / COLLAPSE_TICKS;
            return SPHERE_RADIUS * (1.0D - 0.85D * Math.max(0.0F, Math.min(1.0F, collapse)));
        }

        @Override
        public void tick() {
            super.tick();
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, 0.98D, 0.5D));

            if (this.level().isClientSide) {
                this.emitShell();
                return;
            }
            if (!(this.level() instanceof ServerLevel level)) return;

            int timer = this.timer() + 1;
            this.entityData.set(TIMER, timer);

            Player intruder = this.intruder(level);
            if (intruder != null) {
                this.disrupt(level, intruder);
                return;
            }

            if (timer % 10 == 0) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.4F,
                        0.5F + timer / (float) ASSEMBLE_TICKS);
            }
            if (timer < ASSEMBLE_TICKS) return;

            AllWorldBossesEntity.MetalCoolerCore core = coreOf(this);
            if (core != null && !core.isBossAsleep()) core.spawnGuardian(this.position());
            this.discard();
        }

        private Player intruder(ServerLevel level) {
            Vec3 center = this.sphereCenter();
            double reach = SPHERE_RADIUS + 0.5D;
            for (Player player : level.getEntitiesOfClass(Player.class, new AABB(center, center).inflate(reach + 2.0D))) {
                if (!player.isAlive() || player.isSpectator()) continue;
                Vec3 body = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
                if (body.distanceToSqr(center) <= reach * reach) return player;
            }
            return null;
        }

        private void disrupt(ServerLevel level, Player player) {
            Vec3 center = this.sphereCenter();
            ItemStack loot = new ItemStack(MainItems.GETE_SCRAP.get(), 1 + level.random.nextInt(2));
            if (!player.getInventory().add(loot)) this.spawnAtLocation(loot);
            level.sendParticles(MainParticles.STARDUST.get(), center.x, center.y, center.z, 140, SPHERE_RADIUS * 0.5D, SPHERE_RADIUS * 0.5D,
                    SPHERE_RADIUS * 0.5D, 0.25D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 40, 1.5D, 1.5D, 1.5D, 0.3D);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0F, 0.7F);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 1.2F);
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.displayClientMessage(Component.translatable(STOPPED), true);
            this.discard();
        }

        private void emitShell() {
            Vec3 center = this.sphereCenter();
            double radius = this.shellRadius(0.0F);
            float progress = this.progress(0.0F);
            int count = SHELL_PARTICLES + (int) (progress * 8.0F);
            for (int i = 0; i < count; i++) {
                double y = this.random.nextDouble() * 2.0D - 1.0D;
                double theta = this.random.nextDouble() * Math.PI * 2.0D;
                double ring = Math.sqrt(1.0D - y * y);
                Vec3 dir = new Vec3(Math.cos(theta) * ring, y, Math.sin(theta) * ring);
                Vec3 pos = center.add(dir.scale(radius));
                Vec3 swirl = new Vec3(-dir.z, 0.0D, dir.x).scale(0.03D);
                Vec3 velocity = swirl.add(dir.scale(-0.012D - progress * 0.03D)).add(0.0D, 0.02D, 0.0D);
                this.level().addParticle(MainParticles.STARDUST.get(), pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
            }
            if (this.random.nextFloat() < 0.5F + progress * 0.5F) {
                double y = this.random.nextDouble() * 2.0D - 1.0D;
                double theta = this.random.nextDouble() * Math.PI * 2.0D;
                double ring = Math.sqrt(1.0D - y * y);
                Vec3 dir = new Vec3(Math.cos(theta) * ring, y, Math.sin(theta) * ring);
                Vec3 pos = center.add(dir.scale(radius));
                Vec3 velocity = dir.scale(-radius * 0.08D);
                this.level().addParticle(MainParticles.STARDUST.get(), pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
                this.level().addParticle(new DustParticleOptions(DRAIN_DUST, 1.1F), pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
            }
        }

        @Override
        public boolean shouldRenderAtSqrDistance(double distance) {
            return distance < 96.0D * 96.0D;
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
            this.entityData.set(TIMER, tag.getInt("Timer"));
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
            tag.putInt("Timer", this.timer());
        }
    }
}
