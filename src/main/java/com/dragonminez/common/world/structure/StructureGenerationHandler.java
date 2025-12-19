package com.dragonminez.common.world.structure;

import com.dragonminez.common.world.capability.StructureSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class StructureGenerationHandler {

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			ChunkAccess chunk = event.getChunk();

			chunk.getAllStarts().forEach((structure, structureStart) -> {
				if (!structureStart.isValid()) return;

				String structureId = structureStart.getStructure().type().toString();
				String structureName = serverLevel.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey(structure).toString();

				if (structureName.equals("dragonminez:goku_house") || structureName.equals("dragonminez:roshi_house")) {

					StructureSavedData data = StructureSavedData.get(serverLevel.getServer().overworld());
					String cleanName = structureName.contains("goku") ? "goku_house" : "roshi_house";

					if (data.getPositions().getStructurePosition(cleanName) == null) {
						BoundingBox bb = structureStart.getBoundingBox();
						BlockPos center = new BlockPos(bb.getCenter().getX(), bb.minY(), bb.getCenter().getZ());

						data.getPositions().setStructurePosition(cleanName, center);
						data.setDirty();
					}
				}
			});
		}
	}
}