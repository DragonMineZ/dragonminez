package com.dragonminez.server.world.structure.fitted;

import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.dragonminez.server.world.structure.helper.StructureFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

public class TerrainFitPiece extends StructurePiece {
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	private final ResourceLocation template;
	private final BlockPos origin;
	private final Rotation rotation;
	private final int groundLevel;
	private final int skirt;
	private final float slope;
	private final int foundation;
	private final int carve;
	private final boolean templateFoundation;

	public TerrainFitPiece(ResourceLocation template, SitePlan site, TerrainFit fit) {
		super(MainStructureTypes.TERRAIN_FIT_PIECE.get(), 0, footprint(site));
		this.template = template;
		this.origin = site.origin();
		this.rotation = site.rotation();
		this.groundLevel = site.groundLevel();
		this.skirt = site.skirt();
		this.slope = fit.slope();
		this.foundation = fit.foundation();
		this.carve = fit.carve();
		this.templateFoundation = fit.templateFoundation();
	}

	public TerrainFitPiece(CompoundTag tag) {
		super(MainStructureTypes.TERRAIN_FIT_PIECE.get(), tag);
		this.template = ResourceLocation.parse(tag.getString("Template"));
		this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
		this.rotation = Rotation.valueOf(tag.getString("Rot"));
		this.groundLevel = tag.getInt("Ground");
		this.skirt = tag.getInt("Skirt");
		this.slope = tag.getFloat("Slope");
		this.foundation = tag.getInt("Foundation");
		this.carve = tag.getInt("Carve");
		this.templateFoundation = tag.getBoolean("TemplateFoundation");
	}

	private static BoundingBox footprint(SitePlan site) {
		BoundingBox box = site.templateBox();
		int y = site.origin().getY();
		return new BoundingBox(box.minX() - site.skirt(), y, box.minZ() - site.skirt(),
				box.maxX() + site.skirt(), y, box.maxZ() + site.skirt());
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putString("Template", this.template.toString());
		tag.putInt("OX", this.origin.getX());
		tag.putInt("OY", this.origin.getY());
		tag.putInt("OZ", this.origin.getZ());
		tag.putString("Rot", this.rotation.name());
		tag.putInt("Ground", this.groundLevel);
		tag.putInt("Skirt", this.skirt);
		tag.putFloat("Slope", this.slope);
		tag.putInt("Foundation", this.foundation);
		tag.putInt("Carve", this.carve);
		tag.putBoolean("TemplateFoundation", this.templateFoundation);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
							RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		StructureTemplate structureTemplate = level.getLevel().getStructureManager().getOrCreate(this.template);
		TemplateShape shape = TemplateShape.of(structureTemplate);
		if (shape.sizeX() <= 0 || shape.sizeZ() <= 0) return;
		TemplateShape.SkirtField field = shape.field(this.groundLevel, this.skirt);
		Rotation inverse = StructureFrame.inverse(this.rotation);

		int minX = Math.max(box.minX(), this.boundingBox.minX());
		int maxX = Math.min(box.maxX(), this.boundingBox.maxX());
		int minZ = Math.max(box.minZ(), this.boundingBox.minZ());
		int maxZ = Math.min(box.maxZ(), this.boundingBox.maxZ());
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				BlockPos local = StructureTemplate.transform(new BlockPos(x - this.origin.getX(), 0, z - this.origin.getZ()),
						Mirror.NONE, inverse, BlockPos.ZERO);
				int cell = field.cell(local.getX(), local.getZ());
				if (cell < 0) continue;
				this.fitColumn(level, cursor, shape, field, cell, x, z);
			}
		}
	}

	private void fitColumn(WorldGenLevel level, BlockPos.MutableBlockPos cursor, TemplateShape shape,
						   TemplateShape.SkirtField field, int cell, int x, int z) {
		Profile profile = Profile.scan(level, cursor, x, z);
		if (profile == null) return;
		int column = field.source(cell);
		int support = this.origin.getY() + field.support(column);
		float distance = field.distance(cell);
		int target = this.target(profile.top(), support, field.fills(column), distance, x, z);
		if (target > profile.top()) {
			BlockState material = this.templateFoundation && distance <= 0.0F ? this.baseMaterial(shape, column) : null;
			this.fill(level, cursor, x, z, profile, target, material);
		} else if (target < profile.top()) {
			this.carve(level, cursor, x, z, profile, target);
		}
	}

	private int target(int top, int support, boolean fills, float distance, int x, int z) {
		if (top > support) {
			if (this.carve <= 0) return top;
			if (distance <= 0.0F) return support;
			return Math.min(top, support + this.reach(distance, top - support, x, z));
		}
		if (top < support && fills) {
			if (this.foundation <= 0) return top;
			if (distance <= 0.0F) return support;
			return Math.max(top, support - this.reach(distance, support - top, x, z));
		}
		return top;
	}

	private int reach(float distance, int delta, int x, int z) {
		float rate = Math.max(this.slope * jitter(x, z), delta / (float) Math.max(1, this.skirt));
		return (int) (distance * rate);
	}

	@Nullable
	private BlockState baseMaterial(TemplateShape shape, int column) {
		BlockState state = shape.baseState(column);
		return state == null || !state.blocksMotion() ? null : state.rotate(this.rotation);
	}

	private void fill(WorldGenLevel level, BlockPos.MutableBlockPos cursor, int x, int z, Profile profile, int target,
					  @Nullable BlockState material) {
		int start = Math.max(profile.top() + 1, target - this.foundation);
		boolean skin = !profile.surface().is(profile.under().getBlock());
		BlockState surface = material != null ? material
				: skin && target <= profile.fluidTop() ? profile.under() : profile.surface();
		for (int y = start; y <= target; y++) {
			BlockState state = material != null ? material
					: y == target ? surface : target - y <= 3 ? profile.under() : profile.deep();
			level.setBlock(cursor.set(x, y, z), state, 2);
		}
		if (material == null && skin && start == profile.top() + 1) {
			level.setBlock(cursor.set(x, profile.top(), z), profile.under(), 2);
		}
	}

	private void carve(WorldGenLevel level, BlockPos.MutableBlockPos cursor, int x, int z, Profile profile, int target) {
		int end = Math.min(profile.scanTop(), target + this.carve);
		for (int y = target + 1; y <= end; y++) {
			cursor.set(x, y, z);
			if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, AIR, 2);
		}
		if (target <= profile.fluidTop()) return;
		BlockState current = level.getBlockState(cursor.set(x, target, z));
		if (isGround(current) && current != profile.surface()) level.setBlock(cursor, profile.surface(), 2);
		if (profile.surface().is(profile.under().getBlock())) return;
		for (int y = target - 1; y >= target - 3; y--) {
			current = level.getBlockState(cursor.set(x, y, z));
			if (isGround(current)) level.setBlock(cursor, profile.under(), 2);
		}
	}

	static boolean isGround(BlockState state) {
		if (!state.blocksMotion() || !state.getFluidState().isEmpty()) return false;
		return !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS) && !state.is(BlockTags.ICE)
				&& !state.is(Blocks.CACTUS) && !state.is(Blocks.BAMBOO) && !state.is(Blocks.MUSHROOM_STEM)
				&& !state.is(Blocks.BROWN_MUSHROOM_BLOCK) && !state.is(Blocks.RED_MUSHROOM_BLOCK);
	}

	private static float jitter(int x, int z) {
		int cx = Math.floorDiv(x, 8);
		int cz = Math.floorDiv(z, 8);
		float fx = (x - cx * 8) / 8.0F;
		float fz = (z - cz * 8) / 8.0F;
		fx = fx * fx * (3.0F - 2.0F * fx);
		fz = fz * fz * (3.0F - 2.0F * fz);
		float top = Mth.lerp(fx, hash(cx, cz), hash(cx + 1, cz));
		float bottom = Mth.lerp(fx, hash(cx, cz + 1), hash(cx + 1, cz + 1));
		return 0.8F + Mth.lerp(fz, top, bottom) * 0.4F;
	}

	private static float hash(int x, int z) {
		long h = x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL;
		h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		return (h >>> 40) / (float) (1L << 24);
	}

	private record Profile(int top, int fluidTop, int scanTop, BlockState surface, BlockState under, BlockState deep) {
		@Nullable
		static Profile scan(WorldGenLevel level, BlockPos.MutableBlockPos cursor, int x, int z) {
			int scanTop = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
			int bottom = level.getMinBuildHeight();
			int fluidTop = Integer.MIN_VALUE;
			for (int y = scanTop; y >= bottom; y--) {
				BlockState state = level.getBlockState(cursor.set(x, y, z));
				if (state.isAir()) continue;
				if (isGround(state)) {
					BlockState under = groundAt(level, cursor, x, y - 1, z, state);
					BlockState deep = groundAt(level, cursor, x, y - 5, z, under);
					return new Profile(y, fluidTop, scanTop, state, under, deep);
				}
				if (fluidTop == Integer.MIN_VALUE && (!state.getFluidState().isEmpty() || state.is(BlockTags.ICE))) fluidTop = y;
			}
			return null;
		}

		private static BlockState groundAt(WorldGenLevel level, BlockPos.MutableBlockPos cursor, int x, int y, int z, BlockState fallback) {
			if (y < level.getMinBuildHeight()) return fallback;
			BlockState state = level.getBlockState(cursor.set(x, y, z));
			return isGround(state) ? state : fallback;
		}
	}
}
