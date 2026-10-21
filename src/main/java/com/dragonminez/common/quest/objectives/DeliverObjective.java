package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;
import net.minecraft.world.item.Item;

@Getter
public class DeliverObjective extends QuestObjective {

	private final Item item;
	private final String itemId;
	private final String npcId;

	public DeliverObjective(Item item, String itemId, int count, String npcId) {
		super(ObjectiveType.DELIVER, count);
		this.item = item;
		this.itemId = itemId;
		this.npcId = npcId;
	}

	public boolean matchesNpc(String interactedNpcId) {
		return npcId != null && npcId.equalsIgnoreCase(interactedNpcId);
	}

	@Override
	public boolean checkProgress(Object... params) {
		return isCompleted();
	}
}
