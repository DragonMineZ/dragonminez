package com.dragonminez.server.world.structure.helper;

import com.dragonminez.mixin.common.SinglePoolElementAccessor;
import com.dragonminez.server.world.structure.fitted.FittedTemplatePiece;
import com.dragonminez.server.world.structure.fitted.TerrainFitPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record StructureFrame(@Nullable ResourceLocation template, BlockPos origin, Rotation rotation, Mirror mirror,
							 BlockPos pivot, BoundingBox box) {

	public static List<StructureFrame> all(StructureStart start) {
		List<StructureFrame> frames = new ArrayList<>();
		for (StructurePiece piece : start.getPieces()) {
			StructureFrame frame = of(piece);
			if (frame != null) frames.add(frame);
		}
		return frames;
	}

	public static Optional<StructureFrame> core(StructureStart start) {
		for (StructurePiece piece : start.getPieces()) {
			StructureFrame frame = of(piece);
			if (frame != null) return Optional.of(frame);
		}
		return Optional.empty();
	}

	public static Optional<StructureFrame> forTemplate(StructureStart start, @Nullable ResourceLocation template) {
		if (template == null) return core(start);
		for (StructureFrame frame : all(start)) {
			if (template.equals(frame.template())) return Optional.of(frame);
		}
		return Optional.empty();
	}

	public static BoundingBox coreBox(StructureStart start) {
		List<BoundingBox> boxes = new ArrayList<>();
		for (StructurePiece piece : start.getPieces()) {
			if (!(piece instanceof TerrainFitPiece)) boxes.add(piece.getBoundingBox());
		}
		return BoundingBox.encapsulatingBoxes(boxes).orElseGet(start::getBoundingBox);
	}

	@Nullable
	public static StructureFrame of(StructurePiece piece) {
		if (piece instanceof FittedTemplatePiece fitted) {
			StructurePlaceSettings settings = fitted.placeSettings();
			return new StructureFrame(fitted.templateId(), fitted.templatePosition(), settings.getRotation(),
					settings.getMirror(), settings.getRotationPivot(), piece.getBoundingBox());
		}
		if (piece instanceof TemplateStructurePiece template) {
			StructurePlaceSettings settings = template.placeSettings();
			return new StructureFrame(null, template.templatePosition(), settings.getRotation(),
					settings.getMirror(), settings.getRotationPivot(), piece.getBoundingBox());
		}
		if (piece instanceof PoolElementStructurePiece pool) {
			ResourceLocation id = pool.getElement() instanceof SinglePoolElement single
					? ((SinglePoolElementAccessor) single).getTemplateSource().left().orElse(null)
					: null;
			return new StructureFrame(id, pool.getPosition(), pool.getRotation(), Mirror.NONE, BlockPos.ZERO, piece.getBoundingBox());
		}
		return null;
	}

	public static Rotation inverse(Rotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
			case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
			default -> rotation;
		};
	}

	private static float degrees(Rotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_90 -> 90.0F;
			case CLOCKWISE_180 -> 180.0F;
			case COUNTERCLOCKWISE_90 -> 270.0F;
			default -> 0.0F;
		};
	}

	private float mirrorYaw(float yaw) {
		return switch (this.mirror) {
			case LEFT_RIGHT -> 180.0F - yaw;
			case FRONT_BACK -> -yaw;
			default -> yaw;
		};
	}

	public Vec3 toWorld(Vec3 local) {
		Vec3 rotated = StructureTemplate.transform(local, this.mirror, this.rotation, this.pivot);
		return rotated.add(this.origin.getX(), this.origin.getY(), this.origin.getZ());
	}

	public Vec3 toLocal(Vec3 world) {
		Vec3 relative = world.subtract(this.origin.getX(), this.origin.getY(), this.origin.getZ());
		Vec3 unrotated = StructureTemplate.transform(relative, Mirror.NONE, inverse(this.rotation), this.pivot);
		return this.mirror == Mirror.NONE ? unrotated : StructureTemplate.transform(unrotated, this.mirror, Rotation.NONE, BlockPos.ZERO);
	}

	public BlockPos toWorld(BlockPos local) {
		return StructureTemplate.transform(local, this.mirror, this.rotation, this.pivot).offset(this.origin);
	}

	public BlockPos toLocal(BlockPos world) {
		BlockPos unrotated = StructureTemplate.transform(world.subtract(this.origin), Mirror.NONE, inverse(this.rotation), this.pivot);
		return this.mirror == Mirror.NONE ? unrotated : StructureTemplate.transform(unrotated, this.mirror, Rotation.NONE, BlockPos.ZERO);
	}

	public float toWorldYaw(float yaw) {
		return Mth.wrapDegrees(this.mirrorYaw(yaw) + degrees(this.rotation));
	}

	public float toLocalYaw(float yaw) {
		return Mth.wrapDegrees(this.mirrorYaw(yaw - degrees(this.rotation)));
	}
}
