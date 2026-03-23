package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.CompressionUtil;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.sidequest.SideQuest;
import com.dragonminez.common.quest.sidequest.SideQuestManager;
import com.dragonminez.common.util.QuestObjectiveTypeAdapter;
import com.dragonminez.common.util.QuestRewardTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;

public class SyncSideQuestsS2C {

	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(QuestObjective.class, new QuestObjectiveTypeAdapter())
			.registerTypeAdapter(QuestReward.class, new QuestRewardTypeAdapter())
			.create();

	private final byte[] compressedData;

	public SyncSideQuestsS2C(Map<String, SideQuest> sideQuests) {
		this.compressedData = CompressionUtil.compress(GSON.toJson(sideQuests));
	}

	public SyncSideQuestsS2C(FriendlyByteBuf buf) {
		this.compressedData = buf.readByteArray();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeByteArray(compressedData);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
				String decompressedJson = CompressionUtil.decompress(compressedData);
				Type mapType = new TypeToken<Map<String, SideQuest>>(){}.getType();
				Map<String, SideQuest> sideQuests = GSON.fromJson(decompressedJson, mapType);
				if (sideQuests != null) SideQuestManager.applySyncedSideQuests(sideQuests);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}

