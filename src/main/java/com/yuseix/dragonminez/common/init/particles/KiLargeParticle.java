package com.yuseix.dragonminez.common.init.particles;

import com.yuseix.dragonminez.common.init.particles.particleoptions.KiLargeParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;

public class KiLargeParticle extends TextureSheetParticle {

	private final SpriteSet sprites; // Variable para almacenar el SpriteSet

	protected KiLargeParticle(ClientLevel level, double x, double y, double z, SpriteSet texture, double xSpeed, double ySpeed, double zSpeed, int color) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);

		this.sprites = texture; // Asigna el SpriteSet a la variable
		this.setSpriteFromAge(this.sprites); // Usa el SpriteSet para asignar la textura inicial

		this.gravity = 0F; // Caída
		this.friction = 0.8F; // Qué tan lejos va
		this.quadSize *= 2.8F; // Tamaño
		this.lifetime = 80; // Cuanto tarda en despawnear (ticks)

		// Configura el color de la partícula
		float r = ((color >> 16) & 0xFF) / 255.0F;
		float g = ((color >> 8) & 0xFF) / 255.0F;
		float b = (color & 0xFF) / 255.0F;
		this.setColor(r, g, b);
	}

	@Override
	public void tick() {
		super.tick();

		// Actualiza el sprite según la edad de la partícula
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

	// Proveedor (Provider) para KiTrailParticle
	public static class Provider implements ParticleProvider<KiLargeParticleOptions> {
		private final SpriteSet sprites;

		public Provider(SpriteSet spriteSet) {
			this.sprites = spriteSet;
		}

		@Override
		public Particle createParticle(KiLargeParticleOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new KiLargeParticle(level, x, y, z, this.sprites, xSpeed, ySpeed, zSpeed, type.getColor());
		}
	}
}