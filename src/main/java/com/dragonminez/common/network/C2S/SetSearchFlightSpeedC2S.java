package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.FlightSpeedLimit;
import com.dragonminez.common.stats.skills.Skill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetSearchFlightSpeedC2S {
	private final int value;
	private final boolean relative;

	public SetSearchFlightSpeedC2S(int value) {
		this(value, false);
	}

	public SetSearchFlightSpeedC2S(int value, boolean relative) {
		this.value = value;
		this.relative = relative;
	}

	public SetSearchFlightSpeedC2S(FriendlyByteBuf buf) {
		this.value = buf.readInt();
		this.relative = buf.readBoolean();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(value);
		buf.writeBoolean(relative);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()) return;
				Skill fly = data.getSkills().getSkill("fly");
				if (fly == null || fly.getLevel() <= 0 || (relative && value != -5 && value != 5)) return;

				int target = relative ? data.getStatus().getSearchFlightSpeedLimit() + value : value;
				data.getStatus().setSearchFlightSpeedLimit(FlightSpeedLimit.clampPercent(target));
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}
