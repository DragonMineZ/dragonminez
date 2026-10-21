package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.entities.demon.DemonVillagersEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Status;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class DemonVillagerRenderer<T extends DemonVillagersEntity.Resident> extends EntityRenderer<T> {

	private static final Map<Integer, Puppet> PUPPETS = new HashMap<>();
	private static final int SWEEP_INTERVAL = 200;
	private static ClientLevel puppetLevel;
	private static int renderCounter;
	private static boolean failureLogged;

	public DemonVillagerRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.4F;
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		Puppet puppet = puppetFor(entity);
		if (puppet != null && !puppet.broken) {
			puppet.sync(entity);
			DMZPlayerRenderer renderer = DMZRendererCache.getTPRenderer(puppet);
			if (renderer != null) {
				PoseStack local = new PoseStack();
				local.last().pose().set(poseStack.last().pose());
				local.last().normal().set(poseStack.last().normal());
				try {
					renderer.render(puppet, entityYaw, partialTick, local, buffer, packedLight);
				} catch (RuntimeException e) {
					puppet.broken = true;
					if (!failureLogged) {
						failureLogged = true;
						LogUtil.error(Env.CLIENT, "Demon villager could not be drawn with the player renderer: {}", e.toString());
					}
				}
			}
		}
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	private static Puppet puppetFor(DemonVillagersEntity.Resident entity) {
		if (!(entity.level() instanceof ClientLevel level)) return null;

		if (level != puppetLevel) {
			clearPuppets();
			puppetLevel = level;
		}
		sweep(level);

		Puppet puppet = PUPPETS.get(entity.getId());
		if (puppet == null || puppet.owner != entity) {
			if (puppet != null) DMZRendererCache.evict(puppet.getUUID());
			puppet = new Puppet(level, entity);
			PUPPETS.put(entity.getId(), puppet);
		}
		return puppet;
	}

	private static void sweep(ClientLevel level) {
		if (++renderCounter < SWEEP_INTERVAL) return;
		renderCounter = 0;

		Iterator<Map.Entry<Integer, Puppet>> iterator = PUPPETS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Puppet> entry = iterator.next();
			Entity owner = level.getEntity(entry.getKey());
			if (owner == null || owner.isRemoved() || owner != entry.getValue().owner) {
				DMZRendererCache.evict(entry.getValue().getUUID());
				iterator.remove();
			}
		}
	}

	private static void clearPuppets() {
		for (Puppet puppet : PUPPETS.values()) DMZRendererCache.evict(puppet.getUUID());
		PUPPETS.clear();
	}

	public static class Puppet extends RemotePlayer implements GeoAnimatable, DMZRendererCache.NpcPuppet {
		private static final int BODY_TYPE = 1;
		private static final String RACE = "human";
		private static final String HEAD_BONES = "ears1+hair";
		private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("base.idle");
		private static final RawAnimation WALK = RawAnimation.begin().thenLoop("base.walk");

		private final DemonVillagersEntity.Resident owner;
		private final AnimatableInstanceCache animationCache = new SingletonAnimatableInstanceCache(this);
		private DemonVillagersEntity.Look appliedLook;
		private boolean broken;
		private int syncedTick = Integer.MIN_VALUE;

		private Puppet(ClientLevel level, DemonVillagersEntity.Resident owner) {
			super(level, new GameProfile(puppetId(owner.getUUID()), "DemonVillager"));
			this.owner = owner;
		}

		private static UUID puppetId(UUID owner) {
			UUID id = new UUID(owner.getMostSignificantBits() ^ 0x0D3A0FA11E5C0DE0L, owner.getLeastSignificantBits());
			return (id.hashCode() & 1) == 1 ? new UUID(id.getMostSignificantBits(), id.getLeastSignificantBits() ^ 1L) : id;
		}

		@Override
		public ItemStack getScouter() {
			return ItemStack.EMPTY;
		}

		@Override
		public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
			controllers.add(new AnimationController<>(this, "base_controller", 5, this::walkPredicate));
		}

		private PlayState walkPredicate(AnimationState<Puppet> state) {
			return state.setAndContinue(state.isMoving() ? WALK : IDLE);
		}

		@Override
		public AnimatableInstanceCache getAnimatableInstanceCache() {
			return this.animationCache;
		}

		@Override
		public double getTick(Object object) {
			return this.tickCount;
		}

		private void sync(DemonVillagersEntity.Resident owner) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, this).orElse(null);
			if (data == null) return;

			DemonVillagersEntity.Look look = owner.getLook();
			if (look != this.appliedLook) this.applyLook(data, look);

			this.setPos(owner.getX(), owner.getY(), owner.getZ());
			this.xo = owner.xo;
			this.yo = owner.yo;
			this.zo = owner.zo;
			this.xOld = owner.xOld;
			this.yOld = owner.yOld;
			this.zOld = owner.zOld;
			this.setYRot(owner.getYRot());
			this.yRotO = owner.yRotO;
			this.setXRot(owner.getXRot());
			this.xRotO = owner.xRotO;
			this.yBodyRot = owner.yBodyRot;
			this.yBodyRotO = owner.yBodyRotO;
			this.yHeadRot = owner.yHeadRot;
			this.yHeadRotO = owner.yHeadRotO;
			this.setOnGround(owner.onGround());
			this.setDeltaMovement(owner.getDeltaMovement());
			this.hurtTime = owner.hurtTime;
			this.hurtDuration = owner.hurtDuration;
			this.deathTime = owner.deathTime;
			this.setInvisible(owner.isInvisible());

			if (this.syncedTick == owner.tickCount) return;
			this.syncedTick = owner.tickCount;
			this.tickCount = owner.tickCount;
			this.walkAnimation.update(owner.walkAnimation.speed(), 1.0F);
		}

		private void applyLook(StatsData data, DemonVillagersEntity.Look look) {
			Character character = data.getCharacter();
			character.setRace(RACE);
			character.setGender(look.female() ? Character.GENDER_FEMALE : Character.GENDER_MALE);

			RaceCharacterConfig config = ConfigManager.getRaceCharacter(RACE);
			if (config != null) {
				character.setEyesType(config.getDefaultEyesType());
				character.setNoseType(config.getDefaultNoseType());
				character.setMouthType(config.getDefaultMouthType());
				character.setTattooType(config.getDefaultTattooType());
				if (config.getDefaultBodyColor2() != null) character.setBodyColor2(config.getDefaultBodyColor2());
				if (config.getDefaultBodyColor3() != null) character.setBodyColor3(config.getDefaultBodyColor3());
				if (config.getDefaultEye1Color() != null) character.setEye1Color(config.getDefaultEye1Color());
				if (config.getDefaultEye2Color() != null) character.setEye2Color(config.getDefaultEye2Color());
			}

			character.setBodyType(BODY_TYPE);
			character.setBodyColor(look.skinHex());
			character.setHairId(look.hairId());
			character.setActiveHeadBone(HEAD_BONES);
			character.setRenderHairBase(true);
			character.setHairColor(look.hairHex());
			character.setHasSaiyanTail(false);
			character.clearActiveForm();

			Status status = data.getStatus();
			status.setHasCreatedCharacter(true);

			this.getInventory().armor.set(EquipmentSlot.CHEST.getIndex(), look.armorPiece(ArmorItem.Type.CHESTPLATE));
			this.getInventory().armor.set(EquipmentSlot.LEGS.getIndex(), look.armorPiece(ArmorItem.Type.LEGGINGS));
			this.getInventory().armor.set(EquipmentSlot.FEET.getIndex(), look.armorPiece(ArmorItem.Type.BOOTS));

			this.appliedLook = look;
			this.refreshDimensions();
		}
	}
}
