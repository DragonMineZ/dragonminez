package com.dragonminez.common.init.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class AuraParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected AuraParticle(ClientLevel level, double x, double y, double z, double r, double g, double b, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);

        this.spriteSet = spriteSet;

        this.lifetime = 20 + this.random.nextInt(20);

        this.quadSize = 0.5F + this.random.nextFloat() * 0.5F;

        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.xd = (Math.random() * 2.0D - 1.0D) * 0.05D;
        this.zd = (Math.random() * 2.0D - 1.0D) * 0.05D;
        this.yd = 0.05D + (Math.random() * 0.05D);

        this.rCol = (float) r;
        this.gCol = (float) g;
        this.bCol = (float) b;
        this.alpha = 1.0F;

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

            this.move(this.xd, this.yd, this.zd);

            this.xd *= 0.9D;
            this.zd *= 0.9D;

            // Aceleración suave hacia arriba (opcional, para que suba más rápido al final)
            // this.yd += 0.005D;

            float lifeRatio = (float)this.age / (float)this.lifetime;
            this.alpha = 1.0F - lifeRatio;

            // if (lifeRatio > 0.8F) this.quadSize *= 0.9F;
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
            return new AuraParticle(level, x, y, z, r, g, b, this.spriteSet);
        }
    }
}