package com.dragonminez.common.init.particles;

import com.dragonminez.common.init.MainParticles;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record VehicleSmokeOptions(float size, float shade, float alpha, int lifetime) implements ParticleOptions {

	public static final MapCodec<VehicleSmokeOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.FLOAT.fieldOf("size").forGetter(VehicleSmokeOptions::size),
			Codec.FLOAT.fieldOf("shade").forGetter(VehicleSmokeOptions::shade),
			Codec.FLOAT.fieldOf("alpha").forGetter(VehicleSmokeOptions::alpha),
			Codec.INT.fieldOf("lifetime").forGetter(VehicleSmokeOptions::lifetime)
	).apply(instance, VehicleSmokeOptions::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, VehicleSmokeOptions> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, VehicleSmokeOptions::size,
			ByteBufCodecs.FLOAT, VehicleSmokeOptions::shade,
			ByteBufCodecs.FLOAT, VehicleSmokeOptions::alpha,
			ByteBufCodecs.VAR_INT, VehicleSmokeOptions::lifetime,
			VehicleSmokeOptions::new);

	@Override
	public ParticleType<?> getType() {
		return MainParticles.VEHICLE_SMOKE.get();
	}

}
