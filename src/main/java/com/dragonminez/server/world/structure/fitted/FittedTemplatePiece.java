package com.dragonminez.server.world.structure.fitted;

import com.dragonminez.mixin.common.StructureTemplateAccessor;
import com.dragonminez.mixin.common.StructureTemplatePaletteInvoker;
import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.dragonminez.server.world.structure.processor.ExteriorAirProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

public class FittedTemplatePiece extends TemplateStructurePiece {
	public static final String ENTITY_KEY_TAG = "dmz_structure_entity";
	private static final double TWIN_RADIUS = 4.0D;
	private static final int GAP_MARGIN = 8;
	private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	private final Rotation rotation;

	public FittedTemplatePiece(StructureTemplateManager templates, ResourceLocation location, BlockPos origin, Rotation rotation) {
		super(MainStructureTypes.FITTED_TEMPLATE_PIECE.get(), 0, templates, location, location.toString(),
				settings(templates.getOrCreate(location), rotation), origin);
		this.rotation = rotation;
	}

	public FittedTemplatePiece(StructureTemplateManager templates, CompoundTag tag) {
		super(MainStructureTypes.FITTED_TEMPLATE_PIECE.get(), tag, templates,
				location -> settings(templates.getOrCreate(location), Rotation.valueOf(tag.getString("Rot"))));
		this.rotation = Rotation.valueOf(tag.getString("Rot"));
	}

	private static StructurePlaceSettings settings(StructureTemplate template, Rotation rotation) {
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setKnownShape(true)
				.setIgnoreEntities(true)
				.addProcessor(new ExteriorAirProcessor(TemplateShape.of(template)))
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
				.addProcessor(JigsawReplacementProcessor.INSTANCE);
	}

	public ResourceLocation templateId() {
		return this.makeTemplateLocation();
	}

	public BlockPos origin() {
		return this.templatePosition;
	}

	public Rotation rotation() {
		return this.rotation;
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", this.rotation.name());
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
							RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		StructurePlaceSettings settings = settings(this.template, this.rotation).setBoundingBox(box);
		List<StructureTemplate.StructureBlockInfo> blocks = this.blocksWithin(settings, box);
		if (!blocks.isEmpty()) {
			StructureTemplate clipped = new StructureTemplate();
			((StructureTemplateAccessor) clipped).getPalettes().add(StructureTemplatePaletteInvoker.dragonminez$create(blocks));
			((StructureTemplateAccessor) clipped).setSize(this.template.getSize());
			clipped.placeInWorld(level, this.templatePosition, pivot, settings, random, PLACE_FLAGS);
		}
		this.clearGaps(level, generator, box);
		this.placeEntities(level, box);
	}

