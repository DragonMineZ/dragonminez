package com.dragonminez.common.init.particles;

import com.dragonminez.client.util.ColorUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class KiSplashWaveParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;
    private final float growthRate;

    protected KiSplashWaveParticle(ClientLevel level, double x, double y, double z, double colorData, double sizeData, double unused, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);

        this.spriteSet = spriteSet;

        this.lifetime = 10;
        this.gravity = 0.0F;
        this.hasPhysics = false;

        int color = (int) colorData;

        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;

        this.alpha = 1.0F;

        this.quadSize = (float) sizeData;

        this.growthRate = this.quadSize * 0.15F;

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
            this.quadSize += this.growthRate;

            if (this.age > 5) {
                this.alpha -= 0.1F;
            }
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
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double colorData, double sizeData, double unused) {
            return new KiSplashWaveParticle(level, x, y, z, colorData, sizeData, unused, this.spriteSet);
        }
    }
}