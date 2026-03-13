package com.dragonminez.common.init.particles;

import com.dragonminez.client.util.ModParticleRenderTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class KiSheddingParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected KiSheddingParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);
        this.spriteSet = spriteSet;

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;

        this.lifetime = 8;
        this.quadSize = 0.3F + (float)Math.random() * 0.2F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
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

            this.quadSize *= 0.94F;
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

    public void setKiColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        public Provider(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new KiSheddingParticle(level, x, y, z, vx, vy, vz, this.spriteSet);
        }
    }
}