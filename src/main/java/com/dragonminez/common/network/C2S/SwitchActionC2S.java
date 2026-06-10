package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
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
				if (player.hasEffect(MainEffects.STUN.get())) return;
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					ActionMode newMode;
					if (data.getStatus().getSelectedAction() != mode) {
						newMode = mode;
					} else {
						newMode = ActionMode.FORM;
					}
					data.getStatus().setSelectedAction(newMode);

					boolean committed = false;
					if (newMode == ActionMode.FORM) committed = TransformationsHelper.ensureSelectedFormDefault(data);
					else if (newMode == ActionMode.STACK) committed = TransformationsHelper.ensureSelectedStackFormDefault(data);

					if (committed) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				});
			}
		});
		context.setPacketHandled(true);
	}
}