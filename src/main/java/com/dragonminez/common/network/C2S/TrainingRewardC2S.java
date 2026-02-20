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

	public enum TrainStat {
		NONE(""), STR("STR"), SKP("SKP"), RES("RES"), VIT("VIT"), PWR("PWR"), ENE("ENE");

		private final String statKey;

		TrainStat(String statKey) {
			this.statKey = statKey;
		}

		public String getStatKey() {
			return statKey;
		}
	}

	private final TrainStat stat;
	private final int points;

	public TrainingRewardC2S(TrainStat stat, int points) {
		this.stat = stat;
		this.points = points;
	}

	public TrainingRewardC2S(FriendlyByteBuf buf) {
		this.stat = buf.readEnum(TrainStat.class);
		this.points = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeEnum(stat);
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
						statsData.getTraining().setCurrentTrainingStat(stat.getStatKey());
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
						return;
					}
					if (statsData.getTraining().canTrain(stat.getStatKey())) {
						statsData.getStats().addStat(stat.getStatKey(), points);
						statsData.getTraining().addTrainingPoints(stat.getStatKey(), points);
						player.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.0F);
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
					}
				});
			}
		});
		ctx.get().setPacketHandled(true);
	}
}