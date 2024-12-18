package com.yuseix.dragonminez.storyline;

import com.yuseix.dragonminez.storyline.missions.Quest;
import com.yuseix.dragonminez.storyline.sagas.Saga;
import com.yuseix.dragonminez.storyline.sagas.SaiyanSaga;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Hashtable;

public class StorylineManager {
	private final Hashtable<String, Saga> sagas = new Hashtable<>();

	public StorylineManager() {
		initializeSagas();
	}

	// Initialize predefined sagas
	public void initializeSagas() {
		addSaga(SaiyanSaga.createSaiyanSaga());
		// Add more sagas as needed
	}

	// Add a saga to the manager
	public void addSaga(Saga saga) {
		sagas.put(saga.getId(), saga);
	}

	// Retrieve a saga by ID
	public Saga getSaga(String id) {
		return sagas.get(id);
	}

	public void resetProgress() {
		for (Saga saga : sagas.values()) {
			for (Quest quest : saga.getQuests()) {
				quest.setCompleted(false);
			}
		}
	}

	// No se usa porque ya hay otro que saca directamente la ID de la quest sin necesidad de la saga
	// A futuro usar esto para la posibilidad de que una quest tenga misma id en distintas sagas
//	public void completeQuest(String sagaId, String questId) {
//		Saga saga = sagas.get(sagaId);
//		if (saga != null) {
//			for (Quest quest : saga.getQuests()) {
//				if (quest.getId().equals(questId)) {
//					quest.completeQuest();
//					break;
//				}
//			}
//		}
//	}

	// CHeca si la saga esta completa
	public boolean isSagaCompleted(String sagaId) {
		Saga saga = sagas.get(sagaId);
		return saga != null && saga.isCompleted();
	}

	// Mostrar todas las sagas
	public Hashtable<String, Saga> getAllSagas() {
		return sagas;
	}

	public CompoundTag saveNBTData(CompoundTag nbt) {
		CompoundTag sagasTag = new CompoundTag(); // Main container for all sagas

		for (Saga saga : sagas.values()) {
			CompoundTag sagaTag = new CompoundTag();
			sagaTag.putString("id", saga.getId());
			sagaTag.putBoolean("completed", saga.isCompleted());

			// Save quests in the saga
			ListTag questsTag = new ListTag(); // List for storing quests
			for (Quest quest : saga.getQuests()) {
				CompoundTag questTag = new CompoundTag();
				questTag.putString("id", quest.getId());
				questTag.putBoolean("completed", quest.isCompleted());
				questsTag.add(questTag);
			}
			sagaTag.put("quests", questsTag); // Add quests to saga
			sagasTag.put(saga.getId(), sagaTag); // Add saga to main sagas container
		}

		nbt.put("sagas", sagasTag); // Add all sagas to the provided NBT

		return nbt;
	}


	public void loadNBTData(CompoundTag nbt) {
		if (!nbt.contains("sagas")) return; // Ensure sagas data exists

		CompoundTag sagasTag = nbt.getCompound("sagas");

		for (String sagaKey : sagasTag.getAllKeys()) {
			CompoundTag sagaTag = sagasTag.getCompound(sagaKey);
			String sagaId = sagaTag.getString("id");

			//Start all sagas for data loading
			initializeSagas();

			// Get the existing Saga by ID
			Saga saga = getSaga(sagaId);

			if (saga == null) {
				throw new IllegalArgumentException("Saga with ID '" + sagaId + "' does not exist!");
			}

			// Update quests
			if (sagaTag.contains("quests")) {
				ListTag questsTag = sagaTag.getList("quests", 10); // 10 = CompoundTag type
				for (int i = 0; i < questsTag.size(); i++) {
					CompoundTag questTag = questsTag.getCompound(i);
					String questId = questTag.getString("id");
					boolean questCompleted = questTag.getBoolean("completed");

					// Get the existing Quest by ID
					for (Quest quest : saga.getQuests()) {
						if (quest.getId().equals(questId)) {
							quest.setCompleted(questCompleted); // Update the completed state
							break;
						}
					}
				}
			}
		}
	}
}
