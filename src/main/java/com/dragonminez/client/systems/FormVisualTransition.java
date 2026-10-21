package com.dragonminez.client.systems;

import com.dragonminez.client.events.ClientStatsEvents;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.FormTransition;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.Function;

public final class FormVisualTransition {
	private FormVisualTransition() {}

	public static void tick(List<? extends Player> players) {
		Player local = Minecraft.getInstance().player;
		for (Player player : players) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				FormTransition transition = data.getFormTransition();
				transition.setLocallyReleased(player == local && ClientStatsEvents.isLocalActionChargeReleased());
				transition.tick(data, player.tickCount);
				if (transition.consumeDimensionRefresh()) player.refreshDimensions();
			});
		}
	}

	private static double observe(StatsData stats) {
		if (stats == null || DMZSkinLayer.PREVIEW_MODE || stats.getPlayer() == null) return Double.NaN;
		FormTransition transition = stats.getFormTransition();
		if (!transition.isReady()) return Double.NaN;
		return transition.observe(stats, transition.frameTime(Minecraft.getInstance().getFrameTime()));
	}

	public static FormTransition.Charge charge(StatsData stats) {
		double time = observe(stats);
		return Double.isNaN(time) ? null : stats.getFormTransition().charge(stats.getCharacter(), time);
	}

	public static float chargeFactor(StatsData stats) {
		FormTransition.Charge charge = charge(stats);
		return charge != null ? charge.factor() : 0.0f;
	}

	public static FormTransition.Revert revert(StatsData stats) {
		double time = observe(stats);
		return Double.isNaN(time) ? null : stats.getFormTransition().revert(time);
	}

	public static float[] color(StatsData stats, float[] base, Function<FormConfig.FormData, float[]> rgb) {
		Character character = stats.getCharacter();
		float[] out = FormTransition.Look.current(character).overlay(base, rgb);
		FormTransition.Charge charge = charge(stats);
		if (charge != null) out = FormTransition.lerp(out, charge.look().overlay(base, rgb), charge.factor());
		FormTransition.Revert revert = revert(stats);
		if (revert != null) out = FormTransition.lerp(out, revert.look().overlay(base, rgb), revert.factor());
		return out;
	}

	public static float[] modelScale(StatsData stats) {
		double time = observe(stats);
		if (Double.isNaN(time)) {
			Float[] resolved = stats.getCharacter().getResolvedModelScaling();
			return new float[]{resolved[0], resolved[1], resolved[2]};
		}
		return stats.getFormTransition().modelScale(stats.getCharacter(), time);
	}
}
