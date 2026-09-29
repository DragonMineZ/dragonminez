package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Getter
public class MinigameResultS2C {
	public static final int CHALLENGE_NONE = 0;
	public static final int CHALLENGE_STAGE_CLEARED = 1;
	public static final int CHALLENGE_COMPLETED = 2;
	public static final int CHALLENGE_FAILED = 3;

	private final int sessionId;
	private final boolean finalResult;
	private final boolean invalid;
	private final int tpEarned;
	private final int levelsCleared;
	private final int ticksPlayed;
	private final float performance;
	private final float rewardRate;
	private final boolean mentorBonus;
	private final int learnRuns;
	private final int learnRequiredRuns;
	private final boolean learnedNow;
	private final boolean learnRunCounted;
	private final int challengeOutcome;

	public MinigameResultS2C(int sessionId, boolean finalResult, boolean invalid, int tpEarned, int levelsCleared, int ticksPlayed,
							 float performance, float rewardRate, boolean mentorBonus, int learnRuns, int learnRequiredRuns,
							 boolean learnedNow, boolean learnRunCounted, int challengeOutcome) {
		this.sessionId = sessionId;
		this.finalResult = finalResult;
		this.invalid = invalid;
		this.tpEarned = tpEarned;
		this.levelsCleared = levelsCleared;
		this.ticksPlayed = ticksPlayed;
		this.performance = performance;
		this.rewardRate = rewardRate;
		this.mentorBonus = mentorBonus;
		this.learnRuns = learnRuns;
		this.learnRequiredRuns = learnRequiredRuns;
		this.learnedNow = learnedNow;
		this.learnRunCounted = learnRunCounted;
		this.challengeOutcome = challengeOutcome;
	}

	public MinigameResultS2C(FriendlyByteBuf buf) {
		this.sessionId = buf.readVarInt();
		this.finalResult = buf.readBoolean();
		this.invalid = buf.readBoolean();
		this.tpEarned = buf.readVarInt();
		this.levelsCleared = buf.readVarInt();
		this.ticksPlayed = buf.readVarInt();
		this.performance = buf.readFloat();
		this.rewardRate = buf.readFloat();
		this.mentorBonus = buf.readBoolean();
		this.learnRuns = buf.readVarInt();
		this.learnRequiredRuns = buf.readVarInt();
		this.learnedNow = buf.readBoolean();
		this.learnRunCounted = buf.readBoolean();
		this.challengeOutcome = buf.readByte();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(sessionId);
		buf.writeBoolean(finalResult);
		buf.writeBoolean(invalid);
		buf.writeVarInt(Math.max(0, tpEarned));
		buf.writeVarInt(levelsCleared);
		buf.writeVarInt(ticksPlayed);
		buf.writeFloat(performance);
		buf.writeFloat(rewardRate);
		buf.writeBoolean(mentorBonus);
		buf.writeVarInt(learnRuns);
		buf.writeVarInt(learnRequiredRuns);
		buf.writeBoolean(learnedNow);
		buf.writeBoolean(learnRunCounted);
		buf.writeByte(challengeOutcome);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMinigameResult(this)));
		ctx.get().setPacketHandled(true);
	}
}
