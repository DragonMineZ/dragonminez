package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import com.dragonminez.server.util.PotionEffectHelper;
import net.minecraft.server.level.ServerPlayer;

public class CooldownEffectHandler implements IStatusEffectHandler {
	@Override
	public void handleStatusEffects(ServerPlayer player, StatsData data) {
		PotionEffectHelper.syncCooldownIndicator(player, data, Cooldowns.DASH_CD, MainEffects.DASH_CD.get());
		PotionEffectHelper.syncCooldownIndicator(player, data, Cooldowns.DOUBLEDASH_CD, MainEffects.DOUBLEDASH_CD.get());
		PotionEffectHelper.syncCooldownIndicator(player, data, Cooldowns.TELEPORT_CD, MainEffects.TELEPORT_CD.get());
		PotionEffectHelper.syncCooldownIndicator(player, data, Cooldowns.FUSION_CD, MainEffects.FUSION_CD.get());
		PotionEffectHelper.syncCooldownIndicator(player, data, Cooldowns.KI_BLAST_CD, MainEffects.KI_BLAST_CD.get());
		PotionEffectHelper.syncCooldownIndicator(player, data, Cooldowns.POISE_CD, MainEffects.POISE_CD.get());
		PotionEffectHelper.syncCooldownIndicator(player, data, Cooldowns.MAJIN_REVIVE_CD, MainEffects.MAJIN_REVIVE.get());
		PotionEffectHelper.syncCooldownIndicator(player, data, Cooldowns.ZENKAI, MainEffects.SAIYAN_PASSIVE.get());
		PotionEffectHelper.syncCooldownIndicator(player, data, Cooldowns.DRAIN, MainEffects.BIOANDROID_PASSIVE.get());
	}

	@Override
	public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {

	}

	@Override
	public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {

	}
}
