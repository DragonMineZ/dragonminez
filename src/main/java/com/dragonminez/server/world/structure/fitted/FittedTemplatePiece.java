package com.dragonminez.server.world.structure.fitted;

import com.dragonminez.mixin.common.StructureTemplateAccessor;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", this.rotation.name());
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
							RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		this.template.placeInWorld(level, this.templatePosition, pivot, settings(this.template, this.rotation).setBoundingBox(box), random, 2);
		this.placeEntities(level, box);
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
					mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), MobSpawnType.STRUCTURE, null, tag);
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
