package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.*;
import com.dragonminez.common.quest.objectives.*;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class StoryModeEvents {

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
		if (event.player.tickCount % 20 != 0) return;
		if (!(event.player instanceof ServerPlayer player)) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			QuestData questData = data.getQuestData();
			Map<String, Saga> activeSagas = questData.getActiveSagas();

			for (Map.Entry<String, Saga> entry : activeSagas.entrySet()) {
				String sagaId = entry.getKey();
				Saga saga = entry.getValue();

				for (Quest quest : saga.getQuests()) {
					int questId = quest.getId();
					if (questData.isQuestCompleted(sagaId, questId)) continue;
					int currentObjIndex = -1;
					QuestObjective objective = null;

					for (int i = 0; i < quest.getObjectives().size(); i++) {
						QuestObjective tempObj = quest.getObjectives().get(i);
						int currentProgress = questData.getQuestObjectiveProgress(sagaId, questId, i);
						if (currentProgress < tempObj.getRequired()) {
							currentObjIndex = i;
							objective = tempObj;
							break;
						}
					}

					if (objective == null || currentObjIndex == -1) continue;
					boolean isLocationObjective = (objective instanceof BiomeObjective) || (objective instanceof StructureObjective) || (objective instanceof CoordsObjective);

					if (isLocationObjective) {
						List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(player);
						boolean anyMemberInZone = false;
						for (ServerPlayer member : partyMembers) {
							if (checkLocationCondition(member, objective)) {
								anyMemberInZone = true;
								break;
							}
						}
						int targetProgress = anyMemberInZone ? 1 : 0;
						updatePartyState(partyMembers, sagaId, questId, currentObjIndex, targetProgress);
					} else if (objective instanceof ItemObjective itemObjective) {
						int itemCount = countItems(player, itemObjective.getItemId());
						int savedProgress = questData.getQuestObjectiveProgress(sagaId, questId, currentObjIndex);
						if (itemCount != savedProgress) {
							int progressToSet = Math.min(itemCount, itemObjective.getRequired());
							updateIndividualProgress(player, sagaId, questId, currentObjIndex, progressToSet);
						}
					}
				}
			}
		});
	}

	@SubscribeEvent
	public static void onEntityKill(LivingDeathEvent event) {
		if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
		LivingEntity killedEntity = event.getEntity();
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(killer);
		for (ServerPlayer member : partyMembers) {
			StatsProvider.get(StatsCapability.INSTANCE, member).ifPresent(data -> {
				QuestData qd = data.getQuestData();
				Map<String, Saga> activeSagas = qd.getActiveSagas();

				for (Map.Entry<String, Saga> entry : activeSagas.entrySet()) {
					String sagaId = entry.getKey();
					Saga saga = entry.getValue();

					for (Quest quest : saga.getQuests()) {
						int questId = quest.getId();
						if (qd.isQuestCompleted(sagaId, questId)) continue;

						for (int i = 0; i < quest.getObjectives().size(); i++) {
							QuestObjective objective = quest.getObjectives().get(i);
							int currentProgress = qd.getQuestObjectiveProgress(sagaId, questId, i);
							if (currentProgress >= objective.getRequired()) continue;

							if (objective instanceof KillObjective killObjective) {
								ResourceLocation targetId = new ResourceLocation(killObjective.getEntityId());
								EntityType<?> requiredType = BuiltInRegistries.ENTITY_TYPE.get(targetId);
								if (killedEntity.getType().equals(requiredType)) updateIndividualProgress(member, sagaId, questId, i, currentProgress + 1);
							}
						}
					}
				}
			});
		}
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getEntity() instanceof ServerPlayer interactor)) return;
		if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(interactor);

		for (ServerPlayer member : partyMembers) {
			StatsProvider.get(StatsCapability.INSTANCE, member).ifPresent(data -> {
				QuestData qd = data.getQuestData();
				Map<String, Saga> activeSagas = qd.getActiveSagas();

				for (Map.Entry<String, Saga> entry : activeSagas.entrySet()) {
					String sagaId = entry.getKey();
					Saga saga = entry.getValue();

					for (Quest quest : saga.getQuests()) {
						int questId = quest.getId();
						if (qd.isQuestCompleted(sagaId, questId)) continue;

						for (int i = 0; i < quest.getObjectives().size(); i++) {
							QuestObjective objective = quest.getObjectives().get(i);
							int currentProgress = qd.getQuestObjectiveProgress(sagaId, questId, i);
							if (currentProgress >= objective.getRequired()) continue;
							if (objective instanceof InteractObjective interactObjective) {
								String targetStr = interactObjective.getEntityTypeId();
								EntityType<?> requiredType = targetStr != null ? BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(targetStr)) : null;
								if (requiredType == null || event.getTarget().getType().equals(requiredType)) updateIndividualProgress(member, sagaId, questId, i, currentProgress + 1);
							}
						}
					}
				}
			});
		}
	}

	private static boolean checkLocationCondition(ServerPlayer player, QuestObjective objective) {
		BlockPos pos = player.blockPosition();
		ServerLevel level = player.serverLevel();

		if (objective instanceof BiomeObjective biomeObj) {
			try {
				ResourceKey<Biome> key = level.getBiome(pos).unwrapKey().orElse(null);
				if (key != null) {
					String target = biomeObj.getBiomeId();
					if (!target.contains(":")) target = "minecraft:" + target;
					return key.location().equals(new ResourceLocation(target));
				}
			} catch (Exception ignored) { return false; }
		}
		else if (objective instanceof StructureObjective structObj) {
			try {
				String target = structObj.getStructureId();
				if (!target.contains(":")) target = "minecraft:" + target;
				ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(target));
				return level.structureManager().getStructureWithPieceAt(pos, key).isValid();
			} catch (Exception ignored) { return false; }
		}
		else if (objective instanceof CoordsObjective coordsObj) {
			double distSq = pos.distSqr(coordsObj.getTargetPos());
			double radiusSq = coordsObj.getRadius() * coordsObj.getRadius();
			return distSq <= radiusSq;
		}
		return false;
	}

	private static int countItems(ServerPlayer player, String itemId) {
		try {
			Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
			int count = 0;
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (stack.getItem() == item) count += stack.getCount();
			}
			return count;
		} catch (Exception e) {
			return 0;
		}
	}

	private static void updatePartyState(List<ServerPlayer> members, String sagaId, int questId, int objIndex, int newProgress) {
		for (ServerPlayer member : members) updateIndividualProgress(member, sagaId, questId, objIndex, newProgress);
	}

	private static void updateIndividualProgress(ServerPlayer player, String sagaId, int questId, int objIndex, int newProgress) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			int current = data.getQuestData().getQuestObjectiveProgress(sagaId, questId, objIndex);
			if (current != newProgress) {
				data.getQuestData().setQuestObjectiveProgress(sagaId, questId, objIndex, newProgress);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			}
		});
	}
}