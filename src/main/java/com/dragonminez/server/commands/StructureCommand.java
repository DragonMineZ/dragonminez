package com.dragonminez.server.commands;

import com.dragonminez.server.world.npc.NPCPlacementSavedData;
import com.dragonminez.server.world.npc.StructureNpcs;
import com.dragonminez.server.world.structure.helper.StructureFrame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StructureCommand {

	private record Found(ResourceLocation id, StructureStart start, StructureFrame frame) {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzstructure")
				.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STRUCTURE))
				.then(Commands.literal("here").executes(context -> here(context.getSource())))
				.then(Commands.literal("npcs").executes(context -> npcs(context.getSource()))));
	}

	private static List<Found> structuresAt(ServerLevel level, BlockPos pos) {
		var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		List<Found> found = new ArrayList<>();
		for (Structure structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
			StructureStart start = level.structureManager().getStructureAt(pos, structure);
			if (!start.isValid()) continue;
			ResourceLocation id = registry.getKey(structure);
			if (id == null) continue;
			List<StructureFrame> frames = StructureFrame.all(start);
			if (frames.isEmpty()) continue;
			StructureFrame frame = frames.stream()
					.filter(candidate -> candidate.box().inflatedBy(2).isInside(pos))
					.findFirst()
					.orElse(frames.get(0));
			found.add(new Found(id, start, frame));
		}
		return found;
	}

	private static int here(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		List<Found> found = structuresAt(source.getLevel(), player.blockPosition());
		if (found.isEmpty()) {
			source.sendFailure(Component.translatable("command.dragonminez.structure.none"));
			return 0;
		}
		for (Found entry : found) {
			StructureFrame frame = entry.frame();
			Vec3 local = frame.toLocal(player.position());
			double x = Math.floor(local.x) + 0.5D;
			double y = Math.floor(local.y * 2.0D + 1.0E-4D) / 2.0D;
			double z = Math.floor(local.z) + 0.5D;
			float yaw = Math.round(frame.toLocalYaw(player.getYRot()));
			String template = frame.template() == null ? "?" : frame.template().toString();
			String coords = String.format(Locale.ROOT, "%.1f %.1f %.1f", x, y, z);
			String snippet = String.format(Locale.ROOT,
					"{\"id\": \"npc_new\", \"entity\": \"dragonminez:quest_npc\", \"npc_id\": \"\", %s\"pos\": [%.1f, %.1f, %.1f], \"yaw\": %.1f}",
					frame.template() == null ? "" : "\"template\": \"" + template + "\", ", x, y, z, yaw);
			MutableComponent copy = ComponentUtils.wrapInSquareBrackets(Component.translatable("command.dragonminez.structure.copy"))
					.withStyle(style -> style.withColor(ChatFormatting.GREEN)
							.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, snippet))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(snippet))));
			source.sendSuccess(() -> Component.translatable("command.dragonminez.structure.here",
					Component.literal(entry.id().toString()).withStyle(ChatFormatting.YELLOW),
					template, coords, String.format(Locale.ROOT, "%.0f", yaw)).append(" ").append(copy), false);
		}
		return found.size();
	}

	private static int npcs(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getLevel();
		List<Found> found = structuresAt(level, player.blockPosition());
		if (found.isEmpty()) {
			source.sendFailure(Component.translatable("command.dragonminez.structure.none"));
			return 0;
		}
		NPCPlacementSavedData tracking = NPCPlacementSavedData.get(level);
		int shown = 0;
		for (Found entry : found) {
			List<StructureNpcs.Definition> definitions = StructureNpcs.definitionsFor(entry.id());
			if (definitions.isEmpty()) {
				source.sendSuccess(() -> Component.translatable("command.dragonminez.structure.npcs_none", entry.id().toString()), false);
				continue;
			}
			for (StructureNpcs.Definition definition : definitions) {
				Vec3 pos = StructureNpcs.resolve(entry.start(), definition).orElse(null);
				if (pos == null) continue;
				String coords = String.format(Locale.ROOT, "%.1f %.1f %.1f", pos.x, pos.y, pos.z);
				MutableComponent location = ComponentUtils.wrapInSquareBrackets(Component.literal(coords))
						.withStyle(style -> style.withColor(ChatFormatting.GREEN)
								.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
										"/execute in " + level.dimension().location() + " run tp @s " + coords)));
				Component state = Component.translatable(tracking.hasPlacement(definition.trackingKey(entry.start()))
						? "command.dragonminez.structure.npc_spawned"
						: "command.dragonminez.structure.npc_pending");
				source.sendSuccess(() -> Component.translatable("command.dragonminez.structure.npc", definition.id(), location, state), false);
				shown++;
			}
		}
		return shown;
	}
}
