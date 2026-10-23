package com.dragonminez.mixin.common;

import com.dragonminez.server.world.structure.placement.IncrementalStructure;
import com.dragonminez.server.world.structure.placement.StructureRepairManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
	@Shadow @Final private static Logger LOGGER;

	@Inject(method = "unpackStructureReferences", at = @At("HEAD"), cancellable = true)
	private static void dragonminez$keepIncrementalReferences(RegistryAccess registryAccess, ChunkPos pos, CompoundTag tag,
															  CallbackInfoReturnable<Map<Structure, LongSet>> cir) {
		Registry<Structure> registry = registryAccess.registryOrThrow(Registries.STRUCTURE);
		CompoundTag references = tag.getCompound("References");
		if (!dragonminez$hasDistantIncremental(registry, pos, references)) return;

		Map<Structure, LongSet> result = new HashMap<>();
		for (String key : references.getAllKeys()) {
			ResourceLocation id = ResourceLocation.tryParse(key);
			Structure structure = registry.get(id);
			if (structure == null) {
				LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", id, pos);
				continue;
			}
			long[] packed = references.getLongArray(key);
			if (packed.length == 0) continue;
			int range = dragonminez$referenceRange(structure);
			LongSet kept = new LongOpenHashSet(packed.length);
			for (long origin : packed) {
				ChunkPos start = new ChunkPos(origin);
				if (start.getChessboardDistance(pos) > range) {
					LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", id, start, pos);
				} else {
					kept.add(origin);
				}
			}
			result.put(structure, kept);
		}
		cir.setReturnValue(result);
	}

	private static boolean dragonminez$hasDistantIncremental(Registry<Structure> registry, ChunkPos pos, CompoundTag references) {
		for (String key : references.getAllKeys()) {
			if (!(registry.get(ResourceLocation.tryParse(key)) instanceof IncrementalStructure)) continue;
			for (long origin : references.getLongArray(key)) {
				int distance = new ChunkPos(origin).getChessboardDistance(pos);
				if (distance > 8 && distance <= StructureRepairManager.COMPLETION_RANGE) return true;
			}
		}
		return false;
	}

	private static int dragonminez$referenceRange(Structure structure) {
		return structure instanceof IncrementalStructure ? StructureRepairManager.COMPLETION_RANGE : 8;
	}
}
