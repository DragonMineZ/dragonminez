package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.server.world.structure.TallJigsawStructure;
import com.dragonminez.server.world.structure.tree.TreeOfMightPiece;
import com.dragonminez.server.world.structure.tree.TreeOfMightStructure;
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
	public static final RegistryObject<StructureType<TreeOfMightStructure>> TREE_OF_MIGHT =
			STRUCTURE_TYPES.register("tree_of_might", () -> () -> TreeOfMightStructure.CODEC);

	public static final RegistryObject<StructurePieceType> TREE_OF_MIGHT_PIECE =
			STRUCTURE_PIECES.register("tree_of_might", () -> (StructurePieceType.ContextlessType) TreeOfMightPiece::new);

	public static void register(IEventBus eventBus) {
		STRUCTURE_TYPES.register(eventBus);
		STRUCTURE_PIECES.register(eventBus);
	}
}
