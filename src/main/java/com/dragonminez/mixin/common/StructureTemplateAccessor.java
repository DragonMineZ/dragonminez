package com.dragonminez.mixin.common;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor {
	@Accessor("palettes")
	List<StructureTemplate.Palette> getPalettes();

	@Accessor("entityInfoList")
	List<StructureTemplate.StructureEntityInfo> getEntityInfoList();

	@Accessor("size")
	void setSize(Vec3i size);
}
