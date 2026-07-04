package com.dragonminez.common.init;

import com.dragonminez.server.world.structure.helper.StructureLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Intercambio que entrega un mapa de exploración apuntando a una estructura del
 * mod, a cambio de un costo personalizado ({@code priceA} + {@code priceB}).
 *
 * <p>Usa {@link StructureLocator} (el mismo localizador que el comando del mod)
 * en vez de {@code findNearestMapStructure}, porque las estructuras de DMZ usan
 * placements personalizados que el localizador vanilla no resuelve de forma
 * fiable. El nombre del mapa se traduce como {@code "filled_map." + displayName}.
 */
public class CapsuleCorpMapTrade implements VillagerTrades.ItemListing {

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
		if (pos == null) {
			return null;
		}

		ItemStack map = MapItem.create(serverLevel, pos.getX(), pos.getZ(), (byte) 2, true, true);
		MapItem.renderBiomePreviewMap(serverLevel, map);
		MapItemSavedData.addTargetDecoration(map, pos, "+", this.destinationType);
		map.setHoverName(Component.translatable("filled_map." + this.displayName));

		return new MerchantOffer(
				this.priceA.copy(),
				this.priceB.copy(),
				map,
				this.maxUses,
				this.villagerXp,
				0.2F);
	}
}
