package com.dragonminez.common.network.C2S;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.techniques.KiAttackData;
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
					if (tech != null) {
						int cost = tech instanceof KiAttackData ki ? ki.getUpgradeXpCost(statType)
								: tech instanceof StrikeAttackData st ? st.getUpgradeXpCost(statType) : 100;
						if (tech.getExperience() >= cost) {
							if (tech instanceof KiAttackData ki && !ki.canUpgradeStat(statType)) return;
							if (tech instanceof StrikeAttackData st && !st.canUpgradeStat(statType)) return;
							tech.setExperience(tech.getExperience() - cost);
							switch (statType) {
								case "damage" -> {
									if (tech instanceof KiAttackData ki) {
										ki.setDamageMultiplier(ki.getDamageMultiplier() + 0.1f);
										ki.setDamageLevel(ki.getDamageLevel() + 1);
									} else if (tech instanceof StrikeAttackData st) {
										st.setDamageMultiplier(st.getDamageMultiplier() + 0.1f);
										st.setDamageLevel(st.getDamageLevel() + 1);
									}
								}
								case "size" -> {
									if (tech instanceof KiAttackData ki) {
										ki.setSize(ki.getSize() + 0.1f);
										ki.setSizeLevel(ki.getSizeLevel() + 1);
									}
								}
								case "speed" -> {
									if (tech instanceof KiAttackData ki) {
										ki.setSpeed(ki.getSpeed() + 0.1f);
										ki.setSpeedLevel(ki.getSpeedLevel() + 1);
									}
								}
								case "armor_pen" -> {
									if (tech instanceof KiAttackData ki) {
										ki.setArmorPenetration(ki.getArmorPenetration() + 1);
										ki.setArmorPenLevel(ki.getArmorPenLevel() + 1);
									}
								}
								case "cooldown" -> {
									if (tech instanceof KiAttackData ki) ki.setCooldownLevel(ki.getCooldownLevel() + 1);
									else if (tech instanceof StrikeAttackData st) st.setCooldownLevel(st.getCooldownLevel() + 1);
								}
								case "cast" -> {
									if (tech instanceof KiAttackData ki) ki.setCastTimeLevel(ki.getCastTimeLevel() + 1);
									else if (tech instanceof StrikeAttackData st) st.setCastTimeLevel(st.getCastTimeLevel() + 1);
								}
							}
							if (tech instanceof KiAttackData ki) ki.calculateDerivedValues();
							NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
						}
					}
				});
			}
		});
		context.setPacketHandled(true);
		return true;
	}
}
