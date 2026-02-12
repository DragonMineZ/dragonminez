package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.quest.SagaManager;
import com.dragonminez.common.quest.objectives.KillObjective;
import net.minecraft.network.FriendlyByteBuf;
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
			Saga saga = SagaManager.getSaga(sagaId);
			if (saga == null) return;
			Quest quest = saga.getQuestById(questId);
			if (quest == null) return;

			for (QuestObjective objective : quest.getObjectives()) {
				if (objective instanceof KillObjective killObjective) {
					String entityIdStr = killObjective.getEntityId();
					if (entityIdStr.equals("dragonminez:saga_zarbont1")) entityIdStr = "dragonminez:saga_zarbon";
					if (entityIdStr.equals("dragonminez:saga_frieza_third")) entityIdStr = "dragonminez:saga_frieza_second";
					ResourceLocation resLoc = ResourceLocation.parse(entityIdStr);
					EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(resLoc);

					if (entityType != null) {
						Entity entity = entityType.create(player.level());
						if (entity != null) {
							entity.setPos(player.getX(), player.getY(), player.getZ());
							if (isHardMode) entity.getPersistentData().putBoolean("dmz_is_hardmode", true);
							if (entity instanceof Mob mob) mob.setTarget(player);
							player.serverLevel().addFreshEntity(entity);
							break;
						}
					}
				}
			}
		});
		context.setPacketHandled(true);
	}
}