package com.dragonminez.common.init.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VehicleSmokeParticle extends TextureSheetParticle {

	private static final float GROWTH = 1.8F;
	private static final float FADE_IN = 0.12F;

	private final float baseSize;
	private final float baseAlpha;

	protected VehicleSmokeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, VehicleSmokeOptions options, SpriteSet sprites) {
		super(level, x, y, z);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.baseSize = options.size();
		this.baseAlpha = Mth.clamp(options.alpha(), 0.0F, 1.0F);
		this.quadSize = this.baseSize;
		this.lifetime = Math.max(4, options.lifetime() + this.random.nextInt(Math.max(1, options.lifetime() / 4)));
		this.gravity = -0.004F;
		this.friction = 0.94F;
		this.hasPhysics = false;
		float shade = Mth.clamp(options.shade() + (this.random.nextFloat() - 0.5F) * 0.08F, 0.0F, 1.0F);
		this.rCol = shade;
		this.gCol = shade;
		this.bCol = shade;
		this.alpha = 0.0F;
		this.roll = this.random.nextFloat() * Mth.TWO_PI;
		this.oRoll = this.roll;
		this.pickSprite(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		float life = (float) this.age / this.lifetime;
		this.quadSize = this.baseSize * (1.0F + GROWTH * (1.0F - (1.0F - life) * (1.0F - life)));
		float fade = life < FADE_IN ? life / FADE_IN : 1.0F - (life - FADE_IN) / (1.0F - FADE_IN);
		this.alpha = this.baseAlpha * Mth.clamp(fade, 0.0F, 1.0F);
		this.oRoll = this.roll;
		this.roll += 0.02F;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@OnlyIn(Dist.CLIENT)
	public static class Provider implements ParticleProvider<VehicleSmokeOptions> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(VehicleSmokeOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
			return new VehicleSmokeParticle(level, x, y, z, xd, yd, zd, options, this.sprites);
		}
	}
}
