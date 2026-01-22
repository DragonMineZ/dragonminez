package com.dragonminez.server.commands;

import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Map;

public class LocateCommand {

	private static final Map<String, Pair<ResourceKey<Structure>, ResourceKey<Level>>> STRUCTURE_INFO = Map.of(
			"goku_house", Pair.of(DMZStructures.GOKU_HOUSE, Level.OVERWORLD),
			"roshi_house", Pair.of(DMZStructures.ROSHI_HOUSE, Level.OVERWORLD),
			"elder_guru", Pair.of(DMZStructures.ELDER_GURU, NamekDimension.NAMEK_KEY),
			"timechamber", Pair.of(DMZStructures.TIMECHAMBER, HTCDimension.HTC_KEY),
			"kamilookout", Pair.of(DMZStructures.KAMILOOKOUT, Level.OVERWORLD),
			"enma_palace", Pair.of(DMZStructures.ENMA_PALACE, OtherworldDimension.OTHERWORLD_KEY)
	);

	private static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(STRUCTURE_INFO.keySet(), builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzlocate")
				.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.LOCATE))
				.then(Commands.argument("structure", StringArgumentType.string())
						.suggests(SUGGESTIONS)
						.executes(context -> {
							String structName = StringArgumentType.getString(context, "structure");
							return locateStructure(context.getSource(), structName);
						})));
	}

	private static int locateStructure(CommandSourceStack source, String structName) {
		if (!STRUCTURE_INFO.containsKey(structName)) {
			source.sendFailure(Component.translatable("command.dragonminez.locate.invalid", structName));
			return 0;
		}

		Pair<ResourceKey<Structure>, ResourceKey<Level>> info = STRUCTURE_INFO.get(structName);
		ResourceKey<Structure> structureKey = info.getFirst();
		ResourceKey<Level> targetDimKey = info.getSecond();

		ServerLevel targetLevel = source.getServer().getLevel(targetDimKey);
		if (targetLevel == null) {
			source.sendFailure(Component.translatable("command.dragonminez.locate.dimension_not_found", targetDimKey.location()));
			return 0;
		}

		String playerDim = source.getLevel().dimension().location().toString();

		if (!targetDimKey.location().toString().equals(playerDim)) {
			source.sendFailure(Component.translatable("command.dragonminez.locate.wrong_dimension"));
			return 0;
		}

		BlockPos foundPos = StructureLocator.locateStructure(targetLevel, structureKey, BlockPos.containing(source.getPosition()));

		if (foundPos == null) {
			source.sendFailure(Component.translatable("command.dragonminez.locate.not_found", structName));
			return 0;
		}

		final BlockPos finalPos = foundPos;
		int distance = (source.getLevel() == targetLevel) ? StructureLocator.getDistanceTo(BlockPos.containing(source.getPosition()), finalPos) : -1;

		Component coordComponent = ComponentUtils.wrapInSquareBrackets(
				Component.literal(finalPos.getX() + ", " + finalPos.getY() + ", " + finalPos.getZ())
		).withStyle(style -> style
				.withColor(ChatFormatting.GREEN)
				.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + targetDimKey.location() + " run tp @s " + finalPos.getX() + " " + finalPos.getY() + " " + finalPos.getZ()))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click -> TP")))
		);

		source.sendSuccess(() -> Component.translatable(
				"command.dragonminez.locate.success",
				Component.literal(structName).withStyle(ChatFormatting.YELLOW),
				coordComponent,
				distance == -1 ? "?" : distance
		), false);

		return 1;
	}
}
