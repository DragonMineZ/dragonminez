package com.dragonminez.common.init.entities;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.network.S2C.OpenQuestNPCDialogueS2C;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

public class MastersEntity extends PathfinderMob implements GeoEntity {

	private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);
	@Getter
	protected String masterName = null;

	protected MastersEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
		this.setPersistenceRequired();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 100.0D)
				.add(Attributes.MOVEMENT_SPEED, 2.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
	}


	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return !(entity instanceof Player);
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}


	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity p_20971_) {
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC) || source.is(DamageTypes.GENERIC_KILL)) {
			return super.hurt(source, amount);
		}

		return false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "controller", 0, event -> {
			return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return geoCache;
	}

	@Override
	public boolean isPersistenceRequired() {
		return true;
	}

	@Override
	public void checkDespawn() {
	}

	@Override
	protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
		if (pHand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

		if (!this.level().isClientSide && pPlayer instanceof ServerPlayer serverPlayer && masterName != null) {
			StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) {
					serverPlayer.displayClientMessage(
							Component.translatable("gui.dragonminez.lines.generic.createcharacter"), true);
					return;
				}
				Component blocker = NpcDispositionService.getDialogueBlocker(serverPlayer, this);
				if (blocker != null) {
					serverPlayer.displayClientMessage(blocker, true);
					return;
				}

				QuestService.NPCQuestOptions options = QuestService.collectNpcQuestOptions(masterName, data);
				NetworkHandler.sendToPlayer(
						new OpenQuestNPCDialogueS2C(masterName, options.offerableQuestIds(),
								options.turnInQuestIds(), options.inProgressQuestIds(), true, getId()),
						serverPlayer
				);
			});
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.SUCCESS;
	}
}
