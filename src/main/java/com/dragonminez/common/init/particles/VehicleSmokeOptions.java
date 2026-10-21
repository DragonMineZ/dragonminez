package com.dragonminez.common.init.particles;

import com.dragonminez.common.init.MainParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

public record VehicleSmokeOptions(float size, float shade, float alpha, int lifetime) implements ParticleOptions {

	public static final Codec<VehicleSmokeOptions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.fieldOf("size").forGetter(VehicleSmokeOptions::size),
			Codec.FLOAT.fieldOf("shade").forGetter(VehicleSmokeOptions::shade),
			Codec.FLOAT.fieldOf("alpha").forGetter(VehicleSmokeOptions::alpha),
			Codec.INT.fieldOf("lifetime").forGetter(VehicleSmokeOptions::lifetime)
	).apply(instance, VehicleSmokeOptions::new));

	@SuppressWarnings("deprecation")
	public static final ParticleOptions.Deserializer<VehicleSmokeOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
		@Override
		public VehicleSmokeOptions fromCommand(ParticleType<VehicleSmokeOptions> type, StringReader reader) throws CommandSyntaxException {
			reader.expect(' ');
			float size = reader.readFloat();
			reader.expect(' ');
			float shade = reader.readFloat();
			reader.expect(' ');
			float alpha = reader.readFloat();
			reader.expect(' ');
			int lifetime = reader.readInt();
			return new VehicleSmokeOptions(size, shade, alpha, lifetime);
		}

		@Override
		public VehicleSmokeOptions fromNetwork(ParticleType<VehicleSmokeOptions> type, FriendlyByteBuf buffer) {
			return new VehicleSmokeOptions(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt());
		}
	};

	@Override
	public ParticleType<?> getType() {
		return MainParticles.VEHICLE_SMOKE.get();
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buffer) {
		buffer.writeFloat(size);
		buffer.writeFloat(shade);
		buffer.writeFloat(alpha);
		buffer.writeVarInt(lifetime);
	}

	@Override
	public String writeToString() {
		return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %d", ForgeRegistries.PARTICLE_TYPES.getKey(getType()), size, shade, alpha, lifetime);
	}
}
