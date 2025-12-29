package com.dragonminez.server.commands;

import com.dragonminez.client.events.ForgeClientEvents;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.StatsEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StatsCommand {
	private static final int MAX_STAT_VALUE = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzstats")
                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_INFO_SELF, DMZPermissions.STATS_INFO_OTHERS))

                // set <stat> <value> [player]
                .then(Commands.literal("set")
                        .then(Commands.literal("str")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_SET_SELF, DMZPermissions.STATS_SET_OTHERS))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, MAX_STAT_VALUE))
                                        .executes(ctx -> setStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "str", IntegerArgumentType.getInteger(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_SET_OTHERS))
                                                .executes(ctx -> setStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "str", IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(Commands.literal("skp")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_SET_SELF, DMZPermissions.STATS_SET_OTHERS))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, MAX_STAT_VALUE))
                                        .executes(ctx -> setStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "skp", IntegerArgumentType.getInteger(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_SET_OTHERS))
                                                .executes(ctx -> setStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "skp", IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(Commands.literal("res")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_SET_SELF, DMZPermissions.STATS_SET_OTHERS))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, MAX_STAT_VALUE))
                                        .executes(ctx -> setStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "res", IntegerArgumentType.getInteger(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_SET_OTHERS))
                                                .executes(ctx -> setStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "res", IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(Commands.literal("vit")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_SET_SELF, DMZPermissions.STATS_SET_OTHERS))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, MAX_STAT_VALUE))
                                        .executes(ctx -> setStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "vit", IntegerArgumentType.getInteger(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_SET_OTHERS))
                                                .executes(ctx -> setStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "vit", IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(Commands.literal("pwr")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_SET_SELF, DMZPermissions.STATS_SET_OTHERS))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, MAX_STAT_VALUE))
                                        .executes(ctx -> setStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "pwr", IntegerArgumentType.getInteger(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_SET_OTHERS))
                                                .executes(ctx -> setStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "pwr", IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(Commands.literal("ene")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_SET_SELF, DMZPermissions.STATS_SET_OTHERS))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, MAX_STAT_VALUE))
                                        .executes(ctx -> setStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "ene", IntegerArgumentType.getInteger(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_SET_OTHERS))
                                                .executes(ctx -> setStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "ene", IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(Commands.literal("all")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_SET_SELF, DMZPermissions.STATS_SET_OTHERS))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, MAX_STAT_VALUE))
                                        .executes(ctx -> setStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "all", IntegerArgumentType.getInteger(ctx, "value")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_SET_OTHERS))
                                                .executes(ctx -> setStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "all", IntegerArgumentType.getInteger(ctx, "value"))))))
                )

                // add <stat> <amount> [player]
                .then(Commands.literal("add")
                        .then(Commands.literal("str")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_ADD_SELF, DMZPermissions.STATS_ADD_OTHERS))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-MAX_STAT_VALUE, MAX_STAT_VALUE))
                                        .executes(ctx -> addStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "str", IntegerArgumentType.getInteger(ctx, "amount")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_ADD_OTHERS))
                                                .executes(ctx -> addStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "str", IntegerArgumentType.getInteger(ctx, "amount"))))))
                        .then(Commands.literal("skp")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_ADD_SELF, DMZPermissions.STATS_ADD_OTHERS))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-MAX_STAT_VALUE, MAX_STAT_VALUE))
                                        .executes(ctx -> addStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "skp", IntegerArgumentType.getInteger(ctx, "amount")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_ADD_OTHERS))
                                                .executes(ctx -> addStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "skp", IntegerArgumentType.getInteger(ctx, "amount"))))))
                        .then(Commands.literal("res")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_ADD_SELF, DMZPermissions.STATS_ADD_OTHERS))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-MAX_STAT_VALUE, MAX_STAT_VALUE))
                                        .executes(ctx -> addStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "res", IntegerArgumentType.getInteger(ctx, "amount")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_ADD_OTHERS))
                                                .executes(ctx -> addStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "res", IntegerArgumentType.getInteger(ctx, "amount"))))))
                        .then(Commands.literal("vit")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_ADD_SELF, DMZPermissions.STATS_ADD_OTHERS))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-MAX_STAT_VALUE, MAX_STAT_VALUE))
                                        .executes(ctx -> addStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "vit", IntegerArgumentType.getInteger(ctx, "amount")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_ADD_OTHERS))
                                                .executes(ctx -> addStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "vit", IntegerArgumentType.getInteger(ctx, "amount"))))))
                        .then(Commands.literal("pwr")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_ADD_SELF, DMZPermissions.STATS_ADD_OTHERS))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-MAX_STAT_VALUE, MAX_STAT_VALUE))
                                        .executes(ctx -> addStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "pwr", IntegerArgumentType.getInteger(ctx, "amount")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_ADD_OTHERS))
                                                .executes(ctx -> addStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "pwr", IntegerArgumentType.getInteger(ctx, "amount"))))))
                        .then(Commands.literal("ene")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_ADD_SELF, DMZPermissions.STATS_ADD_OTHERS))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-MAX_STAT_VALUE, MAX_STAT_VALUE))
                                        .executes(ctx -> addStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "ene", IntegerArgumentType.getInteger(ctx, "amount")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_ADD_OTHERS))
                                                .executes(ctx -> addStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "ene", IntegerArgumentType.getInteger(ctx, "amount"))))))
                        .then(Commands.literal("all")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_ADD_SELF, DMZPermissions.STATS_ADD_OTHERS))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-MAX_STAT_VALUE, MAX_STAT_VALUE))
                                        .executes(ctx -> addStat(ctx.getSource(), ctx.getSource().getPlayerOrException(), "all", IntegerArgumentType.getInteger(ctx, "amount")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_ADD_OTHERS))
                                                .executes(ctx -> addStat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "all", IntegerArgumentType.getInteger(ctx, "amount"))))))
                )

                // reset [player]
                .then(Commands.literal("reset")
                        .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_RESET_SELF, DMZPermissions.STATS_RESET_OTHERS))
                        .executes(ctx -> resetStats(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_RESET_OTHERS))
                                .executes(ctx -> resetStats(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))

                // transform <group> <form> [player]
                .then(Commands.literal("transform")
                        .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_TRANSFORM_SELF, DMZPermissions.STATS_TRANSFORM_OTHERS))
                        .then(Commands.argument("group", StringArgumentType.string())
                                .then(Commands.argument("form", StringArgumentType.string())
                                        .executes(ctx -> setTransform(ctx.getSource(), ctx.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "group"), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "form")))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_TRANSFORM_OTHERS))
                                                .executes(ctx -> setTransform(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "group"), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "form")))))))

                // untransform [player]
                .then(Commands.literal("untransform")
                        .requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_TRANSFORM_SELF, DMZPermissions.STATS_TRANSFORM_OTHERS))
                        .executes(ctx -> clearTransform(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_TRANSFORM_OTHERS))
                                .executes(ctx -> clearTransform(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
        );
    }

    private static int setStat(CommandSourceStack source, ServerPlayer player, String stat, int value) {
        int maxStat = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
        int finalValue = Math.min(value, maxStat);

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (stat.equalsIgnoreCase("all")) {
                data.getStats().setStrength(finalValue);
                data.getStats().setStrikePower(finalValue);
                data.getStats().setResistance(finalValue);
                data.getStats().setVitality(finalValue);
                data.getStats().setKiPower(finalValue);
                data.getStats().setEnergy(finalValue);
            } else {
                switch (stat.toLowerCase()) {
                    case "str" -> data.getStats().setStrength(finalValue);
                    case "skp" -> data.getStats().setStrikePower(finalValue);
                    case "res" -> data.getStats().setResistance(finalValue);
                    case "vit" -> data.getStats().setVitality(finalValue);
                    case "pwr" -> data.getStats().setKiPower(finalValue);
                    case "ene" -> data.getStats().setEnergy(finalValue);
                }
            }

            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });

        source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.set.success",
                stat.toUpperCase(), finalValue, player.getName().getString()), true);
        return 1;
    }

    private static int addStat(CommandSourceStack source, ServerPlayer player, String stat, int amount) {
        int maxStat = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (stat.equalsIgnoreCase("all")) {
                data.getStats().setStrength(Math.max(0, Math.min(data.getStats().getStrength() + amount, maxStat)));
                data.getStats().setStrikePower(Math.max(0, Math.min(data.getStats().getStrikePower() + amount, maxStat)));
                data.getStats().setResistance(Math.max(0, Math.min(data.getStats().getResistance() + amount, maxStat)));
                data.getStats().setVitality(Math.max(0, Math.min(data.getStats().getVitality() + amount, maxStat)));
                data.getStats().setKiPower(Math.max(0, Math.min(data.getStats().getKiPower() + amount, maxStat)));
                data.getStats().setEnergy(Math.max(0, Math.min(data.getStats().getEnergy() + amount, maxStat)));
            } else {
                switch (stat.toLowerCase()) {
                    case "str" -> {
                        int newValue = Math.min(data.getStats().getStrength() + amount, maxStat);
                        data.getStats().setStrength(Math.max(0, newValue));
                    }
                    case "skp" -> {
                        int newValue = Math.min(data.getStats().getStrikePower() + amount, maxStat);
                        data.getStats().setStrikePower(Math.max(0, newValue));
                    }
                    case "res" -> {
                        int newValue = Math.min(data.getStats().getResistance() + amount, maxStat);
                        data.getStats().setResistance(Math.max(0, newValue));
                    }
                    case "vit" -> {
                        int newValue = Math.min(data.getStats().getVitality() + amount, maxStat);
                        data.getStats().setVitality(Math.max(0, newValue));
                    }
                    case "pwr" -> {
                        int newValue = Math.min(data.getStats().getKiPower() + amount, maxStat);
                        data.getStats().setKiPower(Math.max(0, newValue));
                    }
                    case "ene" -> {
                        int newValue = Math.min(data.getStats().getEnergy() + amount, maxStat);
                        data.getStats().setEnergy(Math.max(0, newValue));
                    }
                }
            }

            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });

        String key = amount >= 0 ? "command.dragonminez.stats.add.success.added" : "command.dragonminez.stats.add.success.removed";
        source.sendSuccess(() -> Component.translatable(key,
                Math.abs(amount), stat.toUpperCase(), player.getName().getString()), true);
        return 1;
    }

    private static int resetStats(CommandSourceStack source, ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            var stats = data.getStats();
            stats.setStrength(5);
            stats.setStrikePower(5);
            stats.setResistance(5);
            stats.setVitality(5);
            stats.setKiPower(5);
            stats.setEnergy(5);
            data.getStatus().setCreatedCharacter(false);
			data.getResources().setTrainingPoints(0);
			data.getSkills().removeAllSkills();
			data.getEffects().removeAllEffects();

			player.setHealth(20.0F);
			player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D);
			player.getAttribute(Attributes.MAX_HEALTH).removePermanentModifier(StatsEvents.DMZ_HEALTH_MODIFIER_UUID);
			player.setHealth(20.0F);
            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });

        source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.reset.success",
                player.getName().getString()), true);
        return 1;
    }

    private static int setTransform(CommandSourceStack source, ServerPlayer player, String group, String form) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            String raceName = data.getCharacter().getRaceName();

            if (!ConfigManager.hasForm(raceName, group, form)) {
                source.sendFailure(Component.literal("Form " + group + "." + form + " does not exist for race " + raceName));
                return;
            }

            data.getCharacter().setActiveForm(group, form);

            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);

            source.sendSuccess(() -> Component.literal("§a" + player.getName().getString() + " transformed into " + group + "." + form), true);
        });
        return 1;
    }

    private static int clearTransform(CommandSourceStack source, ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            data.getCharacter().clearActiveForm();
            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);

            source.sendSuccess(() -> Component.literal("§a" + player.getName().getString() + " returned to base form"), true);
        });
        return 1;
    }
}
