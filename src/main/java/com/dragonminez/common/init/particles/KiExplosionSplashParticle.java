package com.dragonminez.common.init.particles;

import com.dragonminez.client.util.ColorUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class KiExplosionSplashParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected KiExplosionSplashParticle(ClientLevel level, double x, double y, double z, double sizeData, double colorData, double ignored, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);

        this.spriteSet = spriteSet;

        this.lifetime = 10;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.quadSize = (float) sizeData * 2.0F;

        int colorHex = (int) colorData;
        float[] rgb = ColorUtils.rgbIntToFloat(colorHex);
        this.rCol = rgb[0];
        this.gCol = rgb[1];
        this.bCol = rgb[2];
        this.alpha = 0.7F;

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