package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public class SagaCellSuperPerfectEntity extends DBSagasEntity {

    private static final int SKILL_KAMEHA = 1;
    private static final int SKILL_KILASER = 2;
    private static final int SKILL_BARRIER = 3;

    private int kamehaCooldown = 0;
    private int kilaserCooldown = 0;
    private int barrierCooldown = 0;

    public SagaCellSuperPerfectEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setFlySpeed(0.6D);
        this.setAuraColor(0xFFF06E);
        this.setKiCharge(true);
        this.setLightning(true);
        this.setLightningColor(0xA1FFFF);
        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(2147483647);
        }
    }

    @Override
    public void stopCasting() {
        int usedSkill = getSkillType();

        if (usedSkill == SKILL_KAMEHA) {
            this.kamehaCooldown = 20 * 20;
        }
        else if (usedSkill == SKILL_KILASER) {
            this.kilaserCooldown = 6 * 20;
        }
        else if (usedSkill == SKILL_BARRIER) {
            this.barrierCooldown = 15 * 20;
        }

        super.stopCasting();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "skill_controller", 0, this::skillPredicate));
    }

    @Override
    public void performTeleport(LivingEntity target) {
        Vec3 targetLook = target.getLookAngle().normalize();

        double distanceBehind = 0.7D;
        double destX = target.getX() - (targetLook.x * distanceBehind);
        double destZ = target.getZ() - (targetLook.z * distanceBehind);
        double destY = target.getY();

        this.teleportTo(destX, destY, destZ);

        this.playSound(MainSounds.TP.get(), 1.0F, 1.0F);

        this.lookAt(target, 360, 360);
    }

    private <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        if (this.isCasting()) {
            int currentSkill = getSkillType();

            if (currentSkill == SKILL_KAMEHA) {
                return event.setAndContinue(ANIM_KIWAVE);
            }
            else if (currentSkill == SKILL_KILASER) {
            }
            else if (currentSkill == SKILL_BARRIER) {
            }

            return PlayState.CONTINUE;
        }
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }
}