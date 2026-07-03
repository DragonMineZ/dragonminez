package com.dragonminez.server.world.structure.placement;

import com.dragonminez.common.config.ConfigManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * RandomSpread placement that behaves like vanilla {@link RandomSpreadStructurePlacement} but with
 * two DMZ-specific traits:
 * <ul>
 *   <li>{@code spacing}/{@code separation} are read from the server config at runtime (blocks -&gt;
 *       chunks), so owners can retune rarity without a datapack override, and they are not capped at
 *       vanilla's 4096-chunk codec limit.</li>
 *   <li>Generation is gated by {@code generateCustomStructures}, matching the other DMZ placements.</li>
 * </ul>
 * It is used for the far, repeating copies of a structure; the guaranteed copy near spawn is handled
 * separately by {@link BiomeAwareUniquePlacement}. It <b>extends</b> {@link RandomSpreadStructurePlacement}
 * on purpose: {@code ChunkGenerator.findNearestMapStructure} (used by {@code /locate}, {@code /dmzlocate}
 * and structure-map mods) only walks placements that are {@code instanceof RandomSpreadStructurePlacement},
 * so extending it keeps a custom serialized type while staying visible to that machinery.
 */
public class WideRandomSpreadPlacement extends RandomSpreadStructurePlacement {
	public static final Codec<WideRandomSpreadPlacement> CODEC = RecordCodecBuilder.create(instance ->
			placementCodec(instance).and(
					RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR)
							.forGetter(RandomSpreadStructurePlacement::spreadType)
			).apply(instance, WideRandomSpreadPlacement::new));

	// Fallback spacing/separation handed to super; never actually read, since spacing()/separation()
	// below are overridden to pull live values from the config. Kept valid (spacing > separation).
	private static final int FALLBACK_SPACING = 1875;
	private static final int FALLBACK_SEPARATION = 750;

	public WideRandomSpreadPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod,
									 float frequency, int salt, Optional<ExclusionZone> exclusionZone,
									 RandomSpreadType spreadType) {
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone,
				FALLBACK_SPACING, FALLBACK_SEPARATION, spreadType);
	}

	public WideRandomSpreadPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod,
									 float frequency, int salt, Optional<ExclusionZone> exclusionZone) {
		this(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, RandomSpreadType.LINEAR);
	}

	private static int chunksFromBlocks(int blocks) {
		return Math.max(1, blocks >> 4);
	}

	/** Region size in chunks, derived from {@code structureSpacing} (blocks). Overrides super's field. */
	@Override
	public int spacing() {
		return chunksFromBlocks(ConfigManager.getServerConfig().getWorldGen().getStructureSpacingBlocks());
	}

	/** Minimum gap in chunks kept from a region's far edge; always &lt; {@link #spacing()}. */
	@Override
	public int separation() {
		int spacing = spacing();
		int separation = chunksFromBlocks(ConfigManager.getServerConfig().getWorldGen().getStructureSeparationBlocks());
		return Math.min(separation, spacing - 1);
	}

	public int placementSalt() {
		return this.salt();
	}

	/**
	 * Deterministic chunk chosen for the region owning ({@code chunkX}, {@code chunkZ}). Fully
	 * reimplemented (not delegated to super) because super reads its private {@code spacing} field
	 * directly, which would ignore our config-driven {@link #spacing()}.
	 */
	@Override
	public @NonNull ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
		int spacing = spacing();
		int range = spacing - separation();

		int regionX = Math.floorDiv(chunkX, spacing);
		int regionZ = Math.floorDiv(chunkZ, spacing);

		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureWithSalt(seed, regionX, regionZ, this.salt());

		int offsetX = this.spreadType().evaluate(random, range);
		int offsetZ = this.spreadType().evaluate(random, range);

		return new ChunkPos(regionX * spacing + offsetX, regionZ * spacing + offsetZ);
	}

	@Override
	protected boolean isPlacementChunk(@NonNull ChunkGeneratorStructureState structureState, int x, int z) {
		if (!ConfigManager.getServerConfig().getWorldGen().getGenerateCustomStructures()) return false;
		ChunkPos pos = getPotentialStructureChunk(structureState.getLevelSeed(), x, z);
		return pos.x == x && pos.z == z;
	}

	@Override
	public @NonNull StructurePlacementType<?> type() {
		return MainStructurePlacements.WIDE_RANDOM_SPREAD.get();
	}
}
