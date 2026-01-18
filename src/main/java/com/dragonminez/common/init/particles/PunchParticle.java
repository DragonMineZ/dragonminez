package com.dragonminez.common.init.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class PunchParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;
    private final float initialAlpha;

    protected PunchParticle(ClientLevel level, double x, double y, double z, double r, double g, double b, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);

        this.spriteSet = spriteSet;

        this.lifetime = 10;
        this.quadSize = 0.8F;
        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.rCol = (float) r;
        this.gCol = (float) g;
        this.bCol = (float) b;

        this.initialAlpha = 0.8F;
        this.alpha = this.initialAlpha;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.spriteSet);
            this.quadSize += 0.05F;

            float lifeRatio = (float)this.age / (float)this.lifetime;

            this.alpha = this.initialAlpha * (1.0F - lifeRatio);

            if (this.alpha < 0) this.alpha = 0;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 15728880;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        public Provider(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double r, double g, double b) {
            return new PunchParticle(level, x, y, z, r, g, b, this.spriteSet);
        }
    }
}