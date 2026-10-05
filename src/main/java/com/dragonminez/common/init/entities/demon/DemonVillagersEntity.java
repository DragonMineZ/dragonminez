package com.dragonminez.common.init.entities.demon;

import com.dragonminez.Reference;
import com.dragonminez.common.hair.HairPresets;
import com.dragonminez.common.init.CapsuleCorpMapTrade;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.server.world.worldboss.WorldBossManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DemonVillagersEntity {

	public static final int SKIN = 0xA1DAF0;
	public static final int PATROLLER_HAIR = 0xF2C2FF;
	private static final int[] VILLAGER_HAIR = {0xF2C2FF, 0xF0F0CC, 0xCCEDF0};

	private static final List<Map<ArmorItem.Type, RegistryObject<Item>>> OUTFITS = List.of(
			MainItems.AGE1000_ARMOR, MainItems.WARRIOR_CLAN_ARMOR, MainItems.WONDER_MAJIN_ARMOR,
			MainItems.DRAGON_CLAN_ARMOR, MainItems.MIGHTY_MAJIN_ARMOR, MainItems.MYSTIC_ARMOR);
	private static final int PATROLLER_OUTFIT = 0;

	private static final float BUG_TRADER_CHANCE = 0.4F;
	private static final int MIN_BUG_TRADES = 2;
	private static final int MAX_BUG_TRADES = 3;
	private static final int BUG_TRADE_XP = 1;
	private static final long RESTOCK_TICKS = 24000L;
	private static final List<BugTrade> BUG_TRADES = List.of(
			new BugTrade(MainItems.MEDI_BUG_ANT, 8, 6),
			new BugTrade(MainItems.MEDI_BUG_BEETLE_PAIR, 16, 2),
			new BugTrade(MainItems.MEDI_BUG_RHINO, 6, 6),
			new BugTrade(MainItems.MEDI_BUG_WORM, 6, 6));

	private DemonVillagersEntity() {}

	private record BugTrade(RegistryObject<Item> item, int emeralds, int maxUses) {}

	public record Look(int hairId, int hairColor, int outfit, boolean female) {
		public static final Look DEFAULT = new Look(1, PATROLLER_HAIR, 1, false);

		public static Look villager(RandomSource random) {
			return new Look(randomHair(random), VILLAGER_HAIR[random.nextInt(VILLAGER_HAIR.length)],
					1 + random.nextInt(OUTFITS.size() - 1), random.nextBoolean());
		}

		public static Look patroller(RandomSource random) {
			return new Look(randomHair(random), PATROLLER_HAIR, PATROLLER_OUTFIT, false);
		}

		private static int randomHair(RandomSource random) {
			List<Integer> presets = HairPresets.ids().stream().filter(id -> id != HairPresets.BALD_PRESET_ID).toList();
			return presets.isEmpty() ? 1 : presets.get(random.nextInt(presets.size()));
		}

		public CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putInt("Hair", this.hairId);
			tag.putInt("HairColor", this.hairColor);
			tag.putInt("Outfit", this.outfit);
			tag.putBoolean("Female", this.female);
			return tag;
		}

		public static Look load(CompoundTag tag) {
			if (tag == null || !tag.contains("Hair")) return DEFAULT;
			return new Look(tag.getInt("Hair"), tag.getInt("HairColor"),
					Math.floorMod(tag.getInt("Outfit"), OUTFITS.size()), tag.getBoolean("Female"));
		}

		public ItemStack armorPiece(ArmorItem.Type type) {
			RegistryObject<Item> item = OUTFITS.get(Math.floorMod(this.outfit, OUTFITS.size())).get(type);
			return item != null ? new ItemStack(item.get()) : ItemStack.EMPTY;
		}

		public String hairHex() {
			return String.format("#%06X", this.hairColor & 0xFFFFFF);
		}

		public String skinHex() {
			return String.format("#%06X", SKIN & 0xFFFFFF);
		}
	}

	public static class Resident extends AbstractVillager {
		private static final EntityDataAccessor<CompoundTag> LOOK = SynchedEntityData.defineId(Resident.class, EntityDataSerializers.COMPOUND_TAG);

		private Look look = Look.DEFAULT;
		private boolean bugTrader;
		private long lastRestock;

		public Resident(EntityType<? extends Resident> type, Level level) {
			super(type, level);
			this.setPersistenceRequired();
			if (!level.isClientSide) {
				this.setLook(this.randomLook(this.random));
				this.bugTrader = this.random.nextFloat() < BUG_TRADER_CHANCE;
			}
		}

		public static AttributeSupplier.Builder createAttributes() {
			return Mob.createMobAttributes()
					.add(Attributes.MAX_HEALTH, 40.0D)
					.add(Attributes.MOVEMENT_SPEED, 0.5D)
					.add(Attributes.FOLLOW_RANGE, 32.0D);
		}

		protected Look randomLook(RandomSource random) {
			return Look.villager(random);
		}

		public Look getLook() {
			return this.look;
		}

		public void setLook(Look look) {
			this.look = look;
			this.entityData.set(LOOK, look.save());
		}

		public void setHome(BlockPos home, int radius) {
			this.restrictTo(home, radius);
		}

		@Override
		protected void registerGoals() {
			this.goalSelector.addGoal(0, new FloatGoal(this));
			this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
			this.goalSelector.addGoal(1, new PanicGoal(this, 0.6D));
			this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
			this.goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 0.45D));
			this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.4D));
			this.goalSelector.addGoal(5, new InteractGoal(this, Player.class, 3.0F, 1.0F));
			this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Mob.class, 8.0F));
			this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
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
		public void addAdditionalSaveData(CompoundTag tag) {
			super.addAdditionalSaveData(tag);
			tag.put("DemonLook", this.look.save());
			tag.putBoolean("BugTrader", this.bugTrader);
			tag.putLong("LastRestock", this.lastRestock);
			if (this.hasRestriction()) {
				tag.putLong("Home", this.getRestrictCenter().asLong());
				tag.putInt("HomeRadius", (int) this.getRestrictRadius());
			}
		}

		@Override
		public void readAdditionalSaveData(CompoundTag tag) {
			super.readAdditionalSaveData(tag);
			if (tag.contains("DemonLook")) this.setLook(Look.load(tag.getCompound("DemonLook")));
			this.bugTrader = tag.getBoolean("BugTrader");
			if (tag.contains("LastRestock")) this.lastRestock = tag.getLong("LastRestock");
			if (tag.contains("Home")) this.restrictTo(BlockPos.of(tag.getLong("Home")), tag.getInt("HomeRadius"));
		}

		@Override
		public InteractionResult mobInteract(Player player, InteractionHand hand) {
			if (!this.isAlive() || this.isTrading() || this.isBaby()) return super.mobInteract(player, hand);
			if (hand == InteractionHand.MAIN_HAND) player.awardStat(Stats.TALKED_TO_VILLAGER);
			if (!this.level().isClientSide && !this.prepareOffers().isEmpty()) {
				this.setTradingPlayer(player);
				this.openTradingScreen(player, this.getDisplayName(), 1);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide);
		}

		protected MerchantOffers prepareOffers() {
			MerchantOffers offers = this.getOffers();
			long time = this.level().getGameTime();
			if (time - this.lastRestock >= RESTOCK_TICKS) {
				this.lastRestock = time;
				for (MerchantOffer offer : offers) {
					CompoundTag result = offer.getResult().getTag();
					if (result == null || !result.getBoolean(CapsuleCorpMapTrade.PENDING_TAG)) offer.resetUses();
				}
			}
			return offers;
		}

		@Override
		protected void updateTrades() {
			if (!this.bugTrader) return;
			List<BugTrade> pool = new ArrayList<>(BUG_TRADES);
			int count = MIN_BUG_TRADES + this.random.nextInt(MAX_BUG_TRADES - MIN_BUG_TRADES + 1);
			for (int i = 0; i < count && !pool.isEmpty(); i++) {
				BugTrade trade = pool.remove(this.random.nextInt(pool.size()));
				this.offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, trade.emeralds()),
						new ItemStack(trade.item().get()), trade.maxUses(), BUG_TRADE_XP, 0.05F));
			}
		}

		@Override
		protected void rewardTradeXp(MerchantOffer offer) {
		}

		@Override
		public boolean showProgressBar() {
			return false;
		}

		@Override
		public boolean removeWhenFarAway(double distance) {
			return false;
		}

		@Nullable
		@Override
		public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
			return null;
		}

		@Override
		protected SoundEvent getHurtSound(DamageSource source) {
			return SoundEvents.PLAYER_HURT;
		}

		@Override
		protected SoundEvent getDeathSound() {
			return SoundEvents.PLAYER_DEATH;
		}
	}

	public static class TimePatroller extends Resident {
		public static final int TAMAGAMIS = 3;
		private static final int BOSS_MAP_USES = 4;
		private static final int BOSS_MAP_XP = 10;
		private static final String TAMAGAMI_TAG = "dmz_tamagami";
		private static final String BOSS_MAP_TAG = "dmz_worldboss_map";

		private record BossMap(String key, int price) {}

		private static final List<BossMap> BOSS_MAPS = List.of(
				new BossMap(WorldBossEntity.TAMAGAMI_3, 12),
				new BossMap(WorldBossEntity.TAMAGAMI_2, 16),
				new BossMap(WorldBossEntity.TAMAGAMI_1, 20),
				new BossMap(WorldBossEntity.GOMAH, 24));

		public TimePatroller(EntityType<? extends TimePatroller> type, Level level) {
			super(type, level);
		}

		@Override
		protected Look randomLook(RandomSource random) {
			return Look.patroller(random);
		}

		@Nullable
		public static BlockPos locateTamagami(ServerLevel level, int number) {
			return WorldBossManager.getLair(level.getServer(), WorldBossEntity.tamagamiKey(number));
		}

		@Override
		protected void updateTrades() {
			if (!(this.level() instanceof ServerLevel level) || !level.getServer().isSameThread()) return;
			for (BossMap bossMap : BOSS_MAPS) this.offers.add(bossMapOffer(level, bossMap));
		}

		@Override
		protected MerchantOffers prepareOffers() {
			MerchantOffers offers = super.prepareOffers();
			if (!(this.level() instanceof ServerLevel level)) return offers;
			Set<String> present = new HashSet<>();
			for (int i = 0; i < offers.size(); i++) {
				CompoundTag tag = offers.get(i).getResult().getTag();
				BossMap bossMap = bossMapOf(tag);
				if (bossMap == null) continue;
				present.add(bossMap.key());
				if (!tag.getBoolean(CapsuleCorpMapTrade.PENDING_TAG) && tag.contains(BOSS_MAP_TAG)) continue;
				if (WorldBossManager.getLair(level.getServer(), bossMap.key()) != null || !tag.contains(BOSS_MAP_TAG)) {
					offers.set(i, bossMapOffer(level, bossMap));
				}
			}
			for (BossMap bossMap : BOSS_MAPS) {
				if (!present.contains(bossMap.key())) offers.add(bossMapOffer(level, bossMap));
			}
			return offers;
		}

		@Nullable
		private static BossMap bossMapOf(@Nullable CompoundTag tag) {
			if (tag == null) return null;
			String key = tag.contains(BOSS_MAP_TAG) ? tag.getString(BOSS_MAP_TAG)
					: tag.contains(TAMAGAMI_TAG) ? WorldBossEntity.tamagamiKey(tag.getInt(TAMAGAMI_TAG)) : null;
			if (key == null) return null;
			for (BossMap bossMap : BOSS_MAPS) {
				if (bossMap.key().equals(key)) return bossMap;
			}
			return null;
		}

		private static MerchantOffer bossMapOffer(ServerLevel level, BossMap bossMap) {
			BlockPos lair = WorldBossManager.getLair(level.getServer(), bossMap.key());
			ItemStack map;
			if (lair != null) {
				map = MapItem.create(level, lair.getX(), lair.getZ(), (byte) 2, true, true);
				MapItem.renderBiomePreviewMap(level, map);
				MapItemSavedData.addTargetDecoration(map, lair, "+", MapDecoration.Type.RED_X);
			} else {
				map = new ItemStack(Items.FILLED_MAP);
				map.getOrCreateTag().putBoolean(CapsuleCorpMapTrade.PENDING_TAG, true);
			}
			map.setHoverName(Component.translatable("filled_map." + Reference.MOD_ID + "." + bossMap.key()));
			map.getOrCreateTag().putString(BOSS_MAP_TAG, bossMap.key());

			MerchantOffer offer = new MerchantOffer(new ItemStack(Items.EMERALD, bossMap.price()), map, BOSS_MAP_USES, BOSS_MAP_XP, 0.2F);
			if (lair == null) offer.setToOutOfStock();
			return offer;
		}
	}
}
