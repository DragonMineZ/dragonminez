package com.dragonminez.server.world.structure;

import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.Optional;

public class TallJigsawStructure extends Structure {
	public static final Codec<TallJigsawStructure> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					settingsCodec(instance),
					StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
					ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
					Codec.intRange(0, 20).fieldOf("size").forGetter(s -> s.maxDepth),
					HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
					Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
					Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
					Codec.intRange(1, 512).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
					Codec.INT.optionalFieldOf("min_start_y", Integer.MIN_VALUE).forGetter(s -> s.minStartY)
			).apply(instance, TallJigsawStructure::new));

	private final Holder<StructureTemplatePool> startPool;
	private final Optional<ResourceLocation> startJigsawName;
	private final int maxDepth;
	private final HeightProvider startHeight;
	private final boolean useExpansionHack;
	private final Optional<Heightmap.Types> projectStartToHeightmap;
	private final int maxDistanceFromCenter;
	private final int minStartY;

	public TallJigsawStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool,
							   Optional<ResourceLocation> startJigsawName, int maxDepth, HeightProvider startHeight,
							   boolean useExpansionHack, Optional<Heightmap.Types> projectStartToHeightmap,
							   int maxDistanceFromCenter, int minStartY) {
		super(settings);
		this.startPool = startPool;
		this.startJigsawName = startJigsawName;
		this.maxDepth = maxDepth;
		this.startHeight = startHeight;
		this.useExpansionHack = useExpansionHack;
		this.projectStartToHeightmap = projectStartToHeightmap;
		this.maxDistanceFromCenter = maxDistanceFromCenter;
		this.minStartY = minStartY;
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunkPos = context.chunkPos();
		int x = chunkPos.getMinBlockX();
		int z = chunkPos.getMinBlockZ();
		int offset = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));

		int startY;
		if (this.projectStartToHeightmap.isPresent()) {
			startY = context.chunkGenerator().getFirstFreeHeight(x, z, this.projectStartToHeightmap.get(),
					context.heightAccessor(), context.randomState()) + offset;
		} else {
			startY = offset;
		}
		if (startY < this.minStartY) startY = this.minStartY;

		BlockPos start = new BlockPos(x, startY, z);
		return JigsawPlacement.addPieces(context, this.startPool, this.startJigsawName,
				this.maxDepth, start, this.useExpansionHack, Optional.empty(), this.maxDistanceFromCenter);
	}

	@Override
	public StructureType<?> type() {
		return MainStructureTypes.TALL_JIGSAW.get();
	}
}
