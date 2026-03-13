package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.quest.QuestAvailabilityChecker;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class AcceptSideQuestC2S {
	private final String sideQuestId;
	private final boolean isHardMode;

	public AcceptSideQuestC2S(String sideQuestId, boolean isHardMode) {
		this.sideQuestId = sideQuestId;
		this.isHardMode = isHardMode;
	}

	public AcceptSideQuestC2S(FriendlyByteBuf buffer) {
		this.sideQuestId = buffer.readUtf();
		this.isHardMode = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(sideQuestId);
		buffer.writeBoolean(isHardMode);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			Quest sideQuest = QuestRegistry.getQuest(sideQuestId);
			if (sideQuest == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				PlayerQuestData pqd = data.getPlayerQuestData();
				if (pqd.isQuestAccepted(sideQuestId)) return;
				if (!QuestAvailabilityChecker.isAvailable(sideQuest, data)) return;
				pqd.acceptQuest(sideQuestId);

				for (int i = 0; i < sideQuest.getObjectives().size(); i++) {
					QuestObjective objective = sideQuest.getObjectives().get(i);

					if (objective instanceof KillObjective killObjective) {
						int currentProgress = pqd.getObjectiveProgress(sideQuestId, i);
						int required = killObjective.getRequired();
						int remaining = Math.max(0, required - currentProgress);

						if (remaining <= 0) continue;

						String entityIdStr = killObjective.getEntityId();
						ResourceLocation resLoc = ResourceLocation.parse(entityIdStr);
						EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(resLoc);

						if (entityType != null) {
							for (int j = 0; j < remaining; j++) {
								Entity entity = entityType.create(player.level());
								if (entity != null) {
									double offsetX = (Math.random() - 0.5) * 2.0;
									double offsetZ = (Math.random() - 0.5) * 2.0;
									entity.setPos(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);

									if (isHardMode) entity.getPersistentData().putBoolean("dmz_is_hardmode", true);

									entity.getPersistentData().putString("dmz_sidequest_id", sideQuestId);
									entity.getPersistentData().putString("dmz_quest_owner", player.getStringUUID());

									if (entity instanceof Mob mob) mob.setTarget(player);
									player.serverLevel().addFreshEntity(entity);
								}
							}
						}
					}
				}

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}

