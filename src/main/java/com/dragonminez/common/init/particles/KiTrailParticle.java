package com.dragonminez.common.init.particles;

import com.dragonminez.client.render.util.ModParticleRenderTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class KiTrailParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected KiTrailParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);

        this.spriteSet = spriteSet;

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.quadSize = 0.05F;

        this.lifetime = 25 + (int)(Math.random() * 10);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.alpha = 1.0F;

        this.setSpriteFromAge(spriteSet);
    }


    public void setKiColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    public void setKiScale(float entityScale) {
        this.quadSize = entityScale * (0.1F + (float)Math.random() * 0.3F);
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

            this.xd *= 0.90D;
            this.yd *= 0.90D;
            this.zd *= 0.90D;

            this.quadSize *= 0.97F;

            float lifeRatio = 1.0F - ((float)this.age / (float)this.lifetime);
            this.alpha = lifeRatio;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ModParticleRenderTypes.ADDITIVE_LIT;
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 15728880;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        public Provider(SpriteSet spriteSet) { this.spriteSet = spriteSet; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new KiTrailParticle(level, x, y, z, vx, vy, vz, this.spriteSet);
        }
    }
}