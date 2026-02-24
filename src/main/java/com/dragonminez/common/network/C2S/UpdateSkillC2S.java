package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateSkillC2S {

	public enum SkillAction {
		TOGGLE, UPGRADE, PURCHASE
	}

	private final String skillName;
	private final SkillAction action;
	private final int cost;

	public UpdateSkillC2S(SkillAction action, String skillName, int cost) {
		this.skillName = skillName;
		this.action = action;
		this.cost = cost;
	}

	public UpdateSkillC2S(FriendlyByteBuf buf) {
		this.skillName = buf.readUtf();
		this.action = buf.readEnum(SkillAction.class);
		this.cost = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(this.skillName);
		buf.writeEnum(this.action);
		buf.writeInt(this.cost);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					Skill skill = data.getSkills().getSkill(skillName);
					switch (action) {
						case TOGGLE:
							if (skill != null && skill.getLevel() > 0) {
								skill.setActive(!skill.isActive());
							}
							break;

						case UPGRADE:
							if (skill != null && !skill.isMaxLevel() && data.getResources().getTrainingPoints() >= cost && cost != -1 && !(skillName.equals("potentialunlock") && skill.getLevel() == 10)) {
								data.getResources().removeTrainingPoints(cost);
								skill.addLevel(1);
							}
							break;
						case PURCHASE:
							if (!data.getSkills().hasSkill(skillName)
									&& data.getResources().getTrainingPoints() >= cost
									&& cost != -1) {
								data.getResources().removeTrainingPoints(cost);
								data.getSkills().setSkillLevel(skillName, 1);
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