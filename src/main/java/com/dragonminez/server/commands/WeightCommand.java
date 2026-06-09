package com.dragonminez.server.commands;

import com.dragonminez.common.init.MainItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class WeightCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzweight")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                        .executes(ctx -> giveWeight(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "weight"))))
        );
    }

    private static int giveWeight(CommandSourceStack source, int weightValue) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack weightStack = new ItemStack(MainItems.WEIGHT_ITEM.get());
            weightStack.getOrCreateTag().putInt("WeightValue", weightValue);
            
            if (!player.getInventory().add(weightStack)) {
                player.drop(weightStack, false);
            }
            
            source.sendSuccess(() -> Component.literal("Given Weight item with weight: " + weightValue), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
