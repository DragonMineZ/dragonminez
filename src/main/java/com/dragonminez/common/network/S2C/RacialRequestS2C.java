package com.dragonminez.common.network.S2C;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RacialRequestS2C {
	private final String requesterName;
	private final int timeoutSeconds;

	public RacialRequestS2C(String requesterName, int timeoutSeconds) {
		this.requesterName = requesterName == null ? "" : requesterName;
		this.timeoutSeconds = timeoutSeconds;
	}

	public RacialRequestS2C(FriendlyByteBuf buf) {
		this.requesterName = buf.readUtf();
		this.timeoutSeconds = buf.readVarInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(requesterName);
		buf.writeVarInt(timeoutSeconds);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> show(requesterName, timeoutSeconds)));
		context.setPacketHandled(true);
	}

	private static void show(String requesterName, int timeoutSeconds) {
		var player = Minecraft.getInstance().player;
		if (player == null) return;

		Component accept = Component.translatable("gui.dragonminez.racial.request.accept")
				.withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dmzracial reply accept")));
		Component reject = Component.translatable("gui.dragonminez.racial.request.reject")
				.withStyle(style -> style.withColor(ChatFormatting.RED).withBold(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dmzracial reply reject")));

		player.displayClientMessage(Component.translatable("message.dragonminez.racial.namek.request_received", requesterName, timeoutSeconds)
				.append(Component.literal(" "))
				.append(accept)
				.append(Component.literal(" / "))
				.append(reject), false);
	}
}
