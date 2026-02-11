package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TrainingRewardC2S {
	private final String stat;
	private final int points;

	public TrainingRewardC2S(String stat, int points) {
		this.stat = stat;
		this.points = points;
	}

	public TrainingRewardC2S(FriendlyByteBuf buf) {
		this.stat = buf.readUtf();
		this.points = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(stat);
		buf.writeInt(points);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(statsData -> {
					if (points == -1) {
						statsData.getTraining().setCurrentTrainingStat("");
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
						return;
					}
					if (points == 0) {
						statsData.getTraining().setCurrentTrainingStat(stat);
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
						return;
					}
					if (statsData.getTraining().canTrain(stat)) {
						statsData.getStats().addStat(stat, points);
						statsData.getTraining().addTrainingPoints(stat, points);
						player.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.0F);
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
					}
				});
			}
		});
		ctx.get().setPacketHandled(true);
	}
}