package com.yuseix.dragonminez.events;

import com.yuseix.dragonminez.storyline.missions.Objective;
import com.yuseix.dragonminez.storyline.missions.Quest;
import com.yuseix.dragonminez.storyline.missions.objectives.ObjectiveCollectItem;
import com.yuseix.dragonminez.storyline.player.PlayerStorylineProvider;
import com.yuseix.dragonminez.storyline.sagas.Saga;
import com.yuseix.dragonminez.utils.DebugUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class StorylineEvents {

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		DebugUtils.dmzLog("StorylineManager initialized");
	}

	@SubscribeEvent
	public void onItemPickup(EntityItemPickupEvent event) {

		// Check the item that was picked up
		Item collectedItemId = event.getItem().getItem().getItem();

		// Retrieve the player's storyline capability
		event.getEntity().getCapability(PlayerStorylineProvider.CAPABILITY).ifPresent(playerStoryline -> {
			// Iterate through the active quests
			for (Saga saga : playerStoryline.getAllSagas().values()) {
				for (Quest quest : saga.getQuests()) {
					if (!quest.isCompleted()) {
						// Check each objective in the quest
						for (Objective objective : quest.getObjectives()) {
							if (objective instanceof ObjectiveCollectItem collectObjective) {
								// Pass the collected item to the objective
								collectObjective.onItemCollected(collectedItemId);
							}
						}
					}
				}
			}
		});
	}

	@SubscribeEvent
	public void onPlayerCloned(PlayerEvent.Clone event) {

		CompoundTag nbt = new CompoundTag();

		event.getOriginal().reviveCaps();

		event.getEntity().getCapability(PlayerStorylineProvider.CAPABILITY).ifPresent(playerStoryline ->
				event.getOriginal().getCapability(PlayerStorylineProvider.CAPABILITY).ifPresent(originalPlayerStoryline ->
						playerStoryline.loadNBTData(originalPlayerStoryline.saveNBTData(nbt)))
		);


		event.getOriginal().invalidateCaps();

	}

}
