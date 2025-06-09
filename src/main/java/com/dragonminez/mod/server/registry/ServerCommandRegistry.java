package com.dragonminez.mod.server.registry;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.core.common.util.LogUtil;
import com.dragonminez.mod.server.command.player.PlayerCommand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Central registry for server-side commands under the root command {@code /dmz}.
 *
 * <p>Subcommands should be registered via the {@link #init(LiteralArgumentBuilder)} method.
 * This allows consistent structure and modular inclusion of features.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * root.then(DMZPlayerCapCommand.register());
 * }</pre>
 */

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Bus.FORGE)
public class ServerCommandRegistry {

  /**
   * Initializes and attaches all subcommands under the /dmz root command.
   *
   * @param root the root literal argument builder for the /dmz command
   */
  public static void init(LiteralArgumentBuilder<CommandSourceStack> root) {
    root.then(PlayerCommand.init());
  }

  /**
   * Event handler for command registration. This method is automatically invoked by the Forge event
   * bus when commands are being registered.
   *
   * @param event the command registration event
   */
  @SubscribeEvent
  public static void registerCommands(RegisterCommandsEvent event) {
    final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    final LiteralArgumentBuilder<CommandSourceStack> dmzRoot = Commands.literal("dmz")
        .requires(source -> source.hasPermission(4));

    ServerCommandRegistry.init(dmzRoot);
    dispatcher.register(dmzRoot);

    LogUtil.info("Registered commands successfully.");
  }
}
