package com.yuseix.dragonminez.init.entity.custom.namek;

import com.yuseix.dragonminez.init.entity.goals.DetectEvilTargetGoal;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NamekWarriorEntity extends NamekianEntity {


    public NamekWarriorEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.setPersistenceRequired();

    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2000.0D)
                .add(Attributes.ATTACK_DAMAGE, 750.5D)
                .add(Attributes.ATTACK_SPEED, 1.0f)
                .add(Attributes.MOVEMENT_SPEED, 0.25F).build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));

        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));

        // Solo ataca si es provocado y es agresivo hacia el jugador
        this.targetSelector.addGoal(1, new DetectEvilTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, SoldierEntity.class, true));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));

    }
    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();
        if (target != null) {
            double heightDifference = target.getY() - this.getY();

            // Si el jugador está muy alto, activa el modo de vuelo
            if (heightDifference > 1.5) {
                this.setNoGravity(true);

                // Mantén la misma posición horizontal que el jugador
                double targetX = target.getX();
                double targetY = target.getY()-1.5; // Ajusta esto según la altura deseada
                double targetZ = target.getZ();

                // Configura la posición deseada en el aire con control de altura
                this.getMoveControl().setWantedPosition(targetX, targetY, targetZ, 1.0);

                // Aplica movimiento vertical para flotar
                double verticalSpeed = 0.01; // Ajusta este valor para que el movimiento sea más suave
                this.setDeltaMovement(this.getDeltaMovement().add(0, verticalSpeed, 0));

            } else {
                // Si el jugador no está tan alto, mantén a la entidad en el suelo
                this.setNoGravity(false);

            }
        } else {
            this.setNoGravity(false);
        }

    }

}