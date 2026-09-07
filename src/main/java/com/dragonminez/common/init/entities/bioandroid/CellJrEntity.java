package com.dragonminez.common.init.entities.bioandroid;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.racial.impl.BioAndroidEvolution;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CellJrEntity extends DBSagasEntity {
	private static final Set<UUID> GLOBAL_LIVE_IDS = ConcurrentHashMap.newKeySet();

	private UUID ownerUUID;

	public CellJrEntity(EntityType<? extends Monster> entityType, Level level) {
		super(entityType, level);
		this.setCanFly(true);
		this.setAuraColor(0xFFFC42);
		this.setTextureVariant(0);
		this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
	}

	public void applyOwnerScaling(ServerPlayer owner, StatsData ownerData, double ratio) {
		double health = Math.max(1.0, owner.getMaxHealth() * ratio);
		double attack = Math.max(1.0, ownerData.getMaxMeleeDamage() * ratio);

		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attack);
		this.setHealth(this.getMaxHealth());
	}

	public static int globalLiveCount() {
		return GLOBAL_LIVE_IDS.size();
	}

	public void setOwnerUUID(UUID ownerUUID) {
		this.ownerUUID = ownerUUID;
	}

	public UUID getOwnerUUID() {
		return ownerUUID;
	}

	public LivingEntity resolveOwner() {
		if (ownerUUID == null || !(this.level() instanceof ServerLevel serverLevel)) return null;
		var player = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
		return player;
	}

	public boolean isValidTarget(LivingEntity candidate) {
		LivingEntity owner = resolveOwner();
		if (owner == null) return false;
		if (candidate == owner) return false;
		if (candidate instanceof CellJrEntity otherJr) return !owner.equals(otherJr.resolveOwner());

		if (candidate instanceof ServerPlayer candidatePlayer && owner instanceof ServerPlayer ownerPlayer) {
			if (PartyManager.areInSameParty(ownerPlayer, candidatePlayer)) {
				boolean candidateIsBioAndroid = StatsProvider.get(StatsCapability.INSTANCE, candidatePlayer).resolve()
						.map(d -> "bioandroid".equals(d.getCharacter().getRaceName())).orElse(false);
				if (candidateIsBioAndroid) return false;
				return PartyManager.isPartyPvpEnabled(ownerPlayer);
			}
		}
		return true;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4D, true));
		this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.2D, 8.0D, 4.0D));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isValidTarget));
	}

	@Override
	public String getGeckolibModelName() {
		return "saga_cell_jr";
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide) return;

		LivingEntity owner = resolveOwner();
		if (owner == null) {
			this.discard();
			return;
		}

		double leashRange = ConfigManager.getServerConfig().getRacialSkills().getBioandroid().getCellJrLeashRange();
		if (this.distanceToSqr(owner) > leashRange * leashRange) {
			this.discard();
		}
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
	}

	public void markSpawned() {
		if (!this.level().isClientSide) GLOBAL_LIVE_IDS.add(this.getUUID());
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);

		if (!this.level().isClientSide && reason.shouldDestroy()) {
			GLOBAL_LIVE_IDS.remove(this.getUUID());
			BioAndroidEvolution.onCellJrRemoved(this);
		}
	}

	private static class FollowOwnerGoal extends Goal {
		private final CellJrEntity jr;
		private final double speed;
		private final double startDistance;
		private final double stopDistance;
		private LivingEntity owner;
		private int repathDelay;

		FollowOwnerGoal(CellJrEntity jr, double speed, double startDistance, double stopDistance) {
			this.jr = jr;
			this.speed = speed;
			this.startDistance = startDistance;
			this.stopDistance = stopDistance;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (jr.getTarget() != null) return false;
			LivingEntity resolved = jr.resolveOwner();
			if (resolved == null || jr.distanceToSqr(resolved) < startDistance * startDistance) return false;
			this.owner = resolved;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return owner != null && jr.getTarget() == null && jr.distanceToSqr(owner) > stopDistance * stopDistance;
		}

		@Override
		public void start() {
			repathDelay = 0;
		}

		@Override
		public void stop() {
			owner = null;
			jr.getNavigation().stop();
		}

		@Override
		public void tick() {
			jr.getLookControl().setLookAt(owner, 10.0F, jr.getMaxHeadXRot());
			if (--repathDelay > 0) return;
			repathDelay = 10;
			jr.getNavigation().moveTo(owner, speed);
		}
	}
}
