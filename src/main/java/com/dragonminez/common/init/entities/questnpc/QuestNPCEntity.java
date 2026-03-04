package com.dragonminez.common.init.entities.questnpc;

import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenQuestNPCDialogueS2C;
import com.dragonminez.common.quest.sidequest.SideQuest;
import com.dragonminez.common.quest.sidequest.SideQuestData;
import com.dragonminez.common.quest.sidequest.SideQuestManager;
import com.dragonminez.common.quest.sidequest.QuestAvailabilityChecker;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A single, generic, data-driven quest NPC entity.
 * Each instance stores an "npcId" (e.g. "bulma", "farmer_01", "young_goku") in synched entity data + NBT.
 * The model, texture, and animation are resolved dynamically from the npcId by QuestNPCModel.
 * One entity type registration serves ALL quest NPCs — no need for hundreds of Java classes.
 */
public class QuestNPCEntity extends MastersEntity {

	private static final EntityDataAccessor<String> NPC_ID =
			SynchedEntityData.defineId(QuestNPCEntity.class, EntityDataSerializers.STRING);

	private static final EntityDataAccessor<String> NPC_MODEL =
			SynchedEntityData.defineId(QuestNPCEntity.class, EntityDataSerializers.STRING);

	public QuestNPCEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
		this.setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(NPC_ID, "generic_npc");
		this.entityData.define(NPC_MODEL, "");
	}

	// ---- NPC identity ----

	public String getNpcId() {
		return this.entityData.get(NPC_ID);
	}

	public void setNpcId(String npcId) {
		this.entityData.set(NPC_ID, npcId);
	}

	/**
	 * Optional model override. If set, the renderer uses this instead of npcId for geo/animation resolution.
	 * This allows many NPCs to share the same base model with different textures.
	 * E.g. model="humanoid_male" but npcId="farmer_01" → uses farmer_01.png texture on humanoid_male.geo.json
	 */
	public String getNpcModel() {
		return this.entityData.get(NPC_MODEL);
	}

	public void setNpcModel(String model) {
		this.entityData.set(NPC_MODEL, model != null ? model : "");
	}

	/**
	 * Returns the model key used for geo/animation resolution.
	 * If a model override is set, use that; otherwise fall back to npcId.
	 */
	public String getModelKey() {
		String model = getNpcModel();
		return (model != null && !model.isEmpty()) ? model : getNpcId();
	}

	// ---- Display name ----

	@Override
	public @NonNull Component getName() {
		return Component.translatable("entity.dragonminez.questnpc." + getNpcId());
	}

	@Override
	public @NonNull Component getDisplayName() {
		return getName();
	}

	// ---- NBT persistence ----

	@Override
	public void addAdditionalSaveData(@NonNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString("QuestNpcId", getNpcId());
		String model = getNpcModel();
		if (model != null && !model.isEmpty()) {
			tag.putString("QuestNpcModel", model);
		}
	}

	@Override
	public void readAdditionalSaveData(@NonNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("QuestNpcId")) {
			setNpcId(tag.getString("QuestNpcId"));
		}
		if (tag.contains("QuestNpcModel")) {
			setNpcModel(tag.getString("QuestNpcModel"));
		}
	}

	// ---- Interaction: open quest dialogue ----

	@Override
	protected @NonNull InteractionResult mobInteract(@NonNull Player pPlayer, @NonNull InteractionHand pHand) {
		if (!this.level().isClientSide && pPlayer instanceof ServerPlayer serverPlayer) {
			String npcId = getNpcId();

			StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) {
					serverPlayer.displayClientMessage(
							Component.translatable("gui.dragonminez.lines.generic.createcharacter"), true);
					return;
				}

				// Gather quests this NPC can offer/turn-in
				List<String> offerableQuestIds = new ArrayList<>();
				List<String> turnInQuestIds = new ArrayList<>();
				List<String> inProgressQuestIds = new ArrayList<>();

				collectNPCQuests(npcId, data, offerableQuestIds, turnInQuestIds, inProgressQuestIds);

				// Send dialogue packet to client
				NetworkHandler.sendToPlayer(
						new OpenQuestNPCDialogueS2C(npcId, offerableQuestIds, turnInQuestIds, inProgressQuestIds),
						serverPlayer
				);
			});

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.SUCCESS;
	}

	/**
	 * Collects quest IDs relevant to this NPC for the given player.
	 */
	private static void collectNPCQuests(String npcId, StatsData data,
										 List<String> offerable, List<String> turnIn, List<String> inProgress) {
		SideQuestData sqData = data.getSideQuestData();
		Map<String, SideQuest> allQuests = SideQuestManager.getAllSideQuests();

		List<String> giverQuestIds = SideQuestManager.getQuestIdsByGiver(npcId);

		// Quests this NPC gives
		for (String questId : giverQuestIds) {
			SideQuest quest = allQuests.get(questId);
			if (quest == null) continue;

			if (sqData.isQuestCompleted(questId)) continue;

			if (sqData.isQuestAccepted(questId)) {
				inProgress.add(questId);
			} else if (QuestAvailabilityChecker.isAvailable(quest, data)) {
				offerable.add(questId);
			}
		}

		// Quests where this NPC is the turn-in target
		List<String> turnInQuestIds = SideQuestManager.getQuestIdsByTurnIn(npcId);
		for (String questId : turnInQuestIds) {
			SideQuest quest = allQuests.get(questId);
			if (quest == null) continue;

			if (!sqData.isQuestAccepted(questId)) continue;
			if (sqData.isQuestCompleted(questId)) continue;

			// Check if all non-TALK_TO objectives are complete
			boolean allNonTalkDone = true;
			for (int i = 0; i < quest.getObjectives().size(); i++) {
				var obj = quest.getObjectives().get(i);
				if (obj.getType() == com.dragonminez.common.quest.QuestObjective.ObjectiveType.TALK_TO) continue;
				int progress = sqData.getObjectiveProgress(questId, i);
				if (progress < obj.getRequired()) {
					allNonTalkDone = false;
					break;
				}
			}

			if (allNonTalkDone && !turnIn.contains(questId)) {
				turnIn.add(questId);
			}
		}
	}
}


