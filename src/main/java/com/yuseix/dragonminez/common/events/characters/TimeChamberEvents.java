package com.yuseix.dragonminez.common.events.characters;

import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.config.GeneralConfig;
import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import com.yuseix.dragonminez.server.worldgen.dimension.ModDimensions;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class TimeChamberEvents {
	private static final double penaltyLow = 2.0;
	private static final double penaltyHigh = 1.5;

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		Player player = event.getEntity();

		// Verificar si el jugador está saliendo de la Hab Tiempo
		if (event.getFrom().equals(ModDimensions.TIME_CHAMBER_DIM_LEVEL_KEY)) {
			resetPlayerAttributes(player); // Restaurar los atributos del jugador a default
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		Player player = event.player;

		// Verificar si el jugador está en la Hab Tiempo o en creativo
		if (!player.level().dimension().equals(ModDimensions.TIME_CHAMBER_DIM_LEVEL_KEY)) return;
		if (player.isCreative()) return;

		DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
			int str = stats.getStat("STR");
			int def = stats.getStat("DEF");
			int con = stats.getStat("CON");
			int pwr = stats.getStat("PWR");
			int ene = stats.getStat("ENE");
			boolean isDmzUser = stats.getBoolean("dmzuser");

			// Si no creó su personaje, no hacer nada xd
			if (!isDmzUser) return;

			int level = (str + def + con + pwr + ene) / 5; // Calcula la suma de las estadísticas
			int maxStats = GeneralConfig.attributes().maxAttributes;

			double speedMultiplier = 1.0;
			double jumpMultiplier = 1.0;
			double foodMultiplier = 1.0;

			// Penalizaciones basadas según stats al 15% o 25% del máximo xd
			if (level < (maxStats * 0.10)) {
				speedMultiplier = 0.5; // Reducir velocidad un 50%
				jumpMultiplier = 0.5;  // Reducir salto un 50%
				foodMultiplier = penaltyLow; // Aumentar consumo de comida
			} else if (level < (maxStats * 0.20)) {
				speedMultiplier = 0.75; // 25%
				jumpMultiplier = 0.75; // 25%
				foodMultiplier = penaltyHigh;
			}

			applyGravityPenalty(player, speedMultiplier, jumpMultiplier);
			applyFoodPenalty(player, foodMultiplier);
		});
	}

	private static void applyGravityPenalty(Player player, double speedMultiplier, double jumpMultiplier) {
		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance jumpStrength = player.getAttribute(Attributes.JUMP_STRENGTH);

		if (movementSpeed != null) {
			movementSpeed.setBaseValue(0.1 * speedMultiplier);
		}

		if (jumpStrength != null) {
			jumpStrength.setBaseValue(0.5 * jumpMultiplier);
		}
	}

	private static void resetPlayerAttributes(Player player) {
		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		AttributeInstance jumpStrength = player.getAttribute(Attributes.JUMP_STRENGTH);

		if (movementSpeed != null) {
			movementSpeed.setBaseValue(0.1);
		}

		if (jumpStrength != null) {
			jumpStrength.setBaseValue(0.5);
		}
	}

	private static void applyFoodPenalty(Player player, double multiplier) {
		FoodData foodData = player.getFoodData();

		// Simular mayor consumo de comida
		float exhaustionIncrease = (float) ((multiplier - 1.0) * 0.05); // Es raro como funciona este valor (0.5) xd, pero bueno, funciona.
		// No recomiendo ponerlo más de 0.1 porque si no es exagerado lo rápido que baja la comida sajdjasd
		foodData.addExhaustion(exhaustionIncrease);
	}
}
