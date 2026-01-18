package com.dragonminez.common.init.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class KintonParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected KintonParticle(ClientLevel level, double x, double y, double z, double r, double g, double b, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);

        this.spriteSet = spriteSet;

        this.xd = (Math.random() * 2.0D - 1.0D) * 0.05D;
        this.yd = (Math.random() * 2.0D - 1.0D) * 0.05D;
        this.zd = (Math.random() * 2.0D - 1.0D) * 0.05D;

        this.lifetime = 14 + this.random.nextInt(4);
        this.quadSize = 0.45F;
        this.gravity = 0.0F;
        this.hasPhysics = false;

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
            this.xd *= 0.95D;
            this.yd *= 0.95D;
            this.zd *= 0.95D;

            if (this.age > this.lifetime * 0.8) {
                this.alpha *= 0.9F;
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return super.getLightColor(pPartialTick);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double rParam, double gParam, double bParam) {
            int colorHex = (int) rParam;

            // Desempaquetamos el Hex a R, G, B (0.0 a 1.0)
            double r = ((colorHex >> 16) & 0xFF) / 255.0;
            double g = ((colorHex >> 8) & 0xFF) / 255.0;
            double b = (colorHex & 0xFF) / 255.0;

            // Creamos la partícula con los colores ya separados
            return new KintonParticle(level, x, y, z, r, g, b, this.spriteSet);
        }
    }
}