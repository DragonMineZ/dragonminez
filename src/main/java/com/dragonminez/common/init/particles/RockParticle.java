package com.dragonminez.common.init.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RockParticle extends TextureSheetParticle {

    protected RockParticle(ClientLevel level, double x, double y, double z, double velX, double velY, double velZ) {
        super(level, x, y, z, velX, velY, velZ);

        this.gravity = 0.0f;

        this.yd = velY + (Math.random() * 0.02);
        this.xd = velX;
        this.zd = velZ;

        this.quadSize = 0.1f + (this.random.nextFloat() * 0.2f);
        this.lifetime = 40 + this.random.nextInt(40);

        this.roll = this.random.nextFloat() * 3.14f;
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            // Moverse
            this.move(this.xd, this.yd, this.zd);

            this.roll += 0.1f;

            this.xd *= 0.9;
            this.zd *= 0.9;

            this.yd += 0.002f;

            if (this.age > this.lifetime - 10) {
                this.alpha = 1.0f - ((float)(this.age - (this.lifetime - 10)) / 10f);
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE; // OPAQUE es mejor para rocas sólidas
    }

    // --- FACTORY ---
    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            RockParticle particle = new RockParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}