package com.yuseix.dragonminez.stats.storymode;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.events.StoryEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID)
public class DMZStoryCapability {
	public static final Capability<DMZStoryCapability> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});
	private String currentQuestId;
	private String currentSaga;
	private final Map<String, Integer> entityKillCounts = new HashMap<>();
	private final Map<String, Integer> itemCollectionCounts = new HashMap<>();
	private final Set<String> completedQuests = new HashSet<>();
	private final Set<String> completedKillObjectives = new HashSet<>();
	private boolean structureFound = false;
	private boolean biomeFound = false;
	private boolean hasRequiredItem = false;

	public DMZStoryCapability(String startQuestId, String startSaga) {
		this.currentQuestId = startQuestId;
		this.currentSaga = startSaga;
	}

	public Map<String, Integer> getEntityKillCounts() {
		return entityKillCounts;
	}
	public String getCurrentQuestId() {
		return currentQuestId;
	}
	public void setCurrentQuestId(String QuestId) {
		this.currentQuestId = QuestId;
	}
	public String getCurrentSaga() {
		return currentSaga;
	}
	public void setCurrentSaga(String saga) {
		this.currentSaga = saga;
	}
	public Set<String> getCompletedQuests() {
		return completedQuests;
	}
	public boolean isBiomeFound() {
		return biomeFound;
	}
	public void setBiomeFound(boolean found) {
		this.biomeFound = found;
	}
	public boolean isStructureFound() {
		return structureFound;
	}
	public void setStructureFound(boolean found) {
		this.structureFound = found;
	}
	public void setHasRequiredItem(boolean hasItem) {
		this.hasRequiredItem = hasItem;
	}
	public void addItemCollected(String itemId) {
		itemCollectionCounts.put(itemId, itemCollectionCounts.getOrDefault(itemId, 0) + 1);
	}
	public Map<String, Integer> hasCollectedItems() {
		return itemCollectionCounts;
	}
	public int getItemCollectedCount(String itemId) {
		return itemCollectionCounts.getOrDefault(itemId, 0);
	}
	public void addKill(String entity) {
		entityKillCounts.put(entity, entityKillCounts.getOrDefault(entity, 0) + 1);
	}
	public int getKillCount(String entity) {
		return entityKillCounts.getOrDefault(entity, 0);
	}
	public void setKillObjectiveComplete(String entityId) {
		completedKillObjectives.add(entityId);
	}
	public boolean isKillObjectiveComplete(String entityId) {
		return completedKillObjectives.contains(entityId);
	}

	public boolean isQuestComplete(DMZQuest quest, Player player) {
		if (this.completedQuests.contains(quest.getId())) {
			return true;
		}

		QuestRequirement requirement = quest.getRequirement();

		if (requirement.getRequiredKills() != null) {
			for (Map.Entry<String, Integer> entry : requirement.getRequiredKills().entrySet()) {
				if (!this.isKillObjectiveComplete(entry.getKey())) {
					return false;
				}
			}
		}

		if (requirement.getRequiredItems() != null) {
			for (Map.Entry<String, Integer> entry : requirement.getRequiredItems().entrySet()) {
				if (this.getItemCollectedCount(entry.getKey()) < entry.getValue()) {
					return false;
				}
			}
		}

		if (requirement.getRequiredBiome() != null && !player.level().getBiome(player.blockPosition()).toString().equals(requirement.getRequiredBiome())) {
			return false;
		}

		if (requirement.getRequiredStructure() != null && !this.structureFound) {
			return false;
		}

		return true;
	}

	public void setQuestCompletion(String questId, boolean completed, Player player) {
		if (completed) {
			completedQuests.add(questId);
		} else {
			completedQuests.remove(questId);
		}
		StoryEvents.syncQuestData(player);
		StoryEvents.syncCompletedQuests(player);

	}

	public void resetProgress() {
		entityKillCounts.clear();
		structureFound = false;
		hasRequiredItem = false;
	}

	public void resetAllProgress() {
		getCompletedQuests().clear();
		setCurrentSaga("saiyan");
		setCurrentQuestId("saiyQuest1");
	}

	public DMZQuest getAvailableQuest() {
		return DMZStoryRegistry.getQuest(currentQuestId);
	}

	public DMZQuest getNextQuest() {
		DMZQuest currentQuest = getAvailableQuest();
		return currentQuest != null ? DMZStoryRegistry.getQuest(currentQuest.getNextQuestId()) : null;
	}

	public boolean isSagaCompleted(String saga) {
		return DMZStoryRegistry.getTotalQuests(saga) == completedQuests.size();
	}

	public boolean isObjectiveComplete(Component objective, String questId) {
		DMZQuest quest = DMZStoryRegistry.getQuest(questId);
		if (quest == null) return false;

		String key = "";
		QuestRequirement requirement = quest.getRequirement();
		if (objective.getContents() instanceof TranslatableContents translatable) {
			key = translatable.getKey();
		}

		if (key.equals("dmz.storyline.objective.kill_enemy")) {
			for (Map.Entry<String, Integer> entry : requirement.getRequiredKills().entrySet()) {
				String mobName = entry.getKey();
				int requiredCount = entry.getValue();
				int playerCount = entityKillCounts.getOrDefault(mobName, 0);
				if (objective.getString().contains(mobName) && playerCount >= requiredCount) {
					return true;
				}
			}
		}

		if (key.equals("dmz.storyline.objective.get_to_biome")) {
			System.out.println("Checking biome objective: " + biomeFound);
			if (biomeFound) {
				return true;
			}
		}

		if (key.equals("dmz.storyline.objective.collect_item")) {
			for (Map.Entry<String, Integer> entry : requirement.getRequiredItems().entrySet()) {
				String itemName = entry.getKey();
				int requiredCount = entry.getValue();
				int playerCount = itemCollectionCounts.getOrDefault(itemName, 0);
				if (objective.getString().contains(itemName) && playerCount >= requiredCount) {
					return true;
				}
			}
		}

		if (key.equals("dmz.storyline.objective.get_to_location")) {
			if (structureFound) {
				return true;
			}
		}

		return false;
	}


	public CompoundTag saveNBTData() {
		CompoundTag nbt = new CompoundTag();
		nbt.putString("CurrentSaga", currentSaga);
		nbt.putString("CurrentQuest", currentQuestId);

		CompoundTag killsTag = new CompoundTag();
		entityKillCounts.forEach(killsTag::putInt);
		nbt.put("Kills", killsTag);

		CompoundTag itemsTag = new CompoundTag();
		itemCollectionCounts.forEach(itemsTag::putInt);
		nbt.put("Items", itemsTag);

		nbt.putBoolean("StructureFound", structureFound);
		nbt.putBoolean("BiomeFound", biomeFound);
		nbt.putBoolean("HasRequiredItem", hasRequiredItem);

		ListTag completedQuestsTag = new ListTag();
		for (String questId : completedQuests) {
			completedQuestsTag.add(StringTag.valueOf(questId));
		}
		nbt.put("CompletedQuests", completedQuestsTag);

		return nbt;
	}

	public void loadNBTData(CompoundTag tag) {
		currentSaga = tag.getString("CurrentSaga");
		currentQuestId = tag.getString("CurrentQuest");

		CompoundTag killsTag = tag.getCompound("Kills");
		for (String key : killsTag.getAllKeys()) {
			entityKillCounts.put(key, killsTag.getInt(key));
		}

		CompoundTag itemsTag = tag.getCompound("Items");
		itemCollectionCounts.clear();
		for (String key : itemsTag.getAllKeys()) {
			itemCollectionCounts.put(key, itemsTag.getInt(key));
		}

		structureFound = tag.getBoolean("StructureFound");
		biomeFound = tag.getBoolean("BiomeFound");
		hasRequiredItem = tag.getBoolean("HasRequiredItem");

		completedQuests.clear();
		ListTag completedQuestsTag = tag.getList("CompletedQuests", Tag.TAG_STRING);
		for (Tag questTag : completedQuestsTag) {
			completedQuests.add(questTag.getAsString());
		}
	}
}