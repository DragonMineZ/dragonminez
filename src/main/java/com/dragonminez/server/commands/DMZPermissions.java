package com.dragonminez.server.commands;

import com.dragonminez.Reference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DMZPermissions {

    private static final List<PermissionNode<Boolean>> NODES = new ArrayList<>();

    // Admin (All)
    public static final PermissionNode<Boolean> ADMIN = register("admin", "Grants all DragonMineZ permissions.", (player, uuid, context) -> false);

    // Stats
    public static final PermissionNode<Boolean> STATS_SET_SELF = register("dmzstats.set.self", "Allows setting your own stats.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STATS_SET_OTHERS = register("dmzstats.set.others", "Allows setting other players' stats.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STATS_ADD_SELF = register("dmzstats.add.self", "Allows adding to your own stats.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STATS_ADD_OTHERS = register("dmzstats.add.others", "Allows adding to other players' stats.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STATS_INFO_SELF = register("dmzstats.info.self", "Allows viewing your own stats.", (player, uuid, context) -> true);
    public static final PermissionNode<Boolean> STATS_INFO_OTHERS = register("dmzstats.info.others", "Allows viewing other players' stats.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STATS_RESET_SELF = register("dmzstats.reset.self", "Allows resetting your own stats.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STATS_RESET_OTHERS = register("dmzstats.reset.others", "Allows resetting other players' stats.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STATS_TRANSFORM_SELF = register("dmzstats.transform.self", "Allows transforming yourself via command.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STATS_TRANSFORM_OTHERS = register("dmzstats.transform.others", "Allows transforming other players via command.", (player, uuid, context) -> false);

    // Skills
    public static final PermissionNode<Boolean> SKILLS_SET_SELF = register("dmzskill.set.self", "Allows setting your own skills.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> SKILLS_SET_OTHERS = register("dmzskill.set.others", "Allows setting other players' skills.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> SKILLS_ADD_SELF = register("dmzskill.add.self", "Allows adding skills to yourself.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> SKILLS_ADD_OTHERS = register("dmzskill.add.others", "Allows adding skills to other players.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> SKILLS_REMOVE_SELF = register("dmzskill.remove.self", "Allows removing your own skills.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> SKILLS_REMOVE_OTHERS = register("dmzskill.remove.others", "Allows removing other players' skills.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> SKILLS_LIST_SELF = register("dmzskill.list.self", "Allows listing your own skills.", (player, uuid, context) -> true);
    public static final PermissionNode<Boolean> SKILLS_LIST_OTHERS = register("dmzskill.list.others", "Allows listing other players' skills.", (player, uuid, context) -> false);

    // Bonus
    public static final PermissionNode<Boolean> BONUS_ADD_SELF = register("dmzbonus.add.self", "Allows adding bonus stats to yourself.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> BONUS_ADD_OTHERS = register("dmzbonus.add.others", "Allows adding bonus stats to other players.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> BONUS_CLEAR_SELF = register("dmzbonus.clear.self", "Allows clearing your own bonus stats.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> BONUS_CLEAR_OTHERS = register("dmzbonus.clear.others", "Allows clearing other players' bonus stats.", (player, uuid, context) -> false);

    // Effects
    public static final PermissionNode<Boolean> EFFECTS_GIVE_SELF = register("dmzeffect.give.self", "Allows giving effects to yourself.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> EFFECTS_GIVE_OTHERS = register("dmzeffect.give.others", "Allows giving effects to other players.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> EFFECTS_REMOVE_SELF = register("dmzeffect.remove.self", "Allows removing your own effects.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> EFFECTS_REMOVE_OTHERS = register("dmzeffect.remove.others", "Allows removing other players' effects.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> EFFECTS_CLEAR_SELF = register("dmzeffect.clear.self", "Allows clearing your own effects.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> EFFECTS_CLEAR_OTHERS = register("dmzeffect.clear.others", "Allows clearing other players' effects.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> EFFECTS_LIST_SELF = register("dmzeffect.list.self", "Allows listing your own effects.", (player, uuid, context) -> true);
    public static final PermissionNode<Boolean> EFFECTS_LIST_OTHERS = register("dmzeffect.list.others", "Allows listing other players' effects.", (player, uuid, context) -> false);

    // Points
    public static final PermissionNode<Boolean> POINTS_SET_SELF = register("dmzpoints.set.self", "Allows setting your own points.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> POINTS_SET_OTHERS = register("dmzpoints.set.others", "Allows setting other players' points.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> POINTS_ADD_SELF = register("dmzpoints.add.self", "Allows adding to your own points.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> POINTS_ADD_OTHERS = register("dmzpoints.add.others", "Allows adding to other players' points.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> POINTS_REMOVE_SELF = register("dmzpoints.remove.self", "Allows removing your own points.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> POINTS_REMOVE_OTHERS = register("dmzpoints.remove.others", "Allows removing other players' points.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> POINTS_INFO_SELF = register("dmzpoints.info.self", "Allows viewing your own points.", (player, uuid, context) -> true);
    public static final PermissionNode<Boolean> POINTS_INFO_OTHERS = register("dmzpoints.info.others", "Allows viewing other players' points.", (player, uuid, context) -> false);

    // Story
    public static final PermissionNode<Boolean> STORY_FINISH_SELF = register("dmzstory.finish.self", "Allows finishing quests for yourself.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STORY_FINISH_OTHERS = register("dmzstory.finish.others", "Allows finishing quests for other players.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STORY_REMOVE_SELF = register("dmzstory.remove.self", "Allows removing quests for yourself.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STORY_REMOVE_OTHERS = register("dmzstory.remove.others", "Allows removing quests for other players.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> STORY_LIST_SELF = register("dmzstory.list.self", "Allows listing your own quest progress.", (player, uuid, context) -> true);
    public static final PermissionNode<Boolean> STORY_LIST_OTHERS = register("dmzstory.list.others", "Allows listing other players' quest progress.", (player, uuid, context) -> false);

    // Locate
    public static final PermissionNode<Boolean> LOCATE = register("dmzlocate", "Allows locating special structures.", (player, uuid, context) -> false);

    // Revive
    public static final PermissionNode<Boolean> REVIVE_SELF = register("dmzrevive.self", "Allows reviving yourself.", (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> REVIVE_OTHERS = register("dmzrevive.others", "Allows reviving other players.", (player, uuid, context) -> false);

    public static void init() {
    }

    private static PermissionNode<Boolean> register(String node, String description, PermissionNode.PermissionResolver<Boolean> defaultResolver) {
        PermissionNode<Boolean> permissionNode = new PermissionNode<>(Reference.MOD_ID, node, PermissionTypes.BOOLEAN, defaultResolver);
        permissionNode.setInformation(Component.literal(description), Component.literal(description));
        NODES.add(permissionNode);
        return permissionNode;
    }

    @SubscribeEvent
    public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
        NODES.forEach(event::addNodes);
    }

    public static boolean hasPermission(CommandSourceStack source, PermissionNode<Boolean> node) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionAPI.getPermission(player, ADMIN) || PermissionAPI.getPermission(player, node) || player.hasPermissions(2);
        }
        return source.hasPermission(2);
    }

    public static boolean check(CommandSourceStack source, PermissionNode<Boolean> selfNode, PermissionNode<Boolean> othersNode) {
        return hasPermission(source, selfNode) || hasPermission(source, othersNode);
    }
}
