package com.yuseix.dragonminez.common.network.C2S;

import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;


public class SuperFormsC2S {
	private String skill;
	private int value;
	private int cost;

	public SuperFormsC2S(String skill, int value, int cost) {
		this.skill = skill;
		this.value = value;
		this.cost = cost;
	}

	public SuperFormsC2S(FriendlyByteBuf buf) {
		skill = buf.readUtf();
		value = buf.readInt();
		cost = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(skill);
		buf.writeInt(value);
		buf.writeInt(cost);
	}

	public static void handle(SuperFormsC2S packet, Supplier<NetworkEvent.Context> crx) {
		NetworkEvent.Context context = crx.get();
		context.enqueueWork(() -> {
			ServerPlayer player = crx.get().getSender();

			if (player != null) {
				DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
					switch (packet.skill.toLowerCase(Locale.ROOT)) {
						case "super_form":
							cap.setFormSkillLvl("super_form", packet.value);
							cap.removeIntValue("tps", packet.cost);
							break;
						// Más superforms luego.
					}
				});
			}

		});
	}
}
