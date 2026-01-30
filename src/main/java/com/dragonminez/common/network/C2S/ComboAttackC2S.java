package com.dragonminez.common.network.C2S;

import com.dragonminez.common.util.ComboManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class ComboAttackC2S {
	public ComboAttackC2S() {}
	public ComboAttackC2S(FriendlyByteBuf buffer) {}
	public void encode(FriendlyByteBuf buffer) {}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null) {
				ComboManager.setNextHitAsCombo(player.getUUID(), true);
			}
		});
		context.setPacketHandled(true);
	}
}