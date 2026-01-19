package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwitchActionC2S {
	private final ActionMode mode;

	public SwitchActionC2S(ActionMode mode) {
		this.mode = mode;
	}

	public SwitchActionC2S(FriendlyByteBuf buffer) {
		this.mode = buffer.readEnum(ActionMode.class);
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeEnum(mode);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					if (data.getStatus().getSelectedAction() != mode) {
						data.getStatus().setSelectedAction(mode);
					} else {
						data.getStatus().setSelectedAction(ActionMode.FORM);
					}
					player.playSound(MainSounds.UI_MENU_SWITCH.get());
				});
			}
		});
		context.setPacketHandled(true);
	}
}