package com.dragonminez.common.init.particles;

import com.dragonminez.client.util.ModParticleRenderTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class KiLightningParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected KiLightningParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.lifetime = 3 + this.random.nextInt(6);

        this.roll = this.random.nextFloat() * ((float)Math.PI * 2F);
        this.oRoll = this.roll;

        this.hasPhysics = false;

        this.setSpriteFromAge(sprites);
    }

    public void setLightningColor(float r, float g, float b) {
        this.setColor(r, g, b);
    }

    public void setLightningScale(float size) {
        this.scale(size);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);

            this.move(this.xd, this.yd, this.zd);

            this.xd *= 0.8D;
            this.yd *= 0.8D;
            this.zd *= 0.8D;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ModParticleRenderTypes.ADDITIVE_LIT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new KiLightningParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}