package com.dragonminez.server.commands;

import com.dragonminez.client.events.ForgeClientEvents;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.StatsEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StatsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzstats")
                .requires(source -> source.hasPermission(2))

                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("set")
                                .then(Commands.literal("str")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 10000))
                                                .executes(ctx -> setStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "str",
                                                        IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(Commands.literal("skp")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 10000))
                                                .executes(ctx -> setStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "skp",
                                                        IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(Commands.literal("res")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 10000))
                                                .executes(ctx -> setStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "res",
                                                        IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(Commands.literal("vit")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 10000))
                                                .executes(ctx -> setStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "vit",
                                                        IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(Commands.literal("pwr")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 10000))
                                                .executes(ctx -> setStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "pwr",
                                                        IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(Commands.literal("ene")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 10000))
                                                .executes(ctx -> setStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "ene",
                                                        IntegerArgumentType.getInteger(ctx, "value"))))))

                        .then(Commands.literal("add")
                                .then(Commands.literal("str")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000))
                                                .executes(ctx -> addStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "str",
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("skp")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000))
                                                .executes(ctx -> addStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "skp",
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("res")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000))
                                                .executes(ctx -> addStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "res",
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("vit")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000))
                                                .executes(ctx -> addStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "vit",
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("pwr")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000))
                                                .executes(ctx -> addStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "pwr",
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("ene")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000))
                                                .executes(ctx -> addStat(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        "ene",
                                                        IntegerArgumentType.getInteger(ctx, "amount"))))))

                        .then(Commands.literal("info")
                                .executes(ctx -> showInfo(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"))))

                        .then(Commands.literal("reset")
                                .executes(ctx -> resetStats(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"))))

                        .then(Commands.literal("transform")
                                .then(Commands.argument("group", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .then(Commands.argument("form", com.mojang.brigadier.arguments.StringArgumentType.string())
                                                .executes(ctx -> setTransform(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "group"),
                                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "form"))))))

                        .then(Commands.literal("untransform")
                                .executes(ctx -> clearTransform(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"))))
                )
        );
    }

    private static int setStat(CommandSourceStack source, ServerPlayer player, String stat, int value) {
        int maxStat = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
        int finalValue = Math.min(value, maxStat);

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            switch (stat.toLowerCase()) {
                case "str" -> data.getStats().setStrength(finalValue);
                case "skp" -> data.getStats().setStrikePower(finalValue);
                case "res" -> data.getStats().setResistance(finalValue);
                case "vit" -> data.getStats().setVitality(finalValue);
                case "pwr" -> data.getStats().setKiPower(finalValue);
                case "ene" -> data.getStats().setEnergy(finalValue);
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

            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });

        String key = amount >= 0 ? "command.dragonminez.stats.add.success.added" : "command.dragonminez.stats.add.success.removed";
        source.sendSuccess(() -> Component.translatable(key,
                Math.abs(amount), stat.toUpperCase(), player.getName().getString()), true);
        return 1;
    }

    private static int showInfo(CommandSourceStack source, ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            var stats = data.getStats();
            var status = data.getStatus();

            source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.info.title",
                    player.getName().getString()), false);
            source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.info.level",
                    data.getLevel(), data.getBattlePower()), false);
            source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.info.stats_line1",
                    stats.getStrength(), stats.getStrikePower(), stats.getResistance()), false);
            source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.info.stats_line2",
                    stats.getVitality(), stats.getKiPower(), stats.getEnergy()), false);
            source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.info.max_values",
                    data.getMaxHealth(), data.getMaxEnergy(), data.getMaxStamina()), false);

            String aliveKey = status.isAlive() ? "command.dragonminez.stats.info.status.alive" : "command.dragonminez.stats.info.status.dead";
            String charKey = status.hasCreatedCharacter() ? "command.dragonminez.stats.info.character.created" : "command.dragonminez.stats.info.character.not_created";
            source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.info.status",
                    Component.translatable(aliveKey), Component.translatable(charKey)), false);
        });
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
			ForgeClientEvents.hasCreatedCharacterCache = false;
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
