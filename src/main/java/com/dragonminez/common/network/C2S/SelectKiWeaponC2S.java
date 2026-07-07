package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class SelectKiWeaponC2S {

	private final String type;

	public SelectKiWeaponC2S(String type) {
		this.type = type != null ? type : "";
	}

	public SelectKiWeaponC2S(FriendlyByteBuf buffer) {
		this.type = buffer.readUtf();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(type);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (player.hasEffect(MainEffects.STUN.get())) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getSkills().hasSkill("kimanipulation")) return;
				List<String> types = ConfigManager.getCombatConfig().getKiWeaponTypes();
				if (type.isEmpty() || !types.contains(type.toLowerCase())) return;

				boolean active = data.getSkills().isSkillActive("kimanipulation");
				String current = data.getStatus().getKiWeaponType();

				if (active && current != null && current.equalsIgnoreCase(type)) {
					data.getSkills().setSkillActive("kimanipulation", false);
				} else {
					data.getStatus().setKiWeaponType(type.toLowerCase());
					if (!active) data.getSkills().setSkillActive("kimanipulation", true);
				}

				player.refreshDimensions();
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}
