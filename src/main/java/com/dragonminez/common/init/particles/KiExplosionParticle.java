package com.dragonminez.common.init.particles;

import com.dragonminez.client.render.util.ModParticleRenderTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class KiExplosionParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;
    private final float baseSize;
    private final float targetSize;

    // Define cuántos fotogramas reales tiene tu animación
    private final int animationFrames = 8;

    protected KiExplosionParticle(ClientLevel level, double x, double y, double z, double r, double g, double b, SpriteSet spriteSet, float size) {
        super(level, x, y, z, 0, 0, 0);

        this.spriteSet = spriteSet;

        // Configuraciones de tamaño (usa el valor 'r' que enviamos desde el servidor)
        this.baseSize = size;
        this.targetSize = size * 1.3F; // Se expande un 25%
        this.quadSize = this.baseSize;

        this.lifetime = 20; // 8 ticks de animación + 22 de desvanecimiento

        this.gravity = 0.0F;
        this.hasPhysics = false;

        // Colores base
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;

        // Ponemos el primer frame manualmente al nacer
        this.setSprite(this.spriteSet.get(0, animationFrames));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            // --- 1. LÓGICA DE FOTOGRAMAS (Animación) ---
            if (this.age < this.animationFrames) {
                // Cambia de frame según la edad mientras dure la animación
                this.setSprite(this.spriteSet.get(this.age, this.animationFrames));
            } else {
                // Se queda "congelada" en el último fotograma para el desvanecimiento
                this.setSprite(this.spriteSet.get(this.animationFrames - 1, this.animationFrames));
            }

            // --- 2. LÓGICA DE EXPANSIÓN (Escalado) ---
            if (this.age <= this.animationFrames) {
                // Crece suavemente solo durante la fase de animación
                float expandProgress = (float)this.age / (float)this.animationFrames;
                this.quadSize = Mth.lerp(expandProgress, this.baseSize, this.targetSize);
            } else {
                this.quadSize = this.targetSize;
            }

            // --- 3. LÓGICA DE FADE (Desvanecimiento) ---
            if (this.age > this.animationFrames) {
                // Calculamos el ratio de vida que queda después de la animación
                float fadeTicks = (float)(this.age - this.animationFrames);
                float totalFadeDuration = (float)(this.lifetime - this.animationFrames);

                float lifeRatio = 1.0F - (fadeTicks / totalFadeDuration);

                // Desvanecimiento exponencial para que se vea más natural (suave al final)
                this.alpha = lifeRatio * lifeRatio;
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Usa ADDITIVE_LIT si quieres que brille, o PARTICLE_SHEET_TRANSLUCENT si es más tipo "humo"
        return ModParticleRenderTypes.ADDITIVE_LIT;
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 15728880; // Brillo máximo constante
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        public Provider(SpriteSet spriteSet) { this.spriteSet = spriteSet; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double r, double g, double b) {
            // Recuperamos el tamaño del parámetro 'r' enviado desde el servidor
            float size = (float)r;
            // Si por error llega 0, le damos un tamaño por defecto
            if (size <= 0) size = 1.0F;

            return new KiExplosionParticle(level, x, y, z, r, g, b, this.spriteSet, size);
        }
    }
}