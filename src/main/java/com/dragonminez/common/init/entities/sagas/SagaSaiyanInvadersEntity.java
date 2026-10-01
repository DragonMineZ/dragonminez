package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.hair.HairPresets;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;

public final class SagaSaiyanInvadersEntity {

	public static final String MOON_MESSAGE = "raid.dragonminez.saiyan_assault.moon";

	private static final int KI_MAIN = 0xFFE36B;
	private static final int KI_BORDER = 0xFF9A1F;

	private static final int[] SKIN_TONES = {0xFFD3C9, 0xF4C7AE, 0xE8B593, 0xD9A07B, 0xC48A64};
	private static final int[] HAIR_TONES = {0x222629, 0x1C1F24, 0x26222A, 0x201C1F, 0x2A2628, 0x191C21};

	private static final List<Map<ArmorItem.Type, RegistryObject<Item>>> ARMOR_SETS = List.of(
			MainItems.VEGETA_SAIYAN_ARMOR, MainItems.RADITZ_ARMOR, MainItems.TURLES_ARMOR, MainItems.BARDOCK_DBZ_ARMOR);
	private static final List<RegistryObject<Item>> SCOUTERS = List.of(
			MainItems.GREEN_SCOUTER, MainItems.RED_SCOUTER, MainItems.BLUE_SCOUTER, MainItems.PURPLE_SCOUTER);

	private SagaSaiyanInvadersEntity() {}

	public interface Invader {
		Look getLook();
	}

	public record Look(int hairId, int hairColor, int skinColor, int armor, int scouter) {
		public static final Look DEFAULT = new Look(1, HAIR_TONES[0], SKIN_TONES[0], 0, 0);

		public static Look random(RandomSource random) {
			List<Integer> presets = HairPresets.ids();
			int hair = presets.isEmpty() ? 1 : presets.get(random.nextInt(presets.size()));
			return new Look(hair,
					HAIR_TONES[random.nextInt(HAIR_TONES.length)],
					SKIN_TONES[random.nextInt(SKIN_TONES.length)],
					random.nextInt(ARMOR_SETS.size()),
					random.nextInt(SCOUTERS.size()));
		}

		public CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putInt("Hair", this.hairId);
			tag.putInt("HairColor", this.hairColor);
			tag.putInt("SkinColor", this.skinColor);
			tag.putInt("Armor", this.armor);
			tag.putInt("Scouter", this.scouter);
			return tag;
		}

		public static Look load(CompoundTag tag) {
			if (tag == null || !tag.contains("Hair")) return DEFAULT;
			return new Look(tag.getInt("Hair"), tag.getInt("HairColor"), tag.getInt("SkinColor"),
					Math.floorMod(tag.getInt("Armor"), ARMOR_SETS.size()),
					Math.floorMod(tag.getInt("Scouter"), SCOUTERS.size()));
		}

		public ItemStack armorPiece(ArmorItem.Type type) {
			RegistryObject<Item> item = ARMOR_SETS.get(Math.floorMod(this.armor, ARMOR_SETS.size())).get(type);
			return item != null ? new ItemStack(item.get()) : ItemStack.EMPTY;
		}

		public ItemStack scouterStack() {
			return new ItemStack(SCOUTERS.get(Math.floorMod(this.scouter, SCOUTERS.size())).get());
		}

		public String hairHex() {
			return String.format("#%06X", this.hairColor & 0xFFFFFF);
		}

