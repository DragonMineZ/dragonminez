package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

public class CapsuleCorpMapTrade implements VillagerTrades.ItemListing {

	public static final String STRUCTURE_TAG = "dmz_structure";
	public static final String PENDING_TAG = "dmz_pending";

	private final ItemStack priceA;
	private final ItemStack priceB;
	private final ResourceKey<Structure> destination;
	private final String displayName;
	private final MapDecoration.Type destinationType;
	private final int maxUses;
	private final int villagerXp;

	public CapsuleCorpMapTrade(ItemStack priceA, ItemStack priceB, ResourceKey<Structure> destination,
			String displayName, MapDecoration.Type destinationType, int maxUses, int villagerXp) {
		this.priceA = priceA;
		this.priceB = priceB;
		this.destination = destination;
		this.displayName = displayName;
		this.destinationType = destinationType;
		this.maxUses = maxUses;
		this.villagerXp = villagerXp;
	}

	@Nullable
	@Override
	public MerchantOffer getOffer(Entity trader, RandomSource random) {
		if (!(trader.level() instanceof ServerLevel serverLevel)) {
			return null;
		}
		if (!serverLevel.getServer().isSameThread()) {
			return null;
		}

		BlockPos pos = StructureLocator.locateStructure(serverLevel, this.destination, trader.blockPosition());

        ItemStack map = pos != null
				? createMapStack(serverLevel, pos, this.destination, this.displayName, this.destinationType)
				: createPendingMapStack(this.destination, this.displayName);

		MerchantOffer offer = new MerchantOffer(
				this.priceA.copy(),
				this.priceB.copy(),
				map,
				this.maxUses,
				this.villagerXp,
				0.2F);
		if (pos == null) offer.setToOutOfStock();
		return offer;
	}

	private static ItemStack createPendingMapStack(ResourceKey<Structure> destination, String displayName) {
		ItemStack map = new ItemStack(Items.FILLED_MAP);
		map.setHoverName(Component.translatable("filled_map." + displayName));
		map.getOrCreateTag().putString(STRUCTURE_TAG, destination.location().toString());
		map.getOrCreateTag().putBoolean(PENDING_TAG, true);
		return map;
	}

	private static ItemStack createMapStack(ServerLevel level, BlockPos pos, ResourceKey<Structure> destination,
			String displayName, MapDecoration.Type destinationType) {
		ItemStack map = MapItem.create(level, pos.getX(), pos.getZ(), (byte) 2, true, true);
		MapItem.renderBiomePreviewMap(level, map);
		MapItemSavedData.addTargetDecoration(map, pos, "+", destinationType);
		map.setHoverName(Component.translatable("filled_map." + displayName));
		map.getOrCreateTag().putString(STRUCTURE_TAG, destination.location().toString());
		return map;
	}

	public static void refreshStaleMapOffers(ServerLevel level, AbstractVillager merchant) {
		MerchantOffers offers = merchant.getOffers();
		for (int i = 0; i < offers.size(); i++) {
			MerchantOffer offer = offers.get(i);
			ItemStack result = offer.getResult();
			if (!result.is(Items.FILLED_MAP)) continue;

			ResourceKey<Structure> key = structureKeyFrom(level, result);
			if (key == null) continue;

			BlockPos pos = StructureLocator.locateStructure(level, key, merchant.blockPosition());
			if (pos == null || matchesTarget(result, pos)) continue;

			CompoundTag resultTag = result.getTag();
			boolean wasPending = resultTag != null && resultTag.getBoolean(PENDING_TAG);

			ItemStack fresh = createMapStack(level, pos, key,
					Reference.MOD_ID + "." + key.location().getPath(), MapDecoration.Type.RED_X);
			offers.set(i, new MerchantOffer(offer.getBaseCostA(), offer.getCostB(), fresh,
					wasPending ? 0 : offer.getUses(), offer.getMaxUses(), offer.getXp(),
					offer.getPriceMultiplier(), offer.getDemand()));
		}
	}

	@Nullable
	private static ResourceKey<Structure> structureKeyFrom(ServerLevel level, ItemStack map) {
		ResourceLocation id = null;
		CompoundTag tag = map.getTag();
		if (tag != null && tag.contains(STRUCTURE_TAG, Tag.TAG_STRING)) {
			id = ResourceLocation.tryParse(tag.getString(STRUCTURE_TAG));
		} else if (map.getHoverName().getContents() instanceof TranslatableContents translatable) {
			String prefix = "filled_map." + Reference.MOD_ID + ".";
			String translationKey = translatable.getKey();
			if (translationKey.startsWith(prefix)) {
				id = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
						translationKey.substring(prefix.length()));
			}
		}
		if (id == null) return null;
		if (!level.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(id)) return null;
		return ResourceKey.create(Registries.STRUCTURE, id);
	}

	private static boolean matchesTarget(ItemStack map, BlockPos pos) {
		CompoundTag tag = map.getTag();
		if (tag == null || !tag.contains("Decorations", Tag.TAG_LIST)) return false;
		ListTag decorations = tag.getList("Decorations", Tag.TAG_COMPOUND);
		for (int i = 0; i < decorations.size(); i++) {
			CompoundTag decoration = decorations.getCompound(i);
			if (!"+".equals(decoration.getString("id"))) continue;
			return (int) decoration.getDouble("x") == pos.getX()
					&& (int) decoration.getDouble("z") == pos.getZ();
		}
		return false;
	}
}
