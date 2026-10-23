package com.dragonminez.common.init.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SleepZParticle extends TextureSheetParticle {

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float START_SIZE = 0.10F;
    private static final float END_SIZE = 0.28F;
    private static final double RISE_SPEED = 0.028;
    private static final double SWAY = 0.012;

    private final double driftX;
    private final double driftZ;
    private final double swayX;
    private final double swayZ;
    private final float swayPhase;

    protected SleepZParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);

        this.driftX = xd;
        this.driftZ = zd;
        double drift = Math.sqrt(xd * xd + zd * zd);
        this.swayX = drift > 1.0E-6 ? -zd / drift : 1.0;
        this.swayZ = drift > 1.0E-6 ? xd / drift : 0.0;
        this.xd = xd;
        this.yd = RISE_SPEED;
        this.zd = zd;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.hasPhysics = false;

        this.swayPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.lifetime = 34 + this.random.nextInt(8);
        this.quadSize = START_SIZE;
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;

        float life = (float) this.age / this.lifetime;
        float sway = Mth.sin(this.age * 0.25F + this.swayPhase);
        this.xd = this.driftX + this.swayX * sway * SWAY;
        this.zd = this.driftZ + this.swayZ * sway * SWAY;
        this.yd = RISE_SPEED;

        this.quadSize = Mth.lerp(life, START_SIZE, END_SIZE);
        this.oRoll = this.roll;
        this.roll = sway * 0.3F;

        float fadeIn = Math.min(1.0F, this.age / 4.0F);
        float fadeOut = life > 0.6F ? 1.0F - (life - 0.6F) / 0.4F : 1.0F;
        this.alpha = fadeIn * fadeOut;
    }

    @Override
    public int getLightColor(float partialTick) {
        return FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            SleepZParticle particle = new SleepZParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}
