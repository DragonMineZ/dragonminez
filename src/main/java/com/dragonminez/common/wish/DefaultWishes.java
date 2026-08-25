package com.dragonminez.common.wish;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.util.types.items.GenericItemDTO;
import com.dragonminez.common.wish.wishes.ChangeDifficultyWish;
import com.dragonminez.common.wish.wishes.CommandWish;
import com.dragonminez.common.wish.wishes.ItemListWish;
import com.dragonminez.common.wish.wishes.PassiveResetWish;
import com.dragonminez.common.wish.wishes.ReCustomizeWish;
import com.dragonminez.common.wish.wishes.RelocateStatsWish;
import com.dragonminez.common.wish.wishes.ResetStoryWish;
import com.dragonminez.common.wish.wishes.TPSWish;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The stock wish lists for Shenron and Porunga.
 *
 * <p>Read by both {@code WishManager}, which seeds a new world's
 * {@code <world>/dragonminez/wishes/*.json}, and {@code DMZDragonWishProvider}, which
 * generates the shipped datapack, so the two cannot drift apart.
 */
public final class DefaultWishes {

	private DefaultWishes() {
	}

	public static List<Wish> shenron() {
		List<Wish> wishes = new ArrayList<>();

		wishes.add(new ItemListWish("wish.shenron.senzu.name", "wish.shenron.senzu.desc",
				MainItems.SENZU_BEAN, 16));
		wishes.add(new TPSWish("wish.shenron.tps.name", "wish.shenron.tps.desc", 5000));
		wishes.add(new ItemListWish("wish.shenron.powerpole.name", "wish.shenron.powerpole.desc",
				MainItems.POWER_POLE));
		wishes.add(new ItemListWish("wish.shenron.mightfruit.name", "wish.shenron.mightfruit.desc",
				MainItems.MIGHT_TREE_FRUIT, 16));
		wishes.add(new ItemListWish("wish.shenron.namekcpu.name", "wish.shenron.namekcpu.desc",
				MainItems.T2_RADAR_CPU, 4));
		wishes.add(new ItemListWish("wish.shenron.saiyanship.name", "wish.shenron.saiyanship.desc",
				MainItems.NAVE_SAIYAN_ITEM));

		wishes.add(new PassiveResetWish("wish.shenron.racialskillreset.name", "wish.shenron.racialskillreset.desc"));
		wishes.add(new ReCustomizeWish("wish.shenron.customization.name", "wish.shenron.customization.desc"));
		wishes.add(new ChangeDifficultyWish("wish.shenron.changedifficulty.name", "wish.shenron.changedifficulty.desc"));
		wishes.add(new ResetStoryWish("wish.shenron.resetstory.name", "wish.shenron.resetstory.desc"));
		wishes.add(new CommandWish("wish.shenron.revive.name", "wish.shenron.revive.desc", "dmzrevive %player%"));

		List<GenericItemDTO> materials = new ArrayList<>();
		materials.add(new GenericItemDTO(MainItems.KIKONO_SHARD.getId(), 32));
		materials.add(new GenericItemDTO(Items.IRON_INGOT, 64));
		wishes.add(new ItemListWish("wish.shenron.materials.name", "wish.shenron.materials.desc", materials));

		wishes.add(new ItemListWish("wish.shenron.strongest.name", "wish.shenron.strongest.desc",
				MainItems.STRONGEST_ARMOR));

		return wishes;
	}

	public static List<Wish> porunga() {
		List<Wish> wishes = new ArrayList<>();

		wishes.add(new ItemListWish("wish.porunga.senzu.name", "wish.porunga.senzu.desc",
				MainItems.SENZU_BEAN, 32));
		wishes.add(new TPSWish("wish.porunga.tps.name", "wish.porunga.tps.desc", 15000));
		wishes.add(new ItemListWish("wish.porunga.bravesword.name", "wish.porunga.bravesword.desc",
				MainItems.BRAVE_SWORD));

		wishes.add(new PassiveResetWish("wish.porunga.racialskillreset.name", "wish.porunga.racialskillreset.desc"));
		wishes.add(new ReCustomizeWish("wish.porunga.customization.name", "wish.porunga.customization.desc"));
		wishes.add(new RelocateStatsWish("wish.porunga.relocatestats.name", "wish.porunga.relocatestats.desc"));
		wishes.add(new ChangeDifficultyWish("wish.porunga.changedifficulty.name", "wish.porunga.changedifficulty.desc"));
		wishes.add(new ResetStoryWish("wish.porunga.resetstory.name", "wish.porunga.resetstory.desc"));
		wishes.add(new CommandWish("wish.porunga.revive.name", "wish.porunga.revive.desc", "dmzrevive %player%"));

		List<GenericItemDTO> materials = new ArrayList<>();
		materials.add(new GenericItemDTO(MainItems.KIKONO_SHARD.getId(), 64));
		materials.add(new GenericItemDTO(Items.IRON_INGOT, 128));
		wishes.add(new ItemListWish("wish.porunga.materials.name", "wish.porunga.materials.desc", materials));

		wishes.add(new ItemListWish("wish.porunga.invincible.name", "wish.porunga.invincible.desc",
				MainItems.INVENCIBLE_ARMOR));
		wishes.add(new ItemListWish("wish.porunga.invincible_blue.name", "wish.porunga.invincible_blue.desc",
				MainItems.INVENCIBLE_BLUE_ARMOR));

		List<GenericItemDTO> pothalaYellow = new ArrayList<>();
		pothalaYellow.add(new GenericItemDTO(MainItems.POTHALA_PAIR.getId(), 1));
		wishes.add(new ItemListWish("wish.porunga.pothala_yellow.name", "wish.porunga.pothala_yellow.desc",
				pothalaYellow));

		List<GenericItemDTO> pothalaGreen = new ArrayList<>();
		pothalaGreen.add(new GenericItemDTO(MainItems.GREEN_POTHALA_PAIR.getId(), 1));
		wishes.add(new ItemListWish("wish.porunga.pothala_green.name", "wish.porunga.pothala_green.desc",
				pothalaGreen));

		return wishes;
	}
}
