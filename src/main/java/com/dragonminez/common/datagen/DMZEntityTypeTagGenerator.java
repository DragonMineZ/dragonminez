package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DMZEntityTypeTagGenerator extends EntityTypeTagsProvider {
	public DMZEntityTypeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
									 @Nullable ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, Reference.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.@NotNull Provider provider) {
		this.tag(MainTags.EntityTypes.FRIEZA_SOLDIERS)
				.add(MainEntities.SAGA_FRIEZA_SOLDIER.get())
				.add(MainEntities.SAGA_FRIEZA_SOLDIER2.get())
				.add(MainEntities.SAGA_FRIEZA_SOLDIER3.get());
	}
}
