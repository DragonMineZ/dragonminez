package com.dragonminez.common.init.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DustParticle extends TextureSheetParticle {

    protected DustParticle(ClientLevel level, double x, double y, double z, double velX, double velY, double velZ) {
        super(level, x, y, z, velX, velY, velZ);

        this.xd = velX;
        this.yd = 0;
        this.zd = velZ;

        this.quadSize *= 1.5f;
        this.lifetime = 40 + this.random.nextInt(20);
        this.gravity = 0f;

        this.rCol = 1f;
        this.gCol = 1f;
        this.bCol = 1f;
    }

    @Override
    public void tick() {
        super.tick();
        this.setAlpha(1.0f - ((float) this.age / this.lifetime));

        this.xd *= 0.95;
        this.zd *= 0.95;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // IMPORTANTE: Esto permite transparencias en tu textura PNG
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            DustParticle particle = new DustParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}