package com.dragonminez.common.network.S2C;

import com.dragonminez.client.gui.story.StoryNotificationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StoryToastS2C {

	public enum ToastEventType {
		INTRO_HINT,
		QUEST_STARTED,
		OBJECTIVE_COMPLETE,
		QUEST_COMPLETE
	}

	private final ToastEventType eventType;
	private final String questId;
	private final int objectiveIndex;
	private final int objectiveProgress;
	private final int objectiveRequired;

	public StoryToastS2C(ToastEventType eventType, String questId, int objectiveIndex, int objectiveProgress, int objectiveRequired) {
		this.eventType = eventType;
		this.questId = (questId == null || questId.isBlank()) ? "" : questId;
		this.objectiveIndex = objectiveIndex;
		this.objectiveProgress = objectiveProgress;
		this.objectiveRequired = objectiveRequired;
	}

	public static StoryToastS2C introHint() {
		return new StoryToastS2C(ToastEventType.INTRO_HINT, "", -1, -1, -1);
	}

	public static StoryToastS2C questStarted(String questId) {
		return new StoryToastS2C(ToastEventType.QUEST_STARTED, questId, -1, -1, -1);
	}

	public static StoryToastS2C objectiveComplete(String questId, int objectiveIndex, int objectiveProgress, int objectiveRequired) {
		return new StoryToastS2C(ToastEventType.OBJECTIVE_COMPLETE, questId, objectiveIndex, objectiveProgress, objectiveRequired);
	}

	public static StoryToastS2C questComplete(String questId) {
		return new StoryToastS2C(ToastEventType.QUEST_COMPLETE, questId, -1, -1, -1);
	}

	public ToastEventType getEventType() {
		return eventType;
	}

	public String getQuestId() {
		return questId;
	}

	public int getObjectiveIndex() {
		return objectiveIndex;
	}

	public int getObjectiveProgress() {
		return objectiveProgress;
	}

	public int getObjectiveRequired() {
		return objectiveRequired;
	}

	public static void encode(StoryToastS2C msg, FriendlyByteBuf buf) {
		buf.writeEnum(msg.eventType);
		buf.writeUtf(msg.questId);
		buf.writeInt(msg.objectiveIndex);
		buf.writeInt(msg.objectiveProgress);
		buf.writeInt(msg.objectiveRequired);
	}

	public static StoryToastS2C decode(FriendlyByteBuf buf) {
		return new StoryToastS2C(
				buf.readEnum(ToastEventType.class),
				buf.readUtf(),
				buf.readInt(),
				buf.readInt(),
				buf.readInt()
		);
	}

	public static void handle(StoryToastS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> StoryNotificationManager.push(msg)));
		ctx.get().setPacketHandled(true);
	}
}


