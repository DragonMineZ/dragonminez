package com.dragonminez.common.init.entities.questnpc;

import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenQuestNPCDialogueS2C;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.stats.StatsCapability;
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

/**
 * A single, generic, data-driven quest NPC entity.
 * Each instance stores an "npcId" (e.g. "bulma", "farmer_01", "young_goku") in synched entity data + NBT.
 * The model, texture, and animation are resolved dynamically by QuestNPCModel.
 * One entity type registration serves ALL quest NPCs — no need for hundreds of Java classes.
 */
public class QuestNPCEntity extends MastersEntity {

	private static final EntityDataAccessor<String> NPC_ID =
			SynchedEntityData.defineId(QuestNPCEntity.class, EntityDataSerializers.STRING);

	private static final EntityDataAccessor<String> NPC_MODEL =
			SynchedEntityData.defineId(QuestNPCEntity.class, EntityDataSerializers.STRING);

	private static final EntityDataAccessor<String> NPC_TEXTURE =
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
		this.entityData.define(NPC_TEXTURE, "");
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

	public String getNpcTexture() {
		return this.entityData.get(NPC_TEXTURE);
	}

	public void setNpcTexture(String texture) {
		this.entityData.set(NPC_TEXTURE, texture != null ? texture : "");
	}

	/**
	 * Returns the model key used for geo/animation resolution.
	 * If a model override is set, use that; otherwise fall back to npcId.
	 */
	public String getModelKey() {
		String model = getNpcModel();
		return (model != null && !model.isEmpty()) ? model : getNpcId();
	}

	public String getTextureKey() {
		String texture = getNpcTexture();
		return (texture != null && !texture.isEmpty()) ? texture : getNpcId();
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
		String texture = getNpcTexture();
		if (texture != null && !texture.isEmpty()) {
			tag.putString("QuestNpcTexture", texture);
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
		if (tag.contains("QuestNpcTexture")) {
			setNpcTexture(tag.getString("QuestNpcTexture"));
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

				QuestService.NPCQuestOptions options = QuestService.collectNpcQuestOptions(npcId, data);

				// Send dialogue packet to client
				NetworkHandler.sendToPlayer(
						new OpenQuestNPCDialogueS2C(npcId, options.offerableQuestIds(),
								options.turnInQuestIds(), options.inProgressQuestIds(), false, getId()),
						serverPlayer
				);
			});

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.SUCCESS;
	}
}

