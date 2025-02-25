package com.yuseix.dragonminez.init.entity.custom.projectil;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Optional;
import java.util.UUID;

public class KiSmallBallProjectil extends ThrowableProjectile {

    private int lifetime = 0; // Lleva la cuenta del tiempo en ticks

    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KiSmallBallProjectil.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR_BORDE = SynchedEntityData.defineId(KiSmallBallProjectil.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DANO = SynchedEntityData.defineId(KiSmallBallProjectil.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> VELOCIDAD = SynchedEntityData.defineId(KiSmallBallProjectil.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(KiSmallBallProjectil.class, EntityDataSerializers.OPTIONAL_UUID);

    public KiSmallBallProjectil(EntityType<? extends ThrowableProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.setNoGravity(true);

        this.entityData.define(COLOR, 11403263); // Color predeterminado (negro)
        this.entityData.define(COLOR_BORDE, 3145727); // Color predeterminado (negro)
        this.entityData.define(DANO, 15.0f);
        this.entityData.define(VELOCIDAD, 0.15f);
        this.entityData.define(OWNER_UUID, Optional.empty());

    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }
    public void setColor(int color) {
        this.entityData.set(COLOR, color);
    }
    public int getColorBorde() {
        return this.entityData.get(COLOR_BORDE);
    }
    public void setColorBorde(int color) {
        this.entityData.set(COLOR_BORDE, color);
    }
    public float getDamage() {
        return this.entityData.get(DANO);
    }
    public void setDamage(float damage) {
        this.entityData.set(DANO, damage);
    }
    public float getVelocidad() {
        return this.entityData.get(VELOCIDAD);
    }
    public void setVelocidad(float velocidad) {
        this.entityData.set(VELOCIDAD, velocidad);
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.of(uuid));
    }
    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();

        // Verifica que el objetivo no sea el dueño del proyectil
        if (target.getUUID().equals(this.getOwnerUUID())) {
            return;
        }

        // Verifica si el dueño y el objetivo están en un equipo y si pertenecen al mismo equipo
        if (target instanceof LivingEntity livingTarget) {
            Entity owner = this.getOwner();

            if (owner instanceof LivingEntity ownerLiving) {
                if (ownerLiving.getTeam() != null && livingTarget.getTeam() != null &&
                        ownerLiving.getTeam().equals(livingTarget.getTeam())) {
                    return; // No hace daño si ambos están en el mismo equipo
                }
            }

            // Aplica el daño al objetivo si no pertenece al mismo equipo
            DamageSource damageSource = (owner != null)
                    ? level().damageSources().indirectMagic(this, owner)
                    : level().damageSources().magic();

            livingTarget.hurt(damageSource, this.getDamage());
        }

        // Lógica para destruir o hacer desaparecer el proyectil después del impacto
        //this.discard();
    }

    @Override
    public void tick() {
        super.tick();

        // Incrementa el contador de vida
        lifetime++;

        if (this.getOwner() == null || this.getOwnerUUID() == null) {
            this.discard(); // Destruye la entidad
            return; // Termina el tick para evitar más ejecución
        }

        // Verifica si han pasado 5 segundos (100 ticks)
        if (lifetime >= 100) {
            this.discard(); // Destruye la entidad
        }



    }

    @Override
    protected void defineSynchedData() {

    }


}
