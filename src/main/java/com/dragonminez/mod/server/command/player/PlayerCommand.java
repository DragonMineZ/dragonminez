package com.dragonminez.mod.server.command.player;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.player.capability.CapData;
import com.dragonminez.mod.core.common.player.capability.CapDataHolder;
import com.dragonminez.mod.core.common.player.capability.CapManagerRegistry;
import com.dragonminez.mod.core.common.player.capability.ICap;
import com.dragonminez.mod.core.common.util.JavaUtil;
import com.dragonminez.mod.core.common.util.JavaUtil.DataType;
import com.dragonminez.mod.core.server.player.capability.IServerCapDataManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Registers and handles the /dmz player command used to manage custom player capabilities.
 * <p>
 * The command provides three main subcommands:
 * <ul>
 *   <li><b>set</b>: Sets a capability value on a target player, parsing the input based on the
 *       capability's data type and updating the server-side capability data.</li>
 *   <li><b>get</b>: Retrieves and displays the current value of a specific capability key
 *       from a target player.</li>
 *   <li><b>clone</b>: Copies all capability data from a source player to a target player,
 *       updating the target's capability data accordingly.</li>
 * </ul>
 * <p>
 * Argument suggestions are dynamically provided based on registered capability categories and keys,
 * and value suggestions adapt to the expected data type (e.g., booleans, numeric "max").
 * <p>
 * Internally, the command interacts with {@link CapManagerRegistry} to obtain capability managers,
 * retrieves capability data holders, parses values according to {@link JavaUtil.DataType},
 * and updates or queries data as needed.
 * <p>
 * Feedback is sent to the command source for both successful operations and errors.
 *
 * @see CapManagerRegistry
 * @see JavaUtil.DataType
 * @see net.minecraft.commands.CommandSourceStack
 */

public class PlayerCommand {

  /**
   * Suggests capability category IDs registered under this mod.
   */
  private static final SuggestionProvider<CommandSourceStack> CAP_ID_SUGGEST =
      (ctx, builder) ->
          SharedSuggestionProvider.suggest(CapManagerRegistry.managers(Dist.DEDICATED_SERVER)
                  .keySet().stream()
                  .filter(loc -> loc.getNamespace().equals(Reference.MOD_ID))
                  .map(ResourceLocation::getPath),
              builder
          );

  /**
   * Suggests capability keys available for a given capability category.
   */
  private static final SuggestionProvider<CommandSourceStack> CAP_KEY_SUGGEST =
      (ctx, builder) -> {
        final String category = StringArgumentType.getString(ctx, "category");
        final var manager = CapManagerRegistry.manager(
            new ResourceLocation(Reference.MOD_ID, category), Dist.DEDICATED_SERVER);
        if (manager == null) {
          return Suggestions.empty();
        }

        final var cap = manager.buildCap();
        return SharedSuggestionProvider.suggest(cap.holder().acceptedData()
            .stream()
            .map(CapData::id), builder);
      };

  /**
   * Suggests valid values for a capability key argument based on the key's data type. For numeric
   * types, suggests "max". For boolean types, suggests "true" and "false".
   */
  private static final SuggestionProvider<CommandSourceStack> VALUE_BY_TYPE_SUGGEST =
      (ctx, builder) -> {
        final String category = StringArgumentType.getString(ctx, "category");
        final String key = StringArgumentType.getString(ctx, "key");

        final var manager = CapManagerRegistry.manager(
            new ResourceLocation(Reference.MOD_ID, category), Dist.DEDICATED_SERVER);
        if (manager == null) {
          return Suggestions.empty();
        }

        final var cap = manager.buildCap();
        final CapData<?, ?> data = cap.holder().data(key);
        if (data == null) {
          return Suggestions.empty();
        }

        final DataType type = data.type();
        if (type.isNumeric()) {
          return SharedSuggestionProvider.suggest(List.of("max"), builder);
        } else if (type == DataType.BOOLEAN) {
          return SharedSuggestionProvider.suggest(List.of("true", "false"), builder);
        }

        return Suggestions.empty();
      };

  /**
   * Creates the root command builder for the "player" command with subcommands:
   * <ul>
   *   <li>set - sets a capability value for a target player</li>
   *   <li>get - gets a capability value from a target player</li>
   *   <li>clone - clones all capabilities from a source player to a target player</li>
   * </ul>
   *
   * @return the command literal builder
   */
  public static LiteralArgumentBuilder<CommandSourceStack> init() {
    return Commands.literal("player")
        .then(Commands.literal("set")
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("category", StringArgumentType.string())
                    .suggests(CAP_ID_SUGGEST)
                    .then(Commands.argument("key", StringArgumentType.string())
                        .suggests(CAP_KEY_SUGGEST)
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .suggests(VALUE_BY_TYPE_SUGGEST)
                            .executes(PlayerCommand::set))))))
        .then(Commands.literal("get")
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("category", StringArgumentType.string())
                    .suggests(CAP_ID_SUGGEST)
                    .then(Commands.argument("key", StringArgumentType.string())
                        .suggests(CAP_KEY_SUGGEST)
                        .executes(PlayerCommand::get)))))
        .then(Commands.literal("clone")
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("source", EntityArgument.player())
                    .executes(PlayerCommand::clone))));
  }

  /**
   * Executes the "set" subcommand to update a capability value for the specified player. Validates
   * category, key, and value type before setting.
   *
   * @param ctx command context
   * @return command success or failure code
   * @throws CommandSyntaxException if player argument parsing fails
   */
  private static int set(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    final ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
    final String category = StringArgumentType.getString(ctx, "category");
    final String key = StringArgumentType.getString(ctx, "key");

    final ResourceLocation id = new ResourceLocation(Reference.MOD_ID, category);
    final var manager = CapManagerRegistry.manager(id, Dist.DEDICATED_SERVER);
    if (manager == null) {
      return PlayerCommand.error(ctx, "§cUnknown category §4" + category);
    }

    final var serverManager = (IServerCapDataManager<?, ?>) manager;
    final ICap cap = manager.retrieveData(player);
    final CapDataHolder holder = cap.holder();
    final CapData<?, ?> data = holder.datas().get(key);
    if (data == null) {
      return PlayerCommand.error(ctx, "§cUnknown key §4" + key + "§c in category §4"
          + category);
    }

    String raw = StringArgumentType.getString(ctx, "value");
    final DataType type = data.type();
    final AtomicBoolean max = new AtomicBoolean(false);
    if (raw.equals("max") && type.isNumeric()) {
      // Use the maximum possible value for the given type.
      // Any necessary clamping should be handled internally by the capability manager.
      raw = switch (type) {
        case INTEGER -> String.valueOf(Integer.MAX_VALUE);
        case FLOAT -> String.valueOf(Float.MAX_VALUE);
        case DOUBLE -> String.valueOf(Double.MAX_VALUE);
        case LONG -> String.valueOf(Long.MAX_VALUE);
        default -> raw;
      };
      max.set(true);
    }

    final Object parsed = JavaUtil.parseValue(type, raw);
    if (parsed == null) {
      return PlayerCommand.error(ctx, "§cInvalid value §4" + raw + "§c for type §4" +
          type.name());
    }

    serverManager.set(player, key, parsed, true);
    ctx.getSource().sendSuccess(() ->
        Component.literal("§2" + data.legibleId() + " §ahas been set to §2" +
            (max.get() ? "max" : parsed)), false);
    return Command.SINGLE_SUCCESS;
  }

  /**
   * Executes the "get" subcommand to retrieve a capability value from the specified player.
   * Validates category and key.
   *
   * @param ctx command context
   * @return command success or failure code
   * @throws CommandSyntaxException if player argument parsing fails
   */
  private static int get(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    final ServerPlayer player = EntityArgument.getPlayer(ctx, "target");

    final String category = StringArgumentType.getString(ctx, "category");
    final String key = StringArgumentType.getString(ctx, "key");

    final ResourceLocation id = new ResourceLocation(Reference.MOD_ID, category);
    final var manager = CapManagerRegistry.manager(id, Dist.DEDICATED_SERVER);
    if (manager == null) {
      return PlayerCommand.error(ctx, "§cUnknown category §4" + category);
    }

    final ICap cap = manager.retrieveData(player);
    final CapDataHolder holder = cap.holder();
    final CapData<?, ?> data = holder.datas().get(key);
    if (data == null) {
      return PlayerCommand.error(ctx, "§cUnknown key §4" + key + "§c in category §4"
          + category);
    }

    final Object value = data.get(cap);
    ctx.getSource().sendSuccess(() ->
        Component.literal("§2" + data.legibleId() + ": §a" + value), false);
    return Command.SINGLE_SUCCESS;
  }

  /**
   * Executes the "clone" subcommand to copy all capability data from a source player to a target
   * player.
   *
   * @param ctx command context
   * @return command success code
   * @throws CommandSyntaxException if player argument parsing fails
   */
  private static int clone(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    final ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
    final ServerPlayer source = EntityArgument.getPlayer(ctx, "source");

    for (ResourceLocation id : CapManagerRegistry.managers(Dist.DEDICATED_SERVER).keySet()) {
      final var manager = CapManagerRegistry.manager(id, Dist.DEDICATED_SERVER);
      if (manager == null) {
        continue;
      }
      manager.update(target, source);
    }

    ctx.getSource().sendSuccess(() ->
        Component.literal("§aCloned all capabilities from §2" + source.getName().getString()
            + " §ato §2" + target.getName().getString()), false);
    return Command.SINGLE_SUCCESS;
  }

  /**
   * Sends an error message to the command source and returns failure code.
   *
   * @param ctx     the command context
   * @param message the error message to send
   * @return failure code (0)
   */
  private static int error(CommandContext<CommandSourceStack> ctx, String message) {
    ctx.getSource().sendFailure(Component.literal(message));
    return 0;
  }
}
