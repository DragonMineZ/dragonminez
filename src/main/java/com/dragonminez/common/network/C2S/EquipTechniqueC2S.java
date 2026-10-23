package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.Techniques;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.dragonminez.compat.network.NetworkEvent;

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
					boolean lockedSlot = data.getTechniques().isFormLoadoutActive()
							&& slotIndex < Techniques.FORM_LOADOUT_SIZE;
					boolean raceLocked = techniqueId != null && !techniqueId.isEmpty()
							&& !ConfigManager.getSkillsConfig().isSkillAllowedForRace(techniqueId, data.getCharacter().getRaceName(), data.getStatus().isAndroidUpgraded());
					if (!lockedSlot && !raceLocked) data.getTechniques().equipOrSwapTechnique(slotIndex, techniqueId);
					NetworkHandler.sendToPlayer(new ProgressionSyncS2C(player), player);
				});
			}
		});
		context.setPacketHandled(true);
		return true;
	}
}