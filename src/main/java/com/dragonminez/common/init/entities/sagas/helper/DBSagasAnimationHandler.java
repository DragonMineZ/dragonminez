package com.dragonminez.common.init.entities.sagas.helper;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.AiTier;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.LocomotionMode;
import net.minecraft.world.entity.ai.attributes.Attributes;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class DBSagasAnimationHandler {

    public static <T extends GeoAnimatable> PlayState walkPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (entity.isEvading() || entity.isComboing()) {
            event.getController().setAnimationSpeed(1.0D);
            return PlayState.STOP;
        }

        if (entity.isCasting() && entity.getSkillType() != 9) {
            event.getController().setAnimationSpeed(1.0D);
            return PlayState.STOP;
        }

        double moveSpeedAttr = entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double animModifier = moveSpeedAttr / 0.25D;

        if (event.isMoving()) {
            event.getController().setAnimationSpeed(animModifier);
        } else {
            event.getController().setAnimationSpeed(1.0D);
        }

        int style = entity.getDBZStyle();

        //FLY
        if (entity.isFlying()) {
            double currentSpeedSqr = entity.getDeltaMovement().x * entity.getDeltaMovement().x + entity.getDeltaMovement().z * entity.getDeltaMovement().z;
            if (currentSpeedSqr > 0.15D) {
                if (style == 3) return event.setAndContinue(DBSagasAnimations.ANIM_FLY_FAST4);
                return event.setAndContinue(DBSagasAnimations.ANIM_FLY_FAST);
            }
            if (style == 3) return event.setAndContinue(DBSagasAnimations.ANIM_FLY4);
            return event.setAndContinue(DBSagasAnimations.ANIM_FLY);
        }

        //MOVING
        if (event.isMoving()) {
            boolean runAnim;
            if (entity.getAiTier() != AiTier.SIMPLE) {
                LocomotionMode mode = entity.getLocomotionMode();
                runAnim = mode == LocomotionMode.RUN || mode == LocomotionMode.DASH || mode == LocomotionMode.WALK_SLOW;
                if (mode == LocomotionMode.WALK_SLOW) {
                    event.getController().setAnimationSpeed(1.0D);
                }
            } else {
                runAnim = entity.isAggressive() || entity.getTarget() != null;
            }

            if (runAnim) {
                if (style == 1) return event.setAndContinue(DBSagasAnimations.ANIM_RUN_2);
                if (style == 2) return event.setAndContinue(DBSagasAnimations.ANIM_RUN_3);
                if (style == 3) return event.setAndContinue(DBSagasAnimations.ANIM_RUN_4);
                if (style == 4) return event.setAndContinue(DBSagasAnimations.ANIM_RUN_5);
                return event.setAndContinue(DBSagasAnimations.ANIM_RUN);
            } else {
                if (style == 1) return event.setAndContinue(DBSagasAnimations.ANIM_WALK_2);
                if (style == 2) return event.setAndContinue(DBSagasAnimations.ANIM_WALK_3);
                if (style == 3) return event.setAndContinue(DBSagasAnimations.ANIM_WALK_4);
                if (style == 4) return event.setAndContinue(DBSagasAnimations.ANIM_WALK_5);
                return event.setAndContinue(DBSagasAnimations.ANIM_WALK);
            }
        }

        // Idle
        if (style == 1) return event.setAndContinue(DBSagasAnimations.ANIM_IDLE_2);
        if (style == 2) return event.setAndContinue(DBSagasAnimations.ANIM_IDLE_3);
        if (style == 3) return event.setAndContinue(DBSagasAnimations.ANIM_IDLE_4);
        if (style == 4) return event.setAndContinue(DBSagasAnimations.ANIM_IDLE_5);
        return event.setAndContinue(DBSagasAnimations.ANIM_IDLE);
    }

    private static RawAnimation kiClip(DBSagasEntity entity, int skill, RawAnimation cast, RawAnimation fire) {
        return entity.getClientCastTicks() > SkillManager.getFireTick(skill) ? fire : cast;
    }

    public static <T extends GeoAnimatable> PlayState skillPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (entity.isComboing()) {
            int comboId = entity.getComboId();

            if (comboId == 0) return event.setAndContinue(DBSagasAnimations.ANIM_COMBO1);
            if (comboId == 1) return event.setAndContinue(DBSagasAnimations.ANIM_COMBO2);
            if (comboId == 2) return event.setAndContinue(DBSagasAnimations.ANIM_COMBO3);
            if (comboId == 3) {
                if (entity.comboTimer >= 45) return event.setAndContinue(DBSagasAnimations.ANIM_KI_KAME);
                return event.setAndContinue(DBSagasAnimations.ANIM_COMBO3);
            }
            if (comboId == 4) return event.setAndContinue(DBSagasAnimations.ANIM_GRAB_KI);
            if (comboId == 5) return event.setAndContinue(DBSagasAnimations.ANIM_GUM_PUNCH);
            if (comboId == 6) return event.setAndContinue(DBSagasAnimations.ANIM_COMBO5);
            if (comboId == 7) return event.setAndContinue(DBSagasAnimations.ANIM_SLEEP_RECOVERY);
            if (comboId == 8) return event.setAndContinue(DBSagasAnimations.ANIM_COMBO7);

        }

        if (entity.isCasting()) {
            int skill = entity.getSkillType();
            switch (skill) {
                case 1: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_KAMEHA_CAST, DBSagasAnimations.ANIM_KAMEHA_FIRE));
                case 2: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_GALICK_CAST, DBSagasAnimations.ANIM_GALICK_FIRE));
                case 3: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_MAKKAKO_CAST, DBSagasAnimations.ANIM_MAKKAKO_FIRE));
                case 4: return event.setAndContinue(DBSagasAnimations.ANIM_LASER_FIRE);
                case 5: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_EXPLOSION_CAST, DBSagasAnimations.ANIM_EXPLOSION_FIRE));
                case 6: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_BARRIER_CAST, DBSagasAnimations.ANIM_BARRIER_FIRE));
                case 7: return event.setAndContinue(DBSagasAnimations.ANIM_RAGE_SCREAM);
                case 8: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_FINALFLASH_CAST, DBSagasAnimations.ANIM_FINALFLASH_FIRE));
                case 9: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_MOUTH_BLAST_CAST, DBSagasAnimations.ANIM_MOUTH_BLAST_FIRE));
                case 10: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_BARRAGE_CAST, DBSagasAnimations.ANIM_BARRAGE_FIRE));
                case 11: return event.setAndContinue(DBSagasAnimations.ANIM_KIBLAST);
                case 12: return event.setAndContinue(DBSagasAnimations.ANIM_KIATTACK);
                case 13: return event.setAndContinue(DBSagasAnimations.ANIM_LASER_FIRE_LOOP);
                case 14: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_KIENZAN_CAST, DBSagasAnimations.ANIM_KIENZAN_FIRE));
                case 15: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_LARGE_BALL_CAST, DBSagasAnimations.ANIM_LARGE_BALL_FIRE));
                case 16: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_MASENKO_CAST, DBSagasAnimations.ANIM_MASENKO_FIRE));
                case 17: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_BIGBANG_CAST, DBSagasAnimations.ANIM_BIGBANG_FIRE));
                case 18: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_FINALFLASH_CAST, DBSagasAnimations.ANIM_FINALFLASH_FIRE));
                case 19: return event.setAndContinue(DBSagasAnimations.ANIM_KI_LASER);
                case 20: return event.setAndContinue(DBSagasAnimations.ANIM_KI_EXPLOSION);
                case 22: return event.setAndContinue(DBSagasAnimations.ANIM_WOLF_FANG);
                case 23: return event.setAndContinue(DBSagasAnimations.ANIM_DRAGON_FIST);
                case 21: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_FINALFLASH_CAST, DBSagasAnimations.ANIM_FINALFLASH_FIRE));
                case 24: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_KAMEHA_CAST, DBSagasAnimations.ANIM_KAMEHA_FIRE));
                case 25: return event.setAndContinue(DBSagasAnimations.ANIM_TAIYOKEN);
                case 26: return event.setAndContinue(DBSagasAnimations.ANIM_DODONPA_FIRE);
                case 27: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_BURNING_ATTACK_CAST, DBSagasAnimations.ANIM_BURNING_ATTACK_FIRE));
                case 28: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_SUPERNOVA_COOLER_CAST, DBSagasAnimations.ANIM_SUPERNOVA_COOLER_FIRE));
                case 29: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_ASSAULT_RAIN_CAST, DBSagasAnimations.ANIM_ASSAULT_RAIN_FIRE));
                case 30: return event.setAndContinue(kiClip(entity, skill, DBSagasAnimations.ANIM_BLASTER_METEOR_CAST, DBSagasAnimations.ANIM_BLASTER_METEOR_FIRE));
                default: return event.setAndContinue(DBSagasAnimations.ANIM_KIWAVE);
            }
        }

        if (entity.isTransforming()) {
            int style = entity.getDBZStyle();
            if (style == 1) return event.setAndContinue(DBSagasAnimations.ANIM_TRANSFORMATION2);
            if (style == 2) return event.setAndContinue(DBSagasAnimations.ANIM_TRANSFORMATION3);
            return event.setAndContinue(DBSagasAnimations.ANIM_TRANSFORMATION1);
        }

        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    public static <T extends GeoAnimatable> PlayState attackPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (entity.isCasting() || entity.isTransforming() || entity.isComboing() ||
                entity.isEvading() || entity.isZanzoken()) {
            return PlayState.STOP;
        }

        int style = entity.getDBZStyle();

        if (entity.swingTime > 0 && !entity.isAttacking()) {
            entity.setAttacking(true);
            event.getController().forceAnimationReset();

            int randAttack = entity.getRandom().nextInt(3);

            if (style == 1) {
                if (randAttack == 0) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK1_2);
                else if (randAttack == 1) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK2_2);
                else event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK3_2);
            } else if (style == 2) {
                if (randAttack == 0) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK1_3);
                else if (randAttack == 1) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK2_3);
                else event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK3_3);
            } else if (style == 3) {
                if (randAttack == 0) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK1_4);
                else if (randAttack == 1) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK2_4);
                else event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK3_4);
            } else if (style == 4) {
                if (randAttack == 0) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK1_5);
                else if (randAttack == 1) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK2_5);
                else event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK3_5);
            } else if (style == 5) {
                if (randAttack == 0) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK1_6);
                else if (randAttack == 1) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK2_6);
                else event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK3_6);
            } else {
                if (randAttack == 0) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK1);
                else if (randAttack == 1) event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK2);
                else event.getController().setAnimation(DBSagasAnimations.ANIM_ATTACK3);
            }
            return PlayState.CONTINUE;
        }

        if (entity.isAttacking()) {
            if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                entity.setAttacking(false);
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    public static <T extends GeoAnimatable> PlayState evasionPredicate(AnimationState<T> event) {
        DBSagasEntity entity = (DBSagasEntity) event.getAnimatable();

        if (entity.isZanzoken()) {
            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }

        if (entity.isEvading()) {
            return event.setAndContinue(DBSagasAnimations.ANIM_EVADE);
        }

        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    public static <T extends GeoAnimatable> PlayState tailPredicate(AnimationState<T> event) {
        return event.setAndContinue(DBSagasAnimations.ANIM_TAIL);
    }

    public static <T extends GeoAnimatable> PlayState capePredicate(AnimationState<T> event) {
        return event.setAndContinue(DBSagasAnimations.ANIM_CAPE);
    }
}