	private void clearGaps(WorldGenLevel level, ChunkGenerator generator, BoundingBox box) {
		TemplateShape shape = TemplateShape.of(this.template);
		if (!shape.hasGaps()) return;
		boolean live = level instanceof ServerLevel;
		int minX = Math.max(box.minX() - GAP_MARGIN, this.boundingBox.minX());
		int maxX = Math.min(box.maxX() + GAP_MARGIN, this.boundingBox.maxX());
		int minZ = Math.max(box.minZ() - GAP_MARGIN, this.boundingBox.minZ());
		int maxZ = Math.min(box.maxZ() + GAP_MARGIN, this.boundingBox.maxZ());
		int minY = Math.max(Math.max(box.minY(), this.boundingBox.minY()), generator.getSeaLevel());
		int maxY = Math.min(box.maxY(), this.boundingBox.maxY());
		if (minX > maxX || minZ > maxZ || minY > maxY) return;
		Rotation inverse = inverse(this.rotation);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				BlockPos local = StructureTemplate.transform(new BlockPos(x - this.templatePosition.getX(), 0, z - this.templatePosition.getZ()),
						Mirror.NONE, inverse, BlockPos.ZERO);
				int lowest = shape.lowestAt(local.getX(), local.getZ());
				if (lowest < 0) continue;
				boolean neighbour = x < box.minX() || x > box.maxX() || z < box.minZ() || z > box.maxZ();
				for (int y = Math.max(minY, this.templatePosition.getY() + lowest); y <= maxY; y++) {
					if (!shape.isGap(local.getX(), y - this.templatePosition.getY(), local.getZ())) continue;
					BlockState state = level.getBlockState(cursor.set(x, y, z));
					if (state.isAir()) continue;
					if (live && neighbour && !isNaturalLeaves(state)) continue;
					level.setBlock(cursor, AIR, PLACE_FLAGS);
				}
			}
		}
	}

	private static boolean isNaturalLeaves(BlockState state) {
		return state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
	}

	private List<StructureTemplate.StructureBlockInfo> blocksWithin(StructurePlaceSettings settings, BoundingBox box) {
		List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) this.template).getPalettes();
		if (palettes.isEmpty()) return List.of();
		StructureTemplate.Palette palette = settings.getRandomPalette(palettes, this.templatePosition);
		Rotation inverse = inverse(this.rotation);
		BlockPos a = StructureTemplate.transform(new BlockPos(box.minX() - this.templatePosition.getX(), 0, box.minZ() - this.templatePosition.getZ()),
				Mirror.NONE, inverse, BlockPos.ZERO);
		BlockPos b = StructureTemplate.transform(new BlockPos(box.maxX() - this.templatePosition.getX(), 0, box.maxZ() - this.templatePosition.getZ()),
				Mirror.NONE, inverse, BlockPos.ZERO);
		return TemplateColumns.of(palette, this.template.getSize()).within(
				Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), Math.max(a.getZ(), b.getZ()));
	}

	private static Rotation inverse(Rotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
			case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
			default -> rotation;
		};
	}

	private void placeEntities(WorldGenLevel level, BoundingBox box) {
		List<StructureTemplate.StructureEntityInfo> entities = ((StructureTemplateAccessor) this.template).getEntityInfoList();
		Vec3 origin = Vec3.atLowerCornerOf(this.templatePosition);
		for (int i = 0; i < entities.size(); i++) {
			StructureTemplate.StructureEntityInfo info = entities.get(i);
			BlockPos blockPos = StructureTemplate.transform(info.blockPos, Mirror.NONE, this.rotation, BlockPos.ZERO).offset(this.templatePosition);
			if (!box.isInside(blockPos)) continue;
			String key = this.templateName + "@" + this.templatePosition.asLong() + "#" + i;
			Vec3 pos = StructureTemplate.transform(info.pos, Mirror.NONE, this.rotation, BlockPos.ZERO).add(origin);
			if (isPlaced(level, pos, key)) continue;

			CompoundTag tag = info.nbt.copy();
			ListTag position = new ListTag();
			position.add(DoubleTag.valueOf(pos.x));
			position.add(DoubleTag.valueOf(pos.y));
			position.add(DoubleTag.valueOf(pos.z));
			tag.put("Pos", position);
			tag.remove("UUID");
			create(level, tag).ifPresent(entity -> {
				float yaw = entity.rotate(this.rotation);
				entity.moveTo(pos.x, pos.y, pos.z, yaw, entity.getXRot());
				entity.getPersistentData().putString(ENTITY_KEY_TAG, key);
				if (entity instanceof Mob mob) {
					mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), MobSpawnType.STRUCTURE, null);
				}
				level.addFreshEntityWithPassengers(entity);
			});
		}
	}

	private static Optional<Entity> create(WorldGenLevel level, CompoundTag tag) {
		try {
			return EntityType.create(tag, level.getLevel());
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	private static boolean isPlaced(WorldGenLevel level, Vec3 pos, String key) {
		BlockPos block = BlockPos.containing(pos);
		ChunkAccess chunk = level.getChunk(block);
		if (chunk instanceof ProtoChunk proto) {
			try {
				for (CompoundTag saved : new ArrayList<>(proto.getEntities())) {
					if (key.equals(saved.getCompound("ForgeData").getString(ENTITY_KEY_TAG))) return true;
				}
			} catch (ConcurrentModificationException e) {
				return false;
			}
			return false;
		}
		return !level.getEntitiesOfClass(Entity.class, new AABB(block).inflate(TWIN_RADIUS),
				entity -> key.equals(entity.getPersistentData().getString(ENTITY_KEY_TAG))).isEmpty();
	}

	public static boolean hasTwin(ServerLevel level, Entity entity) {
		String key = entity.getPersistentData().getString(ENTITY_KEY_TAG);
		if (key.isEmpty()) return false;
		return !level.getEntitiesOfClass(Entity.class, entity.getBoundingBox().inflate(TWIN_RADIUS),
				other -> other != entity && !other.isRemoved() && key.equals(other.getPersistentData().getString(ENTITY_KEY_TAG))).isEmpty();
	}

	@Override
	protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
	}
}
