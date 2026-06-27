package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TrainingRewardC2S {
	private final String minigameId;
	private final int levelsCleared;

	public TrainingRewardC2S(String minigameId, int levelsCleared) {
		this.minigameId = minigameId;
		this.levelsCleared = levelsCleared;
	}

	public TrainingRewardC2S(FriendlyByteBuf buf) {
		this.minigameId = buf.readUtf(32);
		this.levelsCleared = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(minigameId, 32);
		buf.writeInt(levelsCleared);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null || levelsCleared <= 0) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(statsData -> {
				TrainingConfig config = ConfigManager.getTrainingConfig();
				TrainingConfig.MinigameSettings settings = config.getSettings(minigameId);

				int currentTpc = statsData.getSingleStatCost(statsData.getStats().getTotalStats());
				float tpsPerLevel = config.computeTpsPerLevel(currentTpc, settings);

				float totalReward = tpsPerLevel * levelsCleared;
				float limit = settings.getTpsLimitPerGame();
				if (limit > 0 && totalReward > limit) totalReward = limit;
				if (totalReward <= 0) return;

				statsData.getResources().addTrainingPoints(totalReward);
				if (!statsData.getCharacter().getKnownMinigames().contains(minigameId)) statsData.getCharacter().addKnownMinigame(minigameId);


				player.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.0F);
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}