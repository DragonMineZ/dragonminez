package com.dragonminez.common.commands;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class SkillsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzskill")
                .requires(source -> source.hasPermission(2)) // Permiso de OP nivel 2

                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("set")
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .suggests(SKILL_SUGGESTIONS) // Sugiere las skills disponibles
                                        .then(Commands.argument("active", BoolArgumentType.bool())
                                                .executes(ctx -> setSkillActive(ctx,
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "skill"),
                                                        BoolArgumentType.getBool(ctx, "active"))))))

                        .then(Commands.literal("list")
                                .executes(ctx -> listSkills(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"))))

                        .then(Commands.literal("add")
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .suggests(SKILL_SUGGESTIONS)
                                        .executes(ctx -> addSkill(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "skill")))))

                        .then(Commands.literal("remove")
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .suggests(SKILL_SUGGESTIONS)
                                        .executes(ctx -> removeSkill(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "skill")))))
                )
        );
    }

    private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS = (context, builder) -> {
        ServerPlayer player = null;
        try {
            player = EntityArgument.getPlayer(context, "player");
        } catch (Exception e) {
            if (context.getSource().getEntity() instanceof ServerPlayer sp) {
                player = sp;
            }
        }

        if (player != null) {
            AtomicReference<Set<String>> skillKeys = new AtomicReference<>();
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                skillKeys.set(data.getSkills().getAllSkills().keySet());
            });

            if (skillKeys.get() != null) {
                return SharedSuggestionProvider.suggest(skillKeys.get(), builder);
            }
        }
        return SharedSuggestionProvider.suggest(new ArrayList<>(), builder);
    };

    private static int setSkillActive(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String skillId, boolean active) {
        AtomicReference<Boolean> success = new AtomicReference<>(false);

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (data.getSkills().getAllSkills().containsKey(skillId)) {
                data.getSkills().setSkillActive(skillId, active);
                NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
                success.set(true);
            }
        });

        if (success.get()) {
            String status = active ? "activated" : "deactivated";
            ctx.getSource().sendSuccess(() -> Component.literal("§aSkill " + skillId + " " + status + " for " + player.getName().getString()), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("§cSkill " + skillId + " not found or could not be modified for " + player.getName().getString()));
            return 0;
        }
    }

    private static int listSkills(CommandSourceStack source, ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            source.sendSuccess(() -> Component.literal("§6Skills for " + player.getName().getString() + ":"), false);
            data.getSkills().getAllSkills().forEach((id, skill) -> {
                String status = skill.isActive() ? "§a[ON]" : "§c[OFF]"; // Asumiendo que Skill tiene .isActive()
                source.sendSuccess(() -> Component.literal(" - " + id + " " + status), false);
            });
        });
        return 1;
    }

    private static int addSkill(CommandSourceStack source, ServerPlayer player, String skillId) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {

            if (!data.getSkills().hasSkill(skillId)) {
                 data.getSkills().addSkillLevel(skillId, 1);
             }

            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });
        source.sendSuccess(() -> Component.literal("§aAdded/Unlocked skill " + skillId + " for " + player.getName().getString()), true);
        return 1;
    }

    private static int removeSkill(CommandSourceStack source, ServerPlayer player, String skillId) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            // data.getSkills().removeSkill(skillId);
            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });
        source.sendSuccess(() -> Component.literal("§cRemoved skill " + skillId + " from " + player.getName().getString()), true);
        return 1;
    }
}