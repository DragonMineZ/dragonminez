package com.yuseix.dragonminez.common.network.C2S;

import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import com.yuseix.dragonminez.common.stats.skills.DMZSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

public class MasterSkillsC2S {
	private String name;
	private DMZSkill skill;

	public MasterSkillsC2S(String name, DMZSkill skill) {
		this.name = name;
		this.skill = skill;
	}

	public MasterSkillsC2S(FriendlyByteBuf buf) {
		name = buf.readUtf();
		skill = new DMZSkill(buf);
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(name);
		skill.toBytes(buf);
	}

	public static void handle(MasterSkillsC2S packet, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		context.enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			int tpCost = 0;
			// Verifica si la skill es fly, ki_control, ki_manipulation, jump, meditation, potential_unlock o kaioken
			switch (packet.name.toLowerCase(Locale.ROOT)) {
				case "fly":
					tpCost = DMZGeneralConfig.FLY_TP_COST_MASTER.get();
					break;
				case "ki_control":
					tpCost = DMZGeneralConfig.KI_CONTROL_TP_COST_MASTER.get();
					break;
				case "ki_manipulation":
					tpCost = DMZGeneralConfig.KI_MANIPULATION_TP_COST_MASTER.get();
					break;
				case "jump":
					tpCost = DMZGeneralConfig.JUMP_TP_COST_MASTER.get();
					break;
				case "meditation":
					tpCost = DMZGeneralConfig.MEDITATION_TP_COST_MASTER.get();
					break;
				case "potunlock":
					tpCost = DMZGeneralConfig.POTUNLOCK_TP_COST_MASTER.get();
					break;
				case "kaioken":
					//tpCost = DMZGeneralConfig.KAIOKEN_TP_COST_MASTER.get();
					break;
			}

			if (player != null) {
				int finalTpCost = tpCost;
				DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
					playerstats.addSkill(packet.name, packet.skill);
					playerstats.removeIntValue("tps", finalTpCost);
				});
			}
		});
	}
}
