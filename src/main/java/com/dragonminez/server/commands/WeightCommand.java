package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.item.WeightItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class WeightCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzweight")
                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.WEIGHT_GIVE))
                .then(Commands.literal("turtle_shell")
                        .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                                .executes(ctx -> giveWeight(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "weight"), MainItems.WEIGHT_TURTLE_SHELL))))
                .then(Commands.literal("workout_weights")
                        .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                                .executes(ctx -> giveWeight(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "weight"), MainItems.WORKOUT_WEIGHTS))))
                .then(Commands.literal("piccolo_cape")
                        .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                                .executes(ctx -> giveWeight(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "weight"), MainItems.WEIGHT_PICCOLO_CAPE))))
                .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                        .executes(ctx -> giveWeight(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "weight"), MainItems.WORKOUT_WEIGHTS)))
        );
    }

    private static int giveWeight(CommandSourceStack source, int weightValue, Supplier<Item> item) {
        boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack weightStack = new ItemStack(item.get());
            WeightItem.setWeight(weightStack, weightValue);

            if (!player.getInventory().add(weightStack)) {
                player.drop(weightStack, false);
            }

            source.sendSuccess(() -> Component.translatable("command.dragonminez.weight.give.success", weightValue, player.getName().getString()), log);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.dragonminez.weight.give.error"));
            return 0;
        }
    }
}
