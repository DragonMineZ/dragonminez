package com.dragonminez.server.commands;

import com.dragonminez.Reference;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

public class LocateCommand {

	private static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(
					context.getSource().registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream()
							.filter(id -> id.getNamespace().equals(Reference.MOD_ID))
							.map(ResourceLocation::getPath),
					builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzlocate")
				.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.LOCATE))
				.then(Commands.argument("structure", StringArgumentType.string())
						.suggests(SUGGESTIONS)
						.executes(context -> locateByName(context.getSource(), StringArgumentType.getString(context, "structure")))));
	}

	private static int locateByName(CommandSourceStack source, String name) {
		ResourceLocation id = name.indexOf(':') >= 0
				? ResourceLocation.tryParse(name)
				: ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name);
		if (id == null || !source.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(id)) {
			source.sendFailure(Component.translatable("command.dragonminez.locate.invalid", name));
			return 0;
		}
		return locate(source, ResourceKey.create(Registries.STRUCTURE, id));
	}

	public static int locate(CommandSourceStack source, ResourceKey<Structure> structureKey) {
		ServerLevel level = source.getLevel();
		BlockPos from = BlockPos.containing(source.getPosition());
		BlockPos foundPos = StructureLocator.locateStructure(level, structureKey, from);
		String structName = structureKey.location().getPath();

		if (foundPos == null) {
			source.sendFailure(Component.translatable("command.dragonminez.locate.not_found", structName));
			return 0;
		}

		int distance = StructureLocator.getDistanceTo(from, foundPos);
		int tpY = (int) source.getPosition().y;
		Component coordComponent = ComponentUtils.wrapInSquareBrackets(
				Component.literal(foundPos.getX() + ", ~, " + foundPos.getZ())
		).withStyle(style -> style
				.withColor(ChatFormatting.GREEN)
				.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + level.dimension().location() + " run tp @s " + foundPos.getX() + " " + tpY + " " + foundPos.getZ()))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click -> TP")))
		);

		source.sendSuccess(() -> Component.translatable(
				"command.dragonminez.locate.success",
				Component.literal(structName).withStyle(ChatFormatting.YELLOW),
				coordComponent,
				distance), false);

		return 1;
	}
}
