package com.dragonminez.common.init.entities.namek;

import com.dragonminez.Reference;
import com.dragonminez.common.init.CapsuleCorpMapTrade;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public class CCNamekianEntity extends NamekTraderEntity {

	private static final int MAP_XP = 40;
	private static final int MAP_MAX_USES = 8;

	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/cc_namekian.png");

	public CCNamekianEntity(EntityType<? extends Villager> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@Override
	public ResourceLocation getCurrentTexture() {
		return TEXTURE;
	}

	@Override
	protected Component getTypeName() {
		return Component.translatable("entity." + Reference.MOD_ID + ".cc_namekian");
	}

	@Override
	protected void updateTrades() {
		if (this.offers == null) {
			this.offers = new MerchantOffers();
		}
		if (!this.offers.isEmpty()) {
			return;
		}
		if (this.level() instanceof ServerLevel serverLevel && !serverLevel.getServer().isSameThread()) {
			return;
		}

		addMapOffer(new ItemStack(MainItems.T1_RADAR_CHIP.get(), 1),
				DMZStructures.FRIEZA_SHIP, "dragonminez.frieza_ship");

		addMapOffer(new ItemStack(MainItems.HEALING_BUCKET.get(), 1),
				DMZStructures.ELDER_GURU, "dragonminez.elder_guru");
	}

	private void addMapOffer(ItemStack cost, ResourceKey<Structure> destination, String displayName) {
		CapsuleCorpMapTrade trade = new CapsuleCorpMapTrade(new ItemStack(Items.MAP, 1), cost, destination,
				displayName, MapDecoration.Type.RED_X, MAP_MAX_USES, MAP_XP);
		MerchantOffer offer = trade.getOffer(this, this.getRandom());
		if (offer != null) {
			this.offers.add(offer);
		}
	}
}
