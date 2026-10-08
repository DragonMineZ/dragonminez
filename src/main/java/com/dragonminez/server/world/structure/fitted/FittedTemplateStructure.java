package com.dragonminez.server.world.structure.fitted;

import com.dragonminez.server.world.structure.helper.MainStructureTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FittedTemplateStructure extends Structure {
	public static final Codec<FittedTemplateStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			settingsCodec(instance),
			ResourceLocation.CODEC.fieldOf("template").forGetter(FittedTemplateStructure::template),
			TerrainFit.CODEC.optionalFieldOf("terrain_fit", TerrainFit.NONE).forGetter(structure -> structure.fit),
			Rotation.CODEC.optionalFieldOf("rotation").forGetter(structure -> structure.fixedRotation)
	).apply(instance, FittedTemplateStructure::new));

	private static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};
	private static final int REFERENCE_REACH = 120;
	private static final int MAX_FALLBACK_SAMPLES = 128;

	private final ResourceLocation template;
	private final TerrainFit fit;
	private final Optional<Rotation> fixedRotation;

	public FittedTemplateStructure(StructureSettings settings, ResourceLocation template, TerrainFit fit, Optional<Rotation> fixedRotation) {
		super(settings);
		this.template = template;
		this.fit = fit;
		this.fixedRotation = fixedRotation;
	}

	public ResourceLocation template() {
		return this.template;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		StructureTemplateManager templates = context.structureTemplateManager();
		HeightSampler sampler = HeightSampler.direct(context.chunkGenerator(), context.heightAccessor(), context.randomState());
		return this.plan(templates, sampler, context.chunkPos())
				.map(site -> new GenerationStub(site.center(), builder -> this.addPieces(builder, templates, site)));
	}

	private void addPieces(StructurePiecesBuilder builder, StructureTemplateManager templates, SitePlan site) {
		if (this.fit.active()) builder.addPiece(new TerrainFitPiece(this.template, site, this.fit));
		builder.addPiece(new FittedTemplatePiece(templates, this.template, site.origin(), site.rotation()));
	}

	public Optional<SitePlan> plan(StructureTemplateManager templates, HeightSampler sampler, ChunkPos chunk) {
		return this.plan(templates, sampler, chunk, this.fixedRotation.map(rotation -> new Rotation[]{rotation}).orElse(ROTATIONS), 1);
	}

	public Optional<SitePlan> estimate(StructureTemplateManager templates, HeightSampler sampler, ChunkPos chunk) {
		return this.plan(templates, sampler, chunk, new Rotation[]{this.fixedRotation.orElse(Rotation.NONE)}, 4);
	}

	private Optional<SitePlan> plan(StructureTemplateManager templates, HeightSampler sampler, ChunkPos chunk,
									Rotation[] rotations, int coarseness) {
		StructureTemplate structureTemplate = templates.getOrCreate(this.template);
		Vec3i size = structureTemplate.getSize();
		if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) return Optional.empty();

		TemplateShape shape = TemplateShape.of(structureTemplate);
		int ground = this.fit.resolveGroundLevel(shape);
		int[][] samples = samplePoints(shape, ground, coarseness);

		SitePlan best = null;
		for (Rotation rotation : rotations) {
			SitePlan site = this.evaluate(structureTemplate, ground, rotation, sampler, chunk, samples);
			if (best == null || site.deviation() < best.deviation() - 1.0E-6D) best = site;
		}
		return Optional.ofNullable(best);
	}

	private SitePlan evaluate(StructureTemplate structureTemplate, int ground, Rotation rotation, HeightSampler sampler,
							  ChunkPos chunk, int[][] samples) {
		StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
		BoundingBox local = structureTemplate.getBoundingBox(settings, BlockPos.ZERO);
		int centerX = chunk.getMiddleBlockX();
		int centerZ = chunk.getMiddleBlockZ();
		int originX = centerX - Math.floorDiv(local.minX() + local.maxX(), 2);
		int originZ = centerZ - Math.floorDiv(local.minZ() + local.maxZ(), 2);

		int count = samples.length;
		int[] xs = new int[count];
		int[] zs = new int[count];
		int[] tops = new int[count];
		int wet = 0;
		int highestWater = Integer.MIN_VALUE;
		for (int i = 0; i < count; i++) {
			BlockPos relative = StructureTemplate.transform(new BlockPos(samples[i][0], 0, samples[i][1]), Mirror.NONE, rotation, BlockPos.ZERO);
			xs[i] = originX + relative.getX();
			zs[i] = originZ + relative.getZ();
			long packed = sampler.sample(xs[i], zs[i]);
			tops[i] = HeightSampler.floor(packed);
			int surface = HeightSampler.surface(packed);
			if (surface > tops[i]) {
				wet++;
				highestWater = Math.max(highestWater, surface);
			}
		}

		int[] sorted = tops.clone();
		Arrays.sort(sorted);
		int groundY = sorted[count / 2];
		if (wet > 0) groundY = Math.max(groundY, highestWater + 1);
		groundY += this.fit.yOffset();

		double deviation = 0.0D;
		for (int top : tops) deviation += Math.abs(top - groundY);
		deviation /= count;

		BlockPos origin = new BlockPos(originX, groundY - ground, originZ);
		BoundingBox box = structureTemplate.getBoundingBox(settings, origin);
		int reach = Math.max(Math.max(centerX - box.minX(), box.maxX() - centerX), Math.max(centerZ - box.minZ(), box.maxZ() - centerZ));
		int skirt = Math.max(0, Math.min(this.fit.skirt(), REFERENCE_REACH - reach));
		return new SitePlan(rotation, origin, new BlockPos(centerX, groundY, centerZ), box, ground, skirt,
				deviation, wet / (float) count, xs, zs, tops);
	}

	private static int[][] samplePoints(TemplateShape shape, int ground, int coarseness) {
		int sx = shape.sizeX();
		int sz = shape.sizeZ();
		int min = Math.min(sx, sz);
		int step = (min >= 40 ? 8 : min >= 16 ? 4 : 2) * coarseness;
		List<int[]> points = new ArrayList<>();
		for (int z = step / 2; z < sz; z += step) {
			for (int x = step / 2; x < sx; x += step) {
				if (shape.isGrounded(x, z, ground)) points.add(new int[]{x, z});
			}
		}
		if (points.isEmpty()) {
			int total = shape.groundedCount(ground);
			int stride = Math.max(1, total / MAX_FALLBACK_SAMPLES);
			int seen = 0;
			for (int z = 0; z < sz; z++) {
				for (int x = 0; x < sx; x++) {
					if (!shape.isGrounded(x, z, ground)) continue;
					if (seen++ % stride == 0) points.add(new int[]{x, z});
				}
			}
		}
		if (points.isEmpty()) points.add(new int[]{sx / 2, sz / 2});
		return points.toArray(new int[0][]);
	}

	@Override
	public StructureType<?> type() {
		return MainStructureTypes.FITTED_TEMPLATE.get();
	}
}
