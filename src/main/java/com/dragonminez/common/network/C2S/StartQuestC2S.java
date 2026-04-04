package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.QuestAvailabilityChecker;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.quest.SagaBranchingHelper;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class StartQuestC2S {
	private final String sagaId;
	private final int questId;
	private final boolean isHardMode;

	public StartQuestC2S(String sagaId, int questId, boolean isHardMode) {
		this.sagaId = sagaId;
		this.questId = questId;
		this.isHardMode = isHardMode;
	}

	public StartQuestC2S(FriendlyByteBuf buffer) {
		this.sagaId = buffer.readUtf();
		this.questId = buffer.readInt();
		this.isHardMode = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(sagaId);
		buffer.writeInt(questId);
		buffer.writeBoolean(isHardMode);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			Saga saga = QuestRegistry.getSaga(sagaId);
			if (saga == null) {
				notifyStartFailure(player, "message.dragonminez.quest.start.unavailable");
				return;
			}
			Quest quest = saga.getQuestById(questId);
			if (quest == null) {
				notifyStartFailure(player, "message.dragonminez.quest.start.unavailable");
				return;
			}

			ServerPlayer controller = PartyManager.resolveQuestController(player);
			if (controller == null) {
				notifyStartFailure(player, "message.dragonminez.quest.start.unavailable");
				return;
			}

			StatsProvider.get(StatsCapability.INSTANCE, controller).ifPresent(data -> {
				PlayerQuestData pqd = data.getPlayerQuestData();
				String questKey = PlayerQuestData.sagaQuestKey(sagaId, questId);
				if (pqd.isQuestCompleted(questKey)
						|| pqd.getQuestStatus(questKey) == PlayerQuestData.QuestStatus.ACCEPTED) {
					notifyStartFailure(player, "message.dragonminez.quest.start.already_active");
					return;
				}

				int questIndex = saga.getQuests().indexOf(quest);
				if (questIndex < 0) {
					notifyStartFailure(player, "message.dragonminez.quest.start.unavailable");
					return;
				}
				if (!SagaBranchingHelper.isSagaQuestAvailable(quest, saga, questIndex, data)) {
					Component reason = QuestAvailabilityChecker.describeAvailabilityFailure(quest, data);
					notifyStartFailure(player, reason != null ? reason : Component.translatable("message.dragonminez.quest.start.locked"));
					return;
				}
				Component controllerBlocker = QuestAvailabilityChecker.describeQuestStartBlocker(quest, questKey, controller, data);
				if (controllerBlocker != null) {
					if (player.getUUID().equals(controller.getUUID())) {
						notifyStartFailure(player, controllerBlocker);
					} else {
						notifyStartFailure(player, Component.translatable(
								"message.dragonminez.quest.start.party_member_requirement",
								controller.getGameProfile().getName(),
								controllerBlocker
						));
					}
					return;
				}

				Component partyBlocker = getPartyRequirementFailure(player, controller, quest, questKey);
				if (partyBlocker != null) {
					notifyStartFailure(player, partyBlocker);
					return;
				}

				SagaBranchingHelper.selectBranchIfNeeded(pqd, sagaId, quest);
				pqd.acceptQuest(questKey);
				pqd.setTrackedQuestId(questKey);
				NetworkHandler.sendToPlayer(StoryToastS2C.questStarted(questKey), controller);

				for (int i = 0; i < quest.getObjectives().size(); i++) {
					QuestObjective objective = quest.getObjectives().get(i);

					if (objective instanceof KillObjective killObjective) {
						int currentProgress = pqd.getObjectiveProgress(questKey, i);
						int required = killObjective.getRequired();
						int remaining = Math.max(0, required - currentProgress);

						if (remaining <= 0) continue;

						String entityIdStr = killObjective.getEntityId();
						if (entityIdStr.equals("dragonminez:saga_zarbont1")) entityIdStr = "dragonminez:saga_zarbon";
						if (entityIdStr.equals("dragonminez:saga_frieza_third"))
							entityIdStr = "dragonminez:saga_frieza_second";
						ResourceLocation resLoc = ResourceLocation.parse(entityIdStr);
						EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(resLoc);

						if (entityType != null) {
							for (int j = 0; j < remaining; j++) {
								Entity entity = entityType.create(player.level());
								if (entity != null) {
									double offsetX = (Math.random() - 0.5) * 8.0;
									double offsetZ = (Math.random() - 0.5) * 8.0;
									entity.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);

									if (isHardMode) entity.getPersistentData().putBoolean("dmz_is_hardmode", true);

									entity.getPersistentData().putString("dmz_saga_id", sagaId);
									entity.getPersistentData().putString("dmz_quest_owner", player.getStringUUID());

									entity.getPersistentData().putDouble("dmz_quest_hp", killObjective.getHealth());
									entity.getPersistentData().putDouble("dmz_quest_melee", killObjective.getMeleeDamage());
									entity.getPersistentData().putDouble("dmz_quest_ki", killObjective.getKiDamage());

									if (entity instanceof Mob mob) mob.setTarget(player);
									player.serverLevel().addFreshEntity(entity);
								}
							}
						}
					}
				}

				if (PartyManager.isInParty(controller)) {
					PartyManager.syncPartyQuestState(controller);
					for (ServerPlayer member : PartyManager.getAllPartyMembers(controller)) {
						if (!member.getUUID().equals(controller.getUUID())) {
							NetworkHandler.sendToPlayer(StoryToastS2C.questStarted(questKey), member);
						}
					}
				} else {
					NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(controller), controller);
				}
			});
		});
		context.setPacketHandled(true);
	}

	private static Component getPartyRequirementFailure(ServerPlayer requester, ServerPlayer controller, Quest quest, String questKey) {
		if (!PartyManager.isInParty(controller)) {
			return null;
		}

		for (ServerPlayer member : PartyManager.getAllPartyMembers(controller)) {
			Component blocker = StatsProvider.get(StatsCapability.INSTANCE, member)
					.map(data -> QuestAvailabilityChecker.describeQuestStartBlocker(quest, questKey, member, data))
					.orElse(Component.translatable("message.dragonminez.quest.start.unavailable"));
			if (blocker == null) {
				continue;
			}

			if (member.getUUID().equals(requester.getUUID())) {
				return blocker;
			}

			return Component.translatable(
					"message.dragonminez.quest.start.party_member_requirement",
					member.getGameProfile().getName(),
					blocker
			);
		}

		return null;
	}

	private static void notifyStartFailure(ServerPlayer player, String translationKey) {
		notifyStartFailure(player, Component.translatable(translationKey));
	}

	private static void notifyStartFailure(ServerPlayer player, Component message) {
		player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
	}
}
