package com.dragonminez.common.init.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class FlyingBlockParticle extends TerrainParticle {

	private static final float MIN_SCALE = 3.0F;
	private static final float SCALE_SPREAD = 2.0F;
	private static final int MIN_LIFETIME = 30;
	private static final int LIFETIME_SPREAD = 15;

	private FlyingBlockParticle(ClientLevel level, double x, double y, double z,
								double xd, double yd, double zd, BlockState state, BlockPos pos) {
		super(level, x, y, z, xd, yd, zd, state, pos);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.gravity = 1.0F;
		this.lifetime = MIN_LIFETIME + this.random.nextInt(LIFETIME_SPREAD);
		this.scale(MIN_SCALE + this.random.nextFloat() * SCALE_SPREAD);
	}

	public static class Provider implements ParticleProvider<BlockParticleOption> {
		@Override
		public Particle createParticle(BlockParticleOption options, ClientLevel level,
									   double x, double y, double z, double xd, double yd, double zd) {
			BlockState state = options.getState();
			if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) return null;
			return new FlyingBlockParticle(level, x, y, z, xd, yd, zd, state, BlockPos.containing(x, y, z));
		}
	}
}
