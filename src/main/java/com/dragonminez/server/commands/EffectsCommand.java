package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class EffectsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzeffect")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("effect", StringArgumentType.string())
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(-1))
                                                .executes(ctx -> giveEffect(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "effect"),
                                                        IntegerArgumentType.getInteger(ctx, "duration")
                                                ))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("effect", StringArgumentType.string())
                                        .executes(ctx -> removeEffect(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "effect")
                                        )))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> clearEffects(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")
                                ))))
                .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> listEffects(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")
                                )))));
    }

    private static int giveEffect(CommandSourceStack source, ServerPlayer target, String effectName, int duration) {
        StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
            double power = getEffectPower(effectName);

            if (power == 0.0) {
                source.sendFailure(Component.translatable("command.dragonminez.effects.unknown_effect", effectName));
                return;
            }

            int durationInTicks = duration == -1 ? -1 : duration * 20;

            data.getEffects().addEffect(effectName, power, durationInTicks);

            NetworkHandler.sendToPlayer(new StatsSyncS2C(target), target);

            String durationText = duration == -1 ? "permanent" : duration + " seconds";
            source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.give_success", effectName, (1.0 + power), target.getName().getString(), durationText), true);
        });

        return 1;
    }

    private static int removeEffect(CommandSourceStack source, ServerPlayer target, String effectName) {
        StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
            if (data.getEffects().hasEffect(effectName)) {
                data.getEffects().removeEffect(effectName);
                NetworkHandler.sendToPlayer(new StatsSyncS2C(target), target);
                source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.remove_success", effectName, target.getName().getString()), true);
            } else {
                source.sendFailure(Component.translatable("command.dragonminez.effects.no_effect", target.getName().getString(), effectName));
            }
        });

        return 1;
    }

    private static int clearEffects(CommandSourceStack source, ServerPlayer target) {
        StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
            data.getEffects().clear();
            NetworkHandler.sendToPlayer(new StatsSyncS2C(target), target);
            source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.clear_success", target.getName().getString()), true);
        });

        return 1;
    }

    private static int listEffects(CommandSourceStack source, ServerPlayer target) {
        StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
            var effects = data.getEffects().getEffectsSortedByDuration();

            if (effects.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.no_active_effects", target.getName().getString()), false);
            } else {
                source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.list_header", target.getName().getString()), false);

                for (var effect : effects) {
                    String duration = effect.isPermanent() ? "permanent" :
                            (effect.getDuration() / 20) + "s";
                    source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.list_entry", effect.getName(), (1.0 + effect.getPower()), duration), false);
                }
            }
        });

        return 1;
    }

    private static double getEffectPower(String effectName) {
        var serverConfig = ConfigManager.getServerConfig();
        if (serverConfig == null) {
            return 0.0;
        }

        return switch (effectName.toLowerCase()) {
            case "mightfruit" -> serverConfig.getGameplay().getMightFruitPower() - 1.0;
            case "majin" -> serverConfig.getGameplay().getMajinPower() - 1.0;
            default -> 0.0;
        };
    }
}

