package com.dragonminez.common.init.particles;

import com.dragonminez.client.render.util.ModParticleRenderTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AuraParticle extends TextureSheetParticle {

    private float baseScale;

    protected AuraParticle(ClientLevel level, double x, double y, double z, double r, double g, double b) {
        super(level, x, y, z);

        this.rCol = (float) r;
        this.gCol = (float) g;
        this.bCol = (float) b;

        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        this.quadSize *= 0.75f + random.nextFloat();
        this.lifetime = 40 + this.random.nextInt(20);
        this.baseScale = this.quadSize;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);

            this.xd *= 0.98;
            this.yd *= 0.98;
            this.zd *= 0.98;

            this.setAlpha(1.0f - ((float) this.age / this.lifetime));
        }
    }

    public void resize(float multiplier) {
        this.quadSize = this.baseScale * multiplier;
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
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double r, double g, double b) {
            // Pasamos r, g, b al constructor
            AuraParticle particle = new AuraParticle(level, x, y, z, r, g, b);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}