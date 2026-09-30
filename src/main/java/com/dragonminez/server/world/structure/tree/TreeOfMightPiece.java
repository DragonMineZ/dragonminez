package com.dragonminez.server.world.structure.tree;

import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.HashMap;
import java.util.Map;

public class TreeOfMightPiece extends StructurePiece {
	private final int version;
	private final long seed;
	private final int centerX;
	private final int centerZ;
	private final int minBuildY;
	private final int maxBuildY;
	private final int seaLevel;
	private final long[] sampleKeys;
	private final int[] sampleValues;
	private volatile TreeOfMightShape shape;

	public TreeOfMightPiece(TreeOfMightShape shape) {
		super(MainStructureTypes.TREE_OF_MIGHT_PIECE.get(), 0,
				new BoundingBox(shape.minX(), shape.minY(), shape.minZ(), shape.maxX(), shape.maxY(), shape.maxZ()));
		this.version = TreeOfMightShape.VERSION;
		this.seed = shape.seed();
		this.centerX = shape.centerX();
		this.centerZ = shape.centerZ();
		this.minBuildY = shape.minBuildY();
		this.maxBuildY = shape.maxBuildY();
		this.seaLevel = shape.seaLevel();
		Map<Long, Integer> samples = shape.groundSamples();
		this.sampleKeys = new long[samples.size()];
		this.sampleValues = new int[samples.size()];
		int i = 0;
		for (Map.Entry<Long, Integer> entry : samples.entrySet()) {
			this.sampleKeys[i] = entry.getKey();
			this.sampleValues[i] = entry.getValue();
			i++;
		}
		this.shape = shape;
		TreeOfMightShapes.remember(shape);
	}

	public TreeOfMightPiece(CompoundTag tag) {
		super(MainStructureTypes.TREE_OF_MIGHT_PIECE.get(), tag);
		this.version = tag.getInt("ShapeVersion");
		this.seed = tag.getLong("Seed");
		this.centerX = tag.getInt("CenterX");
		this.centerZ = tag.getInt("CenterZ");
		this.minBuildY = tag.getInt("MinBuildY");
		this.maxBuildY = tag.getInt("MaxBuildY");
		this.seaLevel = tag.getInt("SeaLevel");
		this.sampleKeys = tag.getLongArray("SampleKeys");
		this.sampleValues = tag.getIntArray("SampleValues");
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("ShapeVersion", this.version);
		tag.putLong("Seed", this.seed);
		tag.putInt("CenterX", this.centerX);
		tag.putInt("CenterZ", this.centerZ);
		tag.putInt("MinBuildY", this.minBuildY);
		tag.putInt("MaxBuildY", this.maxBuildY);
		tag.putInt("SeaLevel", this.seaLevel);
		tag.putLongArray("SampleKeys", this.sampleKeys);
		tag.putIntArray("SampleValues", this.sampleValues);
	}

	public TreeOfMightShape shape() {
		TreeOfMightShape current = this.shape;
		if (current != null) return current;
		current = TreeOfMightShapes.cached(this.seed, this.centerX, this.centerZ);
		if (current == null) {
			Map<Long, Integer> samples = new HashMap<>();
			for (int i = 0; i < Math.min(this.sampleKeys.length, this.sampleValues.length); i++) {
				samples.put(this.sampleKeys[i], this.sampleValues[i]);
			}
			int fallback = samples.getOrDefault(TreeOfMightShape.pack(this.centerX, this.centerZ), this.seaLevel);
			current = TreeOfMightShape.create(this.centerX, this.centerZ, this.seed, this.minBuildY, this.maxBuildY, this.seaLevel,
					(x, z) -> samples.getOrDefault(TreeOfMightShape.pack(x, z), fallback));
			TreeOfMightShapes.remember(current);
		}
		this.shape = current;
		return current;
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
							RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		TreeOfMightWriter.place(level, shape(), chunkPos, box);
	}
}
