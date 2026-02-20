package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

public class StackMasteryCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzstackmastery")
                // set <target> <group> <form> <value>
                .then(Commands.literal("set")
                        .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.MASTERY_SET))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests(SUGGEST_GROUPS)
                                        .then(Commands.argument("form", StringArgumentType.word())
                                                .suggests(SUGGEST_FORMS)
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                                                        .executes(ctx -> setMastery(ctx, false)))))))

                // add <target> <group> <form> <value>
                .then(Commands.literal("add")
                        .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.MASTERY_ADD))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests(SUGGEST_GROUPS)
                                        .then(Commands.argument("form", StringArgumentType.word())
                                                .suggests(SUGGEST_FORMS)
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                        .executes(ctx -> setMastery(ctx, true)))))))
        );
    }

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_GROUPS = (context, builder) -> {
        try {
            ServerPlayer player = getTarget(context);
            if (player != null) {
                return SharedSuggestionProvider.suggest(ConfigManager.getAllStackForms().keySet(), builder);
            }
        } catch (Exception ignored) {}
        return SharedSuggestionProvider.suggest(new ArrayList<>(), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_FORMS = (context, builder) -> {
        try {
            ServerPlayer player = getTarget(context);
            String groupName = StringArgumentType.getString(context, "group");

            if (player != null && groupName != null) {
                FormConfig groupConfig = ConfigManager.getStackFormGroup(groupName);
                if (groupConfig != null) {
                    return SharedSuggestionProvider.suggest(groupConfig.getForms().keySet(), builder);
                }
            }
        } catch (Exception ignored) {}
        return SharedSuggestionProvider.suggest(new ArrayList<>(), builder);
    };

    private static ServerPlayer getTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntitySelector selector = context.getArgument("target", EntitySelector.class);
        return selector.findSinglePlayer(context.getSource());
    }

    private static int setMastery(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        String group = StringArgumentType.getString(ctx, "group");
        String form = StringArgumentType.getString(ctx, "form");
        double value = DoubleArgumentType.getDouble(ctx, "value");
        CommandSourceStack source = ctx.getSource();

        StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
            var masteries = data.getCharacter().getStackFormMasteries();

            double maxMastery = 100.0;
            FormConfig.FormData formData = ConfigManager.getForm(data.getCharacter().getRaceName(), group, form);
            if (formData != null) maxMastery = formData.getMaxMastery();

            if (add) {
                masteries.addMastery(group, form, value, maxMastery);
            } else {
                masteries.setMastery(group, form, value);
            }
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(target), target);
        });

        String modeKey = add ? "add" : "set";
        source.sendSuccess(() -> Component.translatable("command.dragonminez.mastery." + modeKey + ".success", value, group, form, target.getName().getString()), log);

        return 1;
    }
}