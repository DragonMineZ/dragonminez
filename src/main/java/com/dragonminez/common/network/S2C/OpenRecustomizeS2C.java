package com.dragonminez.common.network.S2C;

import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenRecustomizeS2C {
	public OpenRecustomizeS2C() {
	}

	public OpenRecustomizeS2C(FriendlyByteBuf buf) {
	}

	public void encode(FriendlyByteBuf buf) {
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
					mc.setScreen(new CharacterCustomizationScreen(null, data.getCharacter()));
				});
			}
		}));
		ctx.get().setPacketHandled(true);
	}
}
