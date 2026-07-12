package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.Techniques;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * /dmzcooldowns <slot 1-8> <cooldown> [targets]
 *
 * Sets the active (remaining) cooldown of the ki/strike technique equipped in the given hotbar slot.
 * A value of 0 removes the cooldown entirely, letting the technique be used immediately.
 * The value is expressed in ticks (20 ticks = 1 second); this is the same unit techniques apply on fire.
 *
 * Both ki attacks and strike attacks store their active cooldown under the same
 * "TechniqueCooldown_&lt;id&gt;" key, so this works uniformly for either type.
 */
public class CooldownsCommand {

	private static final String TECHNIQUE_COOLDOWN_PREFIX = "TechniqueCooldown_";

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzcooldowns")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.COOLDOWNS_SELF, DMZPermissions.COOLDOWNS_OTHERS))

				// <slot 1-8> <cooldown> [targets]
				.then(Commands.argument("slot", IntegerArgumentType.integer(1, Techniques.SLOT_COUNT))
						.then(Commands.argument("cooldown", IntegerArgumentType.integer(0))
								.executes(ctx -> setCooldown(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()),
										IntegerArgumentType.getInteger(ctx, "slot"), IntegerArgumentType.getInteger(ctx, "cooldown")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.COOLDOWNS_OTHERS))
										.executes(ctx -> setCooldown(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
												IntegerArgumentType.getInteger(ctx, "slot"), IntegerArgumentType.getInteger(ctx, "cooldown"))))))
		);
	}

	private static int setCooldown(CommandSourceStack source, Collection<ServerPlayer> targets, int slot, int cooldownTicks) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		int slotIndex = slot - 1;

		AtomicInteger applied = new AtomicInteger();
		String[] lastPlayer = new String[1];
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				Techniques techniques = data.getTechniques();
				String techId = techniques.getEquippedSlots()[slotIndex];
				if (techId == null || techId.isEmpty()) return;

				TechniqueData tech = techniques.getUnlockedTechniques().get(techId);
				if (tech == null) return;

				// setCooldown removes the entry when value <= 0, so a cooldown of 0 clears it.
				data.getCooldowns().setCooldown(TECHNIQUE_COOLDOWN_PREFIX + techId, cooldownTicks);
				applied.incrementAndGet();
				lastPlayer[0] = player.getName().getString();
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}

		if (applied.get() == 0) {
			source.sendFailure(Component.translatable("command.dragonminez.cooldowns.empty_slot", slot));
			return 0;
		}

		if (applied.get() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.cooldowns.set_success", slot, cooldownTicks, lastPlayer[0]), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.cooldowns.set_multiple", slot, cooldownTicks, applied.get()), log);
		}
		return applied.get();
	}
}
