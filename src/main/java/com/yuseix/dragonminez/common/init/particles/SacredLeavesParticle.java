package com.yuseix.dragonminez.common.init.particles;

import com.yuseix.dragonminez.common.init.particles.particleoptions.SacredLeavesParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;

public class SacredLeavesParticle extends TextureSheetParticle {
	private final SpriteSet sprites; // Variable para almacenar el SpriteSet

	protected SacredLeavesParticle(ClientLevel level, double x, double y, double z, SpriteSet texture, double xSpeed, double ySpeed, double zSpeed) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);

		this.sprites = texture; // Asigna el SpriteSet a la variable
		this.setSpriteFromAge(this.sprites); // Usa el SpriteSet para asignar la textura inicial

		this.gravity = 0.1F; // Caída
		this.friction = 0.98F; // Qué tan lejos va
		this.quadSize *= 0.8F; // Tamaño
		this.lifetime = 80 + this.random.nextInt(40); // Cuanto tarda en despawnear (ticks)

		this.rCol = 1f;
		this.gCol = 1f;
		this.bCol = 1f;
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.sprites);
		fadeOut();
	}

	private void fadeOut() {
		this.alpha = (-(1 / (float) lifetime) * age + 1);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static class Provider implements ParticleProvider<SacredLeavesParticleOptions> {
		private final SpriteSet sprites;

		public Provider(SpriteSet spriteSet) {
			this.sprites = spriteSet;
		}

		@Override
		public Particle createParticle(SacredLeavesParticleOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new SacredLeavesParticle(level, x, y, z, this.sprites, xSpeed, ySpeed, zSpeed);
		}
	}
}
