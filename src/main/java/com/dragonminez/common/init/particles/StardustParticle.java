package com.dragonminez.common.init.particles;

import com.dragonminez.client.render.util.ModParticleRenderTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StardustParticle extends TextureSheetParticle {

    private static final int[] TINTS = {0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xECE2FF, 0xDDF7FF, 0xFFE6F7};
    private static final int FULL_BRIGHT = 0xF000F0;

    private final float baseSize;
    private final float twinkleSpeed;
    private final float twinklePhase;

    protected StardustParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.friction = 0.9F;
        this.gravity = 0.02F;
        this.hasPhysics = false;

        int tint = TINTS[this.random.nextInt(TINTS.length)];
        this.rCol = ((tint >> 16) & 0xFF) / 255.0F;
        this.gCol = ((tint >> 8) & 0xFF) / 255.0F;
        this.bCol = (tint & 0xFF) / 255.0F;

        this.baseSize = this.random.nextFloat() < 0.15F
                ? 0.24F + this.random.nextFloat() * 0.12F
                : 0.08F + this.random.nextFloat() * 0.12F;
        this.quadSize = this.baseSize;
        this.twinkleSpeed = 0.35F + this.random.nextFloat() * 0.45F;
        this.twinklePhase = this.random.nextFloat() * Mth.TWO_PI;
        this.lifetime = 30 + this.random.nextInt(26);
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;

        float life = (float) this.age / this.lifetime;
        float fadeIn = Math.min(1.0F, this.age / 3.0F);
        float fadeOut = 1.0F - life * life;
        float twinkle = 0.55F + 0.45F * Mth.sin(this.age * this.twinkleSpeed + this.twinklePhase);
        this.alpha = fadeIn * fadeOut * twinkle;
        this.quadSize = this.baseSize * (0.7F + 0.4F * twinkle);
    }

    @Override
    public int getLightColor(float partialTick) {
        return FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ModParticleRenderTypes.ADDITIVE_LIT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            StardustParticle particle = new StardustParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}
