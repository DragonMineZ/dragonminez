package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.server.world.structure.TallJigsawStructure;
import com.dragonminez.server.world.structure.DemonVillageStructure;
import com.dragonminez.server.world.structure.BossStructures.SaiyanCraterStructure;
import com.dragonminez.server.world.structure.BossStructures.GeteStarStructure;
import com.dragonminez.server.world.structure.BossStructures.GomahCampStructure;
import com.dragonminez.server.world.structure.BossStructures.GomahCradleStructure;
import com.dragonminez.server.world.structure.BossStructures.NamekRuinsStructure;
import com.dragonminez.server.world.structure.BossStructures.TreeOfMightPiece;
import com.dragonminez.server.world.structure.BossStructures.TreeOfMightStructure;
import com.dragonminez.server.world.structure.fitted.FittedTemplatePiece;
import com.dragonminez.server.world.structure.fitted.FittedTemplateStructure;
import com.dragonminez.server.world.structure.fitted.TerrainFitPiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class MainStructureTypes {
	public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
			DeferredRegister.create(Registries.STRUCTURE_TYPE, Reference.MOD_ID);
	public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
			DeferredRegister.create(Registries.STRUCTURE_PIECE, Reference.MOD_ID);

	public static final RegistryObject<StructureType<TallJigsawStructure>> TALL_JIGSAW =
			STRUCTURE_TYPES.register("tall_jigsaw", () -> () -> TallJigsawStructure.CODEC);
	public static final RegistryObject<StructureType<FittedTemplateStructure>> FITTED_TEMPLATE =
			STRUCTURE_TYPES.register("fitted_template", () -> () -> FittedTemplateStructure.CODEC);
	public static final RegistryObject<StructureType<TreeOfMightStructure>> TREE_OF_MIGHT =
			STRUCTURE_TYPES.register("tree_of_might", () -> () -> TreeOfMightStructure.CODEC);

	public static final RegistryObject<StructureType<SaiyanCraterStructure>> SAIYAN_CRATER =
			STRUCTURE_TYPES.register("saiyan_crater", () -> () -> SaiyanCraterStructure.CODEC);
	public static final RegistryObject<StructureType<NamekRuinsStructure>> NAMEK_RUINS =
			STRUCTURE_TYPES.register("namek_ruins", () -> () -> NamekRuinsStructure.CODEC);
	public static final RegistryObject<StructureType<GeteStarStructure>> GETE_STAR =
			STRUCTURE_TYPES.register("gete_star", () -> () -> GeteStarStructure.CODEC);
	public static final RegistryObject<StructureType<DemonVillageStructure>> DEMON_VILLAGE =
			STRUCTURE_TYPES.register("demon_village", () -> () -> DemonVillageStructure.CODEC);
	public static final RegistryObject<StructureType<GomahCampStructure>> GOMAH_CAMP =
			STRUCTURE_TYPES.register("gomah_camp", () -> () -> GomahCampStructure.CODEC);
	public static final RegistryObject<StructureType<GomahCradleStructure>> GOMAH_CRADLE =
			STRUCTURE_TYPES.register("gomah_cradle", () -> () -> GomahCradleStructure.CODEC);

	public static final RegistryObject<StructurePieceType> FITTED_TEMPLATE_PIECE =
			STRUCTURE_PIECES.register("fitted_template", () -> (StructurePieceType.StructureTemplateType) FittedTemplatePiece::new);
	public static final RegistryObject<StructurePieceType> TERRAIN_FIT_PIECE =
			STRUCTURE_PIECES.register("terrain_fit", () -> (StructurePieceType.ContextlessType) TerrainFitPiece::new);
	public static final RegistryObject<StructurePieceType> TREE_OF_MIGHT_PIECE =
			STRUCTURE_PIECES.register("tree_of_might", () -> (StructurePieceType.ContextlessType) TreeOfMightPiece::new);
	public static final RegistryObject<StructurePieceType> SAIYAN_CRATER_PIECE =
			STRUCTURE_PIECES.register("saiyan_crater", () -> (StructurePieceType.ContextlessType) SaiyanCraterStructure.Piece::new);
	public static final RegistryObject<StructurePieceType> NAMEK_RUINS_PLAZA =
			STRUCTURE_PIECES.register("namek_ruins_plaza", () -> (StructurePieceType.ContextlessType) NamekRuinsStructure.PlazaPiece::new);
	public static final RegistryObject<StructurePieceType> NAMEK_RUINS_HOUSE =
			STRUCTURE_PIECES.register("namek_ruins_house", () -> (StructurePieceType.StructureTemplateType) NamekRuinsStructure.HousePiece::new);
	public static final RegistryObject<StructurePieceType> GETE_STAR_PIECE =
			STRUCTURE_PIECES.register("gete_star", () -> (StructurePieceType.ContextlessType) GeteStarStructure.Piece::new);
	public static final RegistryObject<StructurePieceType> DEMON_VILLAGE_BUILDING =
			STRUCTURE_PIECES.register("demon_village_building", () -> (StructurePieceType.StructureTemplateType) DemonVillageStructure.BuildingPiece::new);
	public static final RegistryObject<StructurePieceType> DEMON_VILLAGE_GROUND =
			STRUCTURE_PIECES.register("demon_village_ground", () -> (StructurePieceType.ContextlessType) DemonVillageStructure.GroundPiece::new);
	public static final RegistryObject<StructurePieceType> GOMAH_CAMP_PIECE =
			STRUCTURE_PIECES.register("gomah_camp", () -> (StructurePieceType.ContextlessType) GomahCampStructure.CampPiece::new);
	public static final RegistryObject<StructurePieceType> GOMAH_CRADLE_PIECE =
			STRUCTURE_PIECES.register("gomah_cradle", () -> (StructurePieceType.ContextlessType) GomahCradleStructure.Piece::new);

	public static void register(IEventBus eventBus) {
		STRUCTURE_TYPES.register(eventBus);
		STRUCTURE_PIECES.register(eventBus);
	}
}
