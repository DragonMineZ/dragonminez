package com.dragonminez.server.world.structure.fitted;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TerrainFit(int groundLevel, int yOffset, int skirt, float slope, int foundation, int carve,
						 boolean templateFoundation) {
	public static final TerrainFit NONE = new TerrainFit(-1, 0, 0, 1.0F, 0, 0, false);

	public static final Codec<TerrainFit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(-1, 512).optionalFieldOf("ground_level", -1).forGetter(TerrainFit::groundLevel),
			Codec.intRange(-64, 64).optionalFieldOf("y_offset", 0).forGetter(TerrainFit::yOffset),
			Codec.intRange(0, 48).optionalFieldOf("skirt", 0).forGetter(TerrainFit::skirt),
			Codec.floatRange(0.25F, 4.0F).optionalFieldOf("slope", 1.0F).forGetter(TerrainFit::slope),
			Codec.intRange(0, 128).optionalFieldOf("foundation", 0).forGetter(TerrainFit::foundation),
			Codec.intRange(0, 128).optionalFieldOf("carve", 0).forGetter(TerrainFit::carve),
			Codec.BOOL.optionalFieldOf("template_foundation", false).forGetter(TerrainFit::templateFoundation)
	).apply(instance, TerrainFit::new));

	public static TerrainFit terrain(int groundLevel, int skirt, float slope, int foundation, int carve) {
		return new TerrainFit(groundLevel, 0, skirt, slope, foundation, carve, false);
	}

	public static TerrainFit pillar(int groundLevel, int skirt, int foundation, int carve) {
		return new TerrainFit(groundLevel, 0, skirt, 1.0F, foundation, carve, true);
	}

	public static TerrainFit flush(int groundLevel) {
		return new TerrainFit(groundLevel, 0, 0, 1.0F, 0, 0, false);
	}

	public boolean active() {
		return this.skirt > 0 || this.foundation > 0 || this.carve > 0;
	}

	public int resolveGroundLevel(TemplateShape shape) {
		return this.groundLevel >= 0 ? this.groundLevel : shape.autoGroundLevel();
	}
}
