package com.dragonminez.common.network.C2S;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class UpgradeTechniqueC2S {
	private final String techniqueId;
	private final String statType;

	public UpgradeTechniqueC2S(String techniqueId, String statType) {
		this.techniqueId = techniqueId;
		this.statType = statType;
	}

	public UpgradeTechniqueC2S(FriendlyByteBuf buf) {
		this.techniqueId = buf.readUtf();
		this.statType = buf.readUtf();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(this.techniqueId);
		buf.writeUtf(this.statType);
	}

	public boolean handle(Supplier<NetworkEvent.Context> supplier) {
		NetworkEvent.Context context = supplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					TechniqueData tech = data.getTechniques().getUnlockedTechniques().get(techniqueId);
					if (tech != null && !PredefinedTechniques.isPredefinedTechnique(tech)) {
						int cost = 100;
						if (tech.getExperience() >= cost) {
							tech.setExperience(tech.getExperience() - cost);
							switch (statType) {
								case "damage" -> {
									if (tech instanceof KiAttackData ki) ki.setDamageMultiplier(ki.getDamageMultiplier() + 0.1f);
									else if (tech instanceof StrikeAttackData st) st.setDamageMultiplier(st.getDamageMultiplier() + 0.1f);
								}
								case "size" -> { if (tech instanceof KiAttackData ki) ki.setSize(ki.getSize() + 0.1f); }
								case "speed" -> { if (tech instanceof KiAttackData ki) ki.setSpeed(ki.getSpeed() + 0.1f); }
								case "armor_pen" -> { if (tech instanceof KiAttackData ki) ki.setArmorPenetration(ki.getArmorPenetration() + 1); }
								case "cooldown" -> tech.setCooldown(Math.max(0, tech.getCooldown() - 2));
								case "cast" -> tech.setCastTime(Math.max(0, tech.getCastTime() - 1));
							}
							if (tech instanceof KiAttackData ki) ki.calculateAndSetBaseCost();
						}
					}
				});
			}
		});
		context.setPacketHandled(true);
		return true;
	}
}