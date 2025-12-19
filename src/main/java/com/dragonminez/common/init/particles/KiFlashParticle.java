package com.dragonminez.common.init.particles;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;

public class KiFlashParticle extends TextureSheetParticle {

    private final int targetEntityId;
    private Entity targetEntity;
    private boolean colorSet = false;
    private final SpriteSet spriteSet;

    protected KiFlashParticle(ClientLevel level, double x, double y, double z, double entityIdAsDouble, double colorData, double sizeData, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);

        this.spriteSet = spriteSet;

        this.targetEntityId = (int) entityIdAsDouble;

        this.quadSize = 1.0F;

        this.lifetime = 1000;
        this.gravity = 0;
        this.hasPhysics = false;
        this.alpha = 0.0F;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        if (this.targetEntity == null) {
            this.targetEntity = this.level.getEntity(this.targetEntityId);
        }

        if (this.targetEntity == null || !this.targetEntity.isAlive()) {
            this.remove();
            return;
        }

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.setPos(targetEntity.getX(), targetEntity.getY() + (targetEntity.getBbHeight() / 2.0), targetEntity.getZ());

        if (this.targetEntity instanceof AbstractKiProjectile kiBall) {

            if (!this.colorSet) {
                int colorHex = kiBall.getColorBorde();
                float[] rgb = ColorUtils.rgbIntToFloat(colorHex);
                this.rCol = rgb[0];
                this.gCol = rgb[1];
                this.bCol = rgb[2];
                this.alpha = 1.0F;
                this.colorSet = true;
            }
            this.setSpriteFromAge(this.spriteSet);
            this.quadSize = kiBall.getSize() + 0.5F;
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
            return new KiFlashParticle(level, x, y, z, dx, dy, dz, this.spriteSet);
        }
    }
}
