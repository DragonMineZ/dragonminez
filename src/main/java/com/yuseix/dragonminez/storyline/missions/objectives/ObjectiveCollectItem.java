package com.yuseix.dragonminez.storyline.missions.objectives;

import com.yuseix.dragonminez.storyline.missions.Objective;
import net.minecraft.world.item.Item;


public class ObjectiveCollectItem extends Objective {
	private final Item itemId;
	private final int requiredAmount;
	private int currentAmount;

	public ObjectiveCollectItem(Item itemId, int requiredAmount) {

		super(false);

		this.itemId = itemId;
		this.requiredAmount = requiredAmount;
		this.currentAmount = 0;
	}

	public void onItemCollected(Item collectedItem) {

		if (collectedItem.equals(itemId)) {
			currentAmount++;
			checkCompletion();
		}
	}


	public int getCurrentAmount() {
		return currentAmount;
	}

	public int getRequiredAmount() {
		return requiredAmount;
	}

	@Override
	public void checkCompletion() {
		if (currentAmount >= requiredAmount) {
			setCompleted(); // Mark the objective as complete
		}
	}
}