		public String skinHex() {
			return String.format("#%06X", this.skinColor & 0xFFFFFF);
		}
	}

	public static class Soldier extends DBSagasEntity implements Invader {
		private static final EntityDataAccessor<CompoundTag> LOOK = SynchedEntityData.defineId(Soldier.class, EntityDataSerializers.COMPOUND_TAG);
		private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(Soldier.class, EntityDataSerializers.INT);

		public static final int ACTION_NONE = 0;
		public static final int ACTION_MOON_CHARGE = 1;
		public static final int ACTION_MOON_THROW = 2;

		private static final double HOME_LEASH_SQR = 12.0D * 12.0D;
		private static final int MOON_AWAKE_DELAY = 60;
		private static final int MOON_THROW_TICK = 30;
		private static final int MOON_END_TICK = 44;
		private static final double MOON_SEARCH_RADIUS = 96.0D;
		private static final double MOON_CALLER_RADIUS = 64.0D;
		private static final double MOON_MESSAGE_RADIUS = 96.0D;
		private static final int MOON_COLOR = 0xF5F3D0;
		private static final float MOON_SIZE = 2.5F;

		private final boolean elite;
		private Look look = Look.DEFAULT;
		private BlockPos home;
		private int awakeTicks;
		private int moonTicks;
		private boolean moonSighted;

		public Soldier(EntityType<? extends Monster> type, Level level) {
			this(type, level, false);
		}

		protected Soldier(EntityType<? extends Monster> type, Level level, boolean elite) {
			super(type, level);
			this.elite = elite;

			this.setBattlePower(elite ? 4000 : 1500);
			this.setCanFly(true);
			this.setFlySpeed(elite ? 0.38D : 0.32D);
			this.setKiBlastSpeed(elite ? 1.3F : 1.1F);
			this.setAuraColor(0xFFFFFF);
			this.setDBZStyle(0);
			this.setEvade(true, elite ? 60 : 100);

			if (elite) this.setAllowedCombos(120, ComboType.BASIC, ComboType.AIR, ComboType.RAPID_KICKS);
			else this.setAllowedCombos(160, ComboType.BASIC, ComboType.RAPID_KICKS);

			this.addKiSkill(KiSkillType.KI_SMALL, elite ? 50 : 70, 1.0F, KI_MAIN, KI_BORDER);
			if (elite) {
				this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 220, 1.1F, KI_MAIN, KI_BORDER);
				this.addKiSkill(KiSkillType.KI_MEDIUM_BALL, 260, 1.4F);
			}

			if (!level.isClientSide) this.setLook(Look.random(this.random));
		}

		public boolean isElite() {
			return this.elite;
		}

		@Override
		public Look getLook() {
			return this.look != null ? this.look : Look.DEFAULT;
		}

		public void setLook(Look look) {
			this.look = look;
			this.entityData.set(LOOK, look.save());
		}

		public int getAction() {
			return this.entityData.get(ACTION);
		}

		private void setAction(int action) {
			this.entityData.set(ACTION, action);
		}

		public void setHome(BlockPos home) {
			this.home = home;
		}

		@Override
		protected void defineSynchedData() {
			super.defineSynchedData();
			this.entityData.define(LOOK, new CompoundTag());
			this.entityData.define(ACTION, ACTION_NONE);
		}

		@Override
		public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
			super.onSyncedDataUpdated(key);
			if (LOOK.equals(key)) this.look = Look.load(this.entityData.get(LOOK));
		}

		@Override
		public void tick() {
			super.tick();
			if (this.level().isClientSide || !this.isAlive()) return;

			this.tickHome();
			if (this.elite) this.tickMoon();
		}

		private void tickHome() {
			if (this.home == null || this.getTarget() != null || this.isCasting() || this.isComboing()) return;
			if (this.tickCount % 20 != 0) return;
			if (this.distanceToSqr(Vec3.atBottomCenterOf(this.home)) <= HOME_LEASH_SQR) return;
			this.getNavigation().moveTo(this.home.getX() + 0.5D, this.home.getY(), this.home.getZ() + 0.5D, 1.0D);
		}

		private boolean canCallMoon() {
			return !this.isTransformationDisabled() && !this.getPersistentData().getBoolean("dmz_quest_no_transform");
		}

		private void tickMoon() {
			if (this.isTransforming() || !this.canCallMoon()) {
				if (this.moonTicks > 0) this.endMoonCall();
				return;
			}

			if (this.moonTicks > 0) {
				this.advanceMoonCall();
				return;
			}

			boolean engaged = this.getTarget() != null && !this.isRaidDormant();
			this.awakeTicks = engaged ? this.awakeTicks + 1 : 0;
			if (!engaged || this.tickCount % 10 != 0) return;

			KiBlastEntity moon = this.findMoon();
			if (moon != null) {
				if (moon.isParked() && !this.isStunned()) {
					this.moonSighted = true;
					this.beginOozaruTransformation();
				}
				return;
			}

			if (this.awakeTicks < MOON_AWAKE_DELAY || this.isCasting() || this.isComboing() || this.isStunned()) return;
			if (this.anotherCallerNearby()) return;

			this.moonTicks = 1;
			this.setAction(ACTION_MOON_CHARGE);
			this.getNavigation().stop();
			this.playSound(MainSounds.KI_EXPLOSION_CHARGE.get(), 1.5F, 0.8F);
		}

		private void advanceMoonCall() {
			if (this.isStunned()) {
				this.endMoonCall();
				return;
			}

			this.moonTicks++;
			this.getNavigation().stop();
			this.setXRot(-70.0F);

			if (this.moonTicks == MOON_THROW_TICK) this.throwMoon();
			if (this.moonTicks >= MOON_END_TICK) this.endMoonCall();
		}

		private void endMoonCall() {
			this.moonTicks = 0;
			this.setAction(ACTION_NONE);
		}

		private void throwMoon() {
			KiBlastEntity moon = new KiBlastEntity(this.level(), this);
			moon.setupFakeMoonPlayer(this, 1.0F, MOON_COLOR, 0xFFFFFF, MOON_SIZE);
			moon.setFiring(true);
			this.setAction(ACTION_MOON_THROW);
			this.playSound(MainSounds.KIBLAST_ATTACK.get(), 2.0F, 0.6F);

			if (!(this.level() instanceof ServerLevel level)) return;
			double radiusSqr = MOON_MESSAGE_RADIUS * MOON_MESSAGE_RADIUS;
			for (ServerPlayer player : level.players()) {
				if (player.distanceToSqr(this) <= radiusSqr) player.sendSystemMessage(Component.translatable(MOON_MESSAGE));
			}
		}

		private KiBlastEntity findMoon() {
			AABB box = this.getBoundingBox().inflate(MOON_SEARCH_RADIUS, 64.0D, MOON_SEARCH_RADIUS);
			KiBlastEntity best = null;
			double bestDistance = Double.MAX_VALUE;
			for (KiBlastEntity ki : this.level().getEntitiesOfClass(KiBlastEntity.class, box,
					k -> k.getKiRenderType() == KiBlastEntity.RENDER_FAKE_MOON && k.isFiring())) {
				double distance = ki.distanceToSqr(this);
				if (distance < bestDistance) {
					best = ki;
					bestDistance = distance;
				}
			}
			return best;
		}

		private boolean anotherCallerNearby() {
			return !this.level().getEntitiesOfClass(Soldier.class, this.getBoundingBox().inflate(MOON_CALLER_RADIUS),
					other -> other != this && other.isAlive() && other.moonTicks > 0).isEmpty();
		}

		private void beginOozaruTransformation() {
			if (this.isCasting()) this.stopCasting();
			if (this.isComboing()) this.stopCombo();
			this.setFlying(false);
			this.setFlyingFast(false);
			this.setNoGravity(false);
			this.startTransformation();
		}

		@Override
		public void startCasting(int type) {
			if (this.moonTicks > 0) return;
			super.startCasting(type);
		}

		@Override
		public void startCombo(int comboId) {
			if (this.moonTicks > 0) return;
			super.startCombo(comboId);
		}

		@Override
		public boolean isMeleeAllowed() {
			return this.moonTicks == 0 && super.isMeleeAllowed();
		}

		@Override
		protected void handleCommonCombatMovement(LivingEntity target, boolean isActionActive) {
			super.handleCommonCombatMovement(target, isActionActive || this.moonTicks > 0);
		}

		@Override
		protected boolean hasTransformation() {
			return this.elite;
		}

		@Override
		protected boolean canTransform() {
			return this.moonSighted && super.canTransform();
		}

		@Override
		public EntityType<? extends DBSagasEntity> getNextTransform() {
			return this.elite ? MainEntities.SAGA_SAIYAN_OOZARU.get() : null;
		}

		@Override
		protected void finishTransformationSpawn(DBSagasEntity newEntity, boolean fullHealth) {
			if (newEntity instanceof Oozaru oozaru) oozaru.setLook(this.getLook());
			KiBlastEntity moon = this.findMoon();
			if (moon != null && moon.getOwner() == this) moon.setOwner(newEntity);
			super.finishTransformationSpawn(newEntity, fullHealth);
		}

		@Override
		public void addAdditionalSaveData(CompoundTag tag) {
			super.addAdditionalSaveData(tag);
			tag.put("SaiyanLook", this.getLook().save());
			if (this.home != null) tag.putLong("CraterHome", this.home.asLong());
			tag.putBoolean("MoonSighted", this.moonSighted);
		}

		@Override
		public void readAdditionalSaveData(CompoundTag tag) {
			super.readAdditionalSaveData(tag);
			if (tag.contains("SaiyanLook")) this.setLook(Look.load(tag.getCompound("SaiyanLook")));
			if (tag.contains("CraterHome")) this.home = BlockPos.of(tag.getLong("CraterHome"));
			this.moonSighted = tag.getBoolean("MoonSighted");
		}
	}

	public static class Elite extends Soldier {
		public Elite(EntityType<? extends Monster> type, Level level) {
			super(type, level, true);
		}
	}

	public static class Oozaru extends SagaOzaruEntity implements Invader {
		private static final EntityDataAccessor<CompoundTag> LOOK = SynchedEntityData.defineId(Oozaru.class, EntityDataSerializers.COMPOUND_TAG);

		private Look look = Look.DEFAULT;

		public Oozaru(EntityType<? extends Monster> type, Level level) {
			super(type, level);
			this.setBattlePower(30000);
			this.setCanFly(false);
			this.setDBZStyle(2);
			this.addKiSkill(KiSkillType.OOZARU_FIST, 260);
			this.addKiSkill(KiSkillType.OOZARU_SLAM, 200);
			if (!level.isClientSide) this.setLook(Look.random(this.random));
		}

		public static AttributeSupplier.Builder createAttributes() {
			return SagaOzaruEntity.createAttributes()
					.add(EntityAttributes.KI_BLAST_DAMAGE.get(), 45.0D)
					.add(EntityAttributes.KI_BLAST_SPEED.get(), 1.5D)
					.add(EntityAttributes.FLY_SPEED.get(), 0.0D);
		}

		@Override
		public Look getLook() {
			return this.look != null ? this.look : Look.DEFAULT;
		}

		public void setLook(Look look) {
			this.look = look;
			this.entityData.set(LOOK, look.save());
		}

		@Override
		protected void defineSynchedData() {
			super.defineSynchedData();
			this.entityData.define(LOOK, new CompoundTag());
		}

		@Override
		public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
			super.onSyncedDataUpdated(key);
			if (LOOK.equals(key)) this.look = Look.load(this.entityData.get(LOOK));
		}

		@Override
		protected EntityDimensions getCoreDimensions() {
			return EntityDimensions.scalable(2.6F, 4.0F);
		}

		@Override
		protected DBSagasPart[] createHitboxParts() {
			return new DBSagasPart[] {
					new DBSagasPart(this, "legs", 3.2F, 3.0F, 0.0F, 0.0F, 1.5F),
					new DBSagasPart(this, "torso", 4.0F, 3.0F, 0.0F, 0.0F, 4.4F),
					new DBSagasPart(this, "head", 2.4F, 2.2F, 0.4F, 0.0F, 6.6F)
			};
		}

		@Override
		public void addAdditionalSaveData(CompoundTag tag) {
			super.addAdditionalSaveData(tag);
			tag.put("SaiyanLook", this.getLook().save());
		}

		@Override
		public void readAdditionalSaveData(CompoundTag tag) {
			super.readAdditionalSaveData(tag);
			if (tag.contains("SaiyanLook")) this.setLook(Look.load(tag.getCompound("SaiyanLook")));
		}
	}
}
