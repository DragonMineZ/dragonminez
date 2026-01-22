package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

public class UpdateSkillC2S {

	private final String skillName;
	private final String action;
	private final int cost;

	public UpdateSkillC2S(String action, String skillName, int cost) {
		this.skillName = skillName;
		this.action = action;
		this.cost = cost;
	}

	public UpdateSkillC2S(FriendlyByteBuf buf) {
		this.skillName = buf.readUtf();
		this.action = buf.readUtf();
		this.cost = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(this.skillName);
		buf.writeUtf(this.action);
		buf.writeInt(this.cost);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {

					if (!data.getSkills().hasSkill(skillName)) return;
					Skill skill = data.getSkills().getSkill(skillName);

					switch (action.toLowerCase(Locale.ROOT)) {
						case "toggle":
							if (skill.getLevel() > 0) {
								skill.setActive(!skill.isActive());
							}
							break;

						case "upgrade":
							if (!skill.isMaxLevel()) {
								data.getResources().removeTrainingPoints(cost);
								skill.addLevel(1);
							}
							break;
					}

					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				});
			}
		});
		ctx.get().setPacketHandled(true);
	}
}