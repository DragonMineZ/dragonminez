package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.init.entities.renderer.sagas.layer.SagaSupervillainLayer;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.effects.AuraBorderRenderer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.SagaSaiyanInvadersEntity;
import com.dragonminez.common.init.entities.sagas.helper.DBSagasAnimationHandler;
import com.dragonminez.common.init.entities.sagas.helper.DBSagasAnimations;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class SaiyanInvaderRenderer<T extends DBSagasEntity & SagaSaiyanInvadersEntity.Invader> extends EntityRenderer<T> {

	private static final Map<Integer, Puppet> PUPPETS = new HashMap<>();
	private static final int SWEEP_INTERVAL = 200;
	private static ClientLevel puppetLevel;
	private static int renderCounter;
	private static boolean failureLogged;

	private final DBSagasRenderer<T> fallback;

	public SaiyanInvaderRenderer(EntityRendererProvider.Context context, float shadowRadius) {
		super(context);
		this.shadowRadius = shadowRadius;
		this.fallback = new DBSagasRenderer<>(context);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return this.fallback.getTextureLocation(entity);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		Puppet puppet = puppetFor(entity);
		if (puppet != null && !puppet.broken) puppet.sync(entity);
		DMZPlayerRenderer renderer = puppet != null && !puppet.broken ? DMZRendererCache.getTPRenderer(puppet) : null;
		if (renderer == null) {
			this.fallback.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
			return;
		}

		PoseStack local = new PoseStack();
		local.last().pose().set(poseStack.last().pose());
		local.last().normal().set(poseStack.last().normal());

		MultiBufferSource source = buffer;
		MultiBufferSource border = null;
		float[] tint = SagaSupervillainLayer.tintColor(entity);
		if (tint != null) {
			source = new TintedBufferSource(buffer, tint);
			border = AuraBorderRenderer.begin(entity, source, SagaSupervillainLayer.borderColor(entity),
					SagaSupervillainLayer.borderInnerColor(entity), partialTick);
			source = border;
		}

		try {
			renderer.render(puppet, entityYaw, partialTick, local, source, packedLight);
		} catch (RuntimeException e) {
			puppet.broken = true;
			if (!failureLogged) {
				failureLogged = true;
				LogUtil.error(Env.CLIENT, "Saiyan invader could not be drawn with the player renderer, using the saga model instead: {}", e.toString());
			}
		} finally {
			if (border != null) AuraBorderRenderer.end(border);
		}

		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	private static Puppet puppetFor(DBSagasEntity entity) {
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

	private static final class TintedBufferSource implements MultiBufferSource {
		private final MultiBufferSource delegate;
		private final float red;
		private final float green;
		private final float blue;

		private TintedBufferSource(MultiBufferSource delegate, float[] tint) {
			this.delegate = delegate;
			float strength = tint[3];
			this.red = 1.0F - strength + strength * tint[0];
			this.green = 1.0F - strength + strength * tint[1];
			this.blue = 1.0F - strength + strength * tint[2];
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			return new TintedConsumer(this.delegate.getBuffer(renderType), this.red, this.green, this.blue);
		}
	}

	private record TintedConsumer(VertexConsumer delegate, float red, float green, float blue) implements VertexConsumer {
		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			this.delegate.vertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer color(int r, int g, int b, int a) {
			this.delegate.color(Math.round(r * this.red), Math.round(g * this.green), Math.round(b * this.blue), a);
			return this;
		}

		@Override
		public VertexConsumer uv(float u, float v) {
			this.delegate.uv(u, v);
			return this;
		}

		@Override
		public VertexConsumer overlayCoords(int u, int v) {
			this.delegate.overlayCoords(u, v);
			return this;
		}

		@Override
		public VertexConsumer uv2(int u, int v) {
			this.delegate.uv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			this.delegate.normal(x, y, z);
			return this;
		}

		@Override
		public void endVertex() {
			this.delegate.endVertex();
		}

		@Override
		public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v,
						   int overlay, int light, float normalX, float normalY, float normalZ) {
			this.delegate.vertex(x, y, z, r * this.red, g * this.green, b * this.blue, a, u, v, overlay, light, normalX, normalY, normalZ);
		}

		@Override
		public void defaultColor(int r, int g, int b, int a) {
			this.delegate.defaultColor(Math.round(r * this.red), Math.round(g * this.green), Math.round(b * this.blue), a);
		}

		@Override
		public void unsetDefaultColor() {
			this.delegate.unsetDefaultColor();
		}
	}

	public static class Puppet extends RemotePlayer implements GeoAnimatable {
		private static final int SAGA_BODY_TYPE = 1;
		private static final String SUPERVILLAIN_AURA = "#9A1CFF";
		private static final String DEFAULT_AURA = "#FFFFFF";

		private final DBSagasEntity owner;
		private final AnimatableInstanceCache animationCache = new SingletonAnimatableInstanceCache(this);
		private ItemStack scouter = ItemStack.EMPTY;
		private SagaSaiyanInvadersEntity.Look appliedLook;
		private boolean appliedOozaru;
		private boolean appliedVillain;
		private boolean broken;
		private int syncedTick = Integer.MIN_VALUE;
		private int lastHurtTime;

		private Puppet(ClientLevel level, DBSagasEntity owner) {
			super(level, new GameProfile(puppetId(owner.getUUID()), "SaiyanInvader"));
			this.owner = owner;
		}

		private static UUID puppetId(UUID owner) {
			UUID id = new UUID(owner.getMostSignificantBits() ^ 0x5A17E5D0C0FFEE00L, owner.getLeastSignificantBits());
			return (id.hashCode() & 1) == 1 ? new UUID(id.getMostSignificantBits(), id.getLeastSignificantBits() ^ 1L) : id;
		}

		public ItemStack getScouter() {
			return this.scouter;
		}

		@Override
		public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
			controllers.add(new AnimationController<>(this, "base_controller", 5,
					state -> DBSagasAnimationHandler.walkPredicate(state, this.owner)));
			controllers.add(new AnimationController<>(this, "skill_controller", 5, this::skillPredicate));
			controllers.add(new AnimationController<>(this, "evasion_controller", 5,
					state -> DBSagasAnimationHandler.evasionPredicate(state, this.owner)));
			controllers.add(new AnimationController<>(this, "attack_controller", 0,
					state -> DBSagasAnimationHandler.attackPredicate(state, this.owner)));
			controllers.add(new AnimationController<>(this, DBSagasEntity.HURT_CONTROLLER, 0,
					state -> DBSagasAnimationHandler.hurtPredicate(state, this.owner))
					.triggerableAnim(DBSagasEntity.HURT_ANIM_LEFT, DBSagasAnimations.ANIM_HURT_LEFT)
					.triggerableAnim(DBSagasEntity.HURT_ANIM_RIGHT, DBSagasAnimations.ANIM_HURT_RIGHT));
			controllers.add(new AnimationController<>(this, "tail_controller", 5, DBSagasAnimationHandler::tailPredicate));
		}

		private PlayState skillPredicate(AnimationState<Puppet> state) {
			if (this.owner instanceof SagaSaiyanInvadersEntity.Soldier soldier) {
				int action = soldier.getAction();
				if (action == SagaSaiyanInvadersEntity.Soldier.ACTION_MOON_CHARGE) return state.setAndContinue(DBSagasAnimations.ANIM_LARGE_BALL_CAST);
				if (action == SagaSaiyanInvadersEntity.Soldier.ACTION_MOON_THROW) return state.setAndContinue(DBSagasAnimations.ANIM_LARGE_BALL_FIRE);
			}
			return DBSagasAnimationHandler.skillPredicate(state, this.owner);
		}

		@Override
		public AnimatableInstanceCache getAnimatableInstanceCache() {
			return this.animationCache;
		}

		@Override
		public double getTick(Object object) {
			return this.tickCount;
		}

		private void sync(DBSagasEntity owner) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, this).orElse(null);
			if (data == null) return;

			boolean oozaru = owner instanceof SagaSaiyanInvadersEntity.Oozaru;
			boolean villain = owner.isSupervillain();
			SagaSaiyanInvadersEntity.Look look = ((SagaSaiyanInvadersEntity.Invader) owner).getLook();
			if (look != this.appliedLook || oozaru != this.appliedOozaru || villain != this.appliedVillain) {
				this.applyLook(data, look, oozaru, villain);
			}

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
			this.syncStatus(owner, data);
			this.mirrorHurt(owner);
		}

		private void applyLook(StatsData data, SagaSaiyanInvadersEntity.Look look, boolean oozaru, boolean villain) {
			Character character = data.getCharacter();
			character.setRace("saiyan");
			character.setGender(Character.GENDER_MALE);

			RaceCharacterConfig config = ConfigManager.getRaceCharacter("saiyan");
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

			character.setBodyType(SAGA_BODY_TYPE);
			character.setBodyColor(look.skinHex());
			character.setHairId(look.hairId());
			character.setActiveHeadBone("hair");
			character.setRenderHairBase(true);
			character.setHairColor(look.hairHex());
			character.setHasSaiyanTail(true);
			character.setAuraColor(villain ? SUPERVILLAIN_AURA : DEFAULT_AURA);
			if (oozaru) character.setActiveForm(SaiyanForms.GROUP_OOZARU, SaiyanForms.OOZARU);
			else character.clearActiveForm();

			Status status = data.getStatus();
			status.setHasCreatedCharacter(true);
			status.setTailVisible(false);

			this.getInventory().armor.set(EquipmentSlot.CHEST.getIndex(), look.armorPiece(ArmorItem.Type.CHESTPLATE));
			this.getInventory().armor.set(EquipmentSlot.LEGS.getIndex(), look.armorPiece(ArmorItem.Type.LEGGINGS));
			this.getInventory().armor.set(EquipmentSlot.FEET.getIndex(), look.armorPiece(ArmorItem.Type.BOOTS));
			this.scouter = look.scouterStack();

			this.appliedLook = look;
			this.appliedOozaru = oozaru;
			this.appliedVillain = villain;
			this.refreshDimensions();
		}

		private void syncStatus(DBSagasEntity owner, StatsData data) {
			Status status = data.getStatus();
			boolean transforming = owner.isTransforming();
			status.setChargingKi(owner.isCharge() && !transforming);
			status.setAuraActive(owner.isCharge() || transforming);
			status.setStunEffect(owner.isStunned());
		}

		private void mirrorHurt(DBSagasEntity owner) {
			boolean freshHit = owner.hurtTime > this.lastHurtTime && owner.hurtTime >= owner.hurtDuration - 1;
			this.lastHurtTime = owner.hurtTime;
			if (!freshHit || owner.isDeadOrDying()) return;

			AnimatableManager<GeoAnimatable> manager = this.animationCache.getManagerForId(this.getId());
			manager.tryTriggerAnimation(DBSagasEntity.HURT_CONTROLLER,
					owner.getRandom().nextBoolean() ? DBSagasEntity.HURT_ANIM_LEFT : DBSagasEntity.HURT_ANIM_RIGHT);
		}
	}
}
