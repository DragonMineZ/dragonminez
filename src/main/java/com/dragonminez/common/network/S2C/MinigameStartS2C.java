package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import com.dragonminez.common.training.MinigameOrigin;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Getter
public class MinigameStartS2C {
	private final boolean accepted;
	private final int sessionId;
	private final long seed;
	private final String minigameId;
	private final MinigameOrigin origin;
	private final int challengeStage;
	private final int targetLevels;
	private final boolean mentorBonus;
	private final boolean known;
	private final int learnRuns;
	private final int learnRequiredRuns;
	private final int learnRequiredLevel;
	private final String rejectKey;
	private final String option;
	private final byte[] payload;

	public MinigameStartS2C(boolean accepted, int sessionId, long seed, String minigameId, MinigameOrigin origin, int challengeStage,
							int targetLevels, boolean mentorBonus, boolean known, int learnRuns, int learnRequiredRuns,
							int learnRequiredLevel, String rejectKey, String option, byte[] payload) {
		this.accepted = accepted;
		this.sessionId = sessionId;
		this.seed = seed;
		this.minigameId = minigameId;
		this.origin = origin;
		this.challengeStage = challengeStage;
		this.targetLevels = targetLevels;
		this.mentorBonus = mentorBonus;
		this.known = known;
		this.learnRuns = learnRuns;
		this.learnRequiredRuns = learnRequiredRuns;
		this.learnRequiredLevel = learnRequiredLevel;
		this.rejectKey = rejectKey == null ? "" : rejectKey;
		this.option = option == null ? "" : option;
		this.payload = payload == null ? new byte[0] : payload;
	}

	public static MinigameStartS2C rejected(String minigameId, MinigameOrigin origin, int challengeStage, String rejectKey) {
		return new MinigameStartS2C(false, -1, 0L, minigameId, origin, challengeStage, -1, false, false, 0, 0, 0, rejectKey, "", null);
	}

	public MinigameStartS2C(FriendlyByteBuf buf) {
		this.accepted = buf.readBoolean();
		this.sessionId = buf.readVarInt();
		this.seed = buf.readLong();
		this.minigameId = buf.readUtf(32);
		this.origin = MinigameOrigin.byId(buf.readByte());
		this.challengeStage = buf.readVarInt();
		this.targetLevels = buf.readInt();
		this.mentorBonus = buf.readBoolean();
		this.known = buf.readBoolean();
		this.learnRuns = buf.readVarInt();
		this.learnRequiredRuns = buf.readVarInt();
		this.learnRequiredLevel = buf.readVarInt();
		this.rejectKey = buf.readUtf(128);
		this.option = buf.readUtf(128);
		this.payload = buf.readByteArray(1 << 18);
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(accepted);
		buf.writeVarInt(sessionId);
		buf.writeLong(seed);
		buf.writeUtf(minigameId, 32);
		buf.writeByte(origin.ordinal());
		buf.writeVarInt(challengeStage);
		buf.writeInt(targetLevels);
		buf.writeBoolean(mentorBonus);
		buf.writeBoolean(known);
		buf.writeVarInt(learnRuns);
		buf.writeVarInt(learnRequiredRuns);
		buf.writeVarInt(learnRequiredLevel);
		buf.writeUtf(rejectKey, 128);
		buf.writeUtf(option, 128);
		buf.writeByteArray(payload);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMinigameStart(this)));
		ctx.get().setPacketHandled(true);
	}
}
