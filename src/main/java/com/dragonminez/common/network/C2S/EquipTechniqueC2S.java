package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EquipTechniqueC2S {
	private final int slotIndex;
	private final String techniqueId;

	public EquipTechniqueC2S(int slotIndex, String techniqueId) {
		this.slotIndex = slotIndex;
		this.techniqueId = techniqueId;
	}

	public EquipTechniqueC2S(FriendlyByteBuf buf) {
		this.slotIndex = buf.readInt();
		this.techniqueId = buf.readUtf();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(this.slotIndex);
		buf.writeUtf(this.techniqueId);
	}

	public boolean handle(Supplier<NetworkEvent.Context> supplier) {
		NetworkEvent.Context context = supplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					data.getTechniques().equipOrSwapTechnique(slotIndex, techniqueId);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				});
			}
		});
		context.setPacketHandled(true);
		return true;
	}
}