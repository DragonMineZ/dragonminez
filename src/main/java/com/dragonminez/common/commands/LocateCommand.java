package com.dragonminez.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class LocateCommand {

	private enum StructureConfig {
		GOKU("goku_house", 12345678, "minecraft:has_structure/village_plains", Level.OVERWORLD),
		ROSHI("roshi_house", 87654321, "minecraft:is_ocean", Level.OVERWORLD),

		// GURU("guru_house", 11223344, "dragonminez:is_namek", ResourceKey.create(Registries.DIMENSION, new ResourceLocation("dragonminez:namek")))
		;

		final String name;
		final int salt;
		final String biomeTag;
		final ResourceKey<Level> dimensionKey;

		StructureConfig(String name, int salt, String biomeTag, ResourceKey<Level> dimensionKey) {
			this.name = name;
			this.salt = salt;
			this.biomeTag = biomeTag;
			this.dimensionKey = dimensionKey;
		}

		static StructureConfig byName(String name) {
			for (StructureConfig cfg : values()) if (cfg.name.equals(name)) return cfg;
			return null;
		}
	}

	private static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(new String[]{"goku_house", "roshi_house"}, builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzlocate")
				.requires(source -> source.hasPermission(2))
				.then(Commands.argument("structure", StringArgumentType.string())
						.suggests(SUGGESTIONS)
						.executes(context -> {
							String structName = StringArgumentType.getString(context, "structure");
							return locateStructure(context.getSource(), structName);
						})));
	}

	private static int locateStructure(CommandSourceStack source, String structName) {
		ServerLevel level = source.getLevel();
		long worldSeed = level.getSeed();

		StructureConfig config = StructureConfig.byName(structName);

		if (config == null) {
			source.sendFailure(Component.translatable("command.dragonminez.locate.invalid", structName));
			return 0;
		}

		if (!level.dimension().equals(config.dimensionKey)) {
			source.sendFailure(Component.translatable("command.dragonminez.locate.wrong_dimension"));
			return 0;
		}

		TagKey<Biome> biomeTag = TagKey.create(Registries.BIOME, new ResourceLocation(config.biomeTag));
		ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
		RandomState randomState = level.getChunkSource().randomState();

		for (int attempt = 0; attempt < 10; attempt++) {
			WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(worldSeed + config.salt + attempt));

			int targetChunkX = random.nextInt(400) - 200;
			int targetChunkZ = random.nextInt(400) - 200;

			int quartX = QuartPos.fromBlock(targetChunkX * 16 + 8);
			int quartY = QuartPos.fromBlock(64);
			int quartZ = QuartPos.fromBlock(targetChunkZ * 16 + 8);

			Holder<Biome> biome = chunkGenerator.getBiomeSource().getNoiseBiome(quartX, quartY, quartZ, randomState.sampler());

			if (biome.is(biomeTag)) {
				int x = targetChunkX * 16 + 8;
				int z = targetChunkZ * 16 + 8;
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				if (y < 60) y = 90;

				BlockPos structurePos = new BlockPos(x, y, z);
				BlockPos playerPos = BlockPos.containing(source.getPosition());
				int distance = (int) Math.sqrt(playerPos.distSqr(structurePos));

				int finalY = y;
				Component coordComponent = ComponentUtils.wrapInSquareBrackets(
						Component.literal(x + ", " + y + ", " + z)
				).withStyle(style -> style
						.withColor(ChatFormatting.GREEN)
						.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + x + " " + finalY + " " + z))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
				);

				source.sendSuccess(() -> Component.translatable(
						"command.dragonminez.locate.success",
						Component.literal(structName).withStyle(ChatFormatting.YELLOW),
						coordComponent,
						distance
				), false);

				return 1;
			}
		}

		source.sendFailure(Component.translatable("command.dragonminez.locate.not_found", structName));
		return 0;
	}
}