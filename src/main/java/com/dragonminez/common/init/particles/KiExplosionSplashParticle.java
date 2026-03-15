package com.dragonminez.common.init.particles;

import com.dragonminez.client.util.ColorUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class KiExplosionSplashParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
    private float baseScale;

    protected KiExplosionSplashParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet spriteSet) {
        super(level, x, y, z, dx, dy, dz);
        this.spriteSet = spriteSet;

        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;

        this.lifetime = 20;
        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.baseScale = 1.0F;
        this.quadSize = this.baseScale;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 0.7F;

        this.setSpriteFromAge(spriteSet);
    }

    public void setSplashScale(float scale) {
        this.baseScale = scale;
        this.quadSize = this.baseScale;
    }

    public void setSplashColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
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
            this.quadSize = this.baseScale + (this.baseScale * ((float)this.age / this.lifetime));
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
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new KiExplosionSplashParticle(level, x, y, z, dx, dy, dz, this.spriteSet);
        }
    }
}