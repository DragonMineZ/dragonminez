package com.dragonminez.common.init.entities.sagas.helper;

import software.bernie.geckolib.core.animation.RawAnimation;

public class DBSagasAnimations {

    // DEFAULT = DBZSTYLE 0
    public static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation ANIM_RUN = RawAnimation.begin().thenLoop("run1");
    public static final RawAnimation ANIM_ATTACK1 = RawAnimation.begin().thenPlay("attack1_1");
    public static final RawAnimation ANIM_ATTACK2 = RawAnimation.begin().thenPlay("attack2_1");
    public static final RawAnimation ANIM_ATTACK3 = RawAnimation.begin().thenPlay("attack3_1");
    public static final RawAnimation ANIM_TRANSFORMATION1 = RawAnimation.begin().thenPlay("transformation_1");

    // KICKS = DBZSTYLE 1
    public static final RawAnimation ANIM_IDLE_2 = RawAnimation.begin().thenLoop("idle2");
    public static final RawAnimation ANIM_WALK_2 = RawAnimation.begin().thenLoop("walk2");
    public static final RawAnimation ANIM_RUN_2 = RawAnimation.begin().thenLoop("run2");
    public static final RawAnimation ANIM_ATTACK1_2 = RawAnimation.begin().thenPlay("attack1_2");
    public static final RawAnimation ANIM_ATTACK2_2 = RawAnimation.begin().thenPlay("attack2_2");
    public static final RawAnimation ANIM_ATTACK3_2 = RawAnimation.begin().thenPlay("attack3_2");
    public static final RawAnimation ANIM_TRANSFORMATION2 = RawAnimation.begin().thenPlay("transformation_2");

    // FIGHTER = DBZSTYLE 2
    public static final RawAnimation ANIM_IDLE_3 = RawAnimation.begin().thenLoop("idle3");
    public static final RawAnimation ANIM_WALK_3 = RawAnimation.begin().thenLoop("walk3");
    public static final RawAnimation ANIM_RUN_3 = RawAnimation.begin().thenLoop("run3");
    public static final RawAnimation ANIM_ATTACK1_3 = RawAnimation.begin().thenPlay("attack1_3");
    public static final RawAnimation ANIM_ATTACK2_3 = RawAnimation.begin().thenPlay("attack2_3");
    public static final RawAnimation ANIM_ATTACK3_3 = RawAnimation.begin().thenPlay("attack3_3");
    public static final RawAnimation ANIM_TRANSFORMATION3 = RawAnimation.begin().thenPlay("transformation_3");

    // HILDEGARN = DBZSTYLE 3
    public static final RawAnimation ANIM_IDLE_4 = RawAnimation.begin().thenLoop("idle4");
    public static final RawAnimation ANIM_WALK_4 = RawAnimation.begin().thenLoop("walk4");
    public static final RawAnimation ANIM_RUN_4 = RawAnimation.begin().thenLoop("run4");
    public static final RawAnimation ANIM_ATTACK1_4 = RawAnimation.begin().thenPlay("attack1_4");
    public static final RawAnimation ANIM_ATTACK2_4 = RawAnimation.begin().thenPlay("attack2_4");
    public static final RawAnimation ANIM_ATTACK3_4 = RawAnimation.begin().thenPlay("attack3_4");
    public static final RawAnimation ANIM_TRANSFORMATION4 = RawAnimation.begin().thenPlay("transformation_3");

    // OOZARU = DBZSTYLE 4
    public static final RawAnimation ANIM_IDLE_5 = RawAnimation.begin().thenLoop("idle5");
    public static final RawAnimation ANIM_WALK_5 = RawAnimation.begin().thenLoop("walk5");
    public static final RawAnimation ANIM_RUN_5 = RawAnimation.begin().thenLoop("run5");
    public static final RawAnimation ANIM_ATTACK1_5 = RawAnimation.begin().thenPlay("attack1_4");
    public static final RawAnimation ANIM_ATTACK2_5 = RawAnimation.begin().thenPlay("attack2_4");
    public static final RawAnimation ANIM_ATTACK3_5 = RawAnimation.begin().thenPlay("attack3_4");
    public static final RawAnimation ANIM_TRANSFORMATION5 = RawAnimation.begin().thenPlay("transformation_1");

    // DEFAULT
    public static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("fly");
    public static final RawAnimation ANIM_FLY_FAST = RawAnimation.begin().thenLoop("fly_fast");
    // HILDEGARN
    public static final RawAnimation ANIM_FLY4 = RawAnimation.begin().thenLoop("fly_idle4");
    public static final RawAnimation ANIM_FLY_FAST4 = RawAnimation.begin().thenLoop("fly_fast4");

    public static final RawAnimation ANIM_EVADE = RawAnimation.begin().thenPlay("evasion1");
    public static final RawAnimation ANIM_KIWAVE = RawAnimation.begin().thenPlay("ki_finalflash");
    public static final RawAnimation ANIM_KIATTACK = RawAnimation.begin().thenPlay("kiattack");
    public static final RawAnimation ANIM_KIBALL = RawAnimation.begin().thenPlay("ki_ball");
    public static final RawAnimation ANIM_KIBLAST = RawAnimation.begin().thenPlay("ki_blast");
    public static final RawAnimation ANIM_TAIL = RawAnimation.begin().thenLoop("tail");
    public static final RawAnimation ANIM_CAPE = RawAnimation.begin().thenLoop("cape");
    public static final RawAnimation ANIM_GRAB = RawAnimation.begin().thenLoop("grab");
    public static final RawAnimation ANIM_GRAB_KI = RawAnimation.begin().thenLoop("grab_ki");
    public static final RawAnimation ANIM_KI_BARRAGE = RawAnimation.begin().thenPlay("ki_barrage");

    public static final RawAnimation ANIM_KI_MAKKAKO = RawAnimation.begin().thenPlay("ki_makkako");
    public static final RawAnimation ANIM_KI_KAME = RawAnimation.begin().thenPlay("ki_kame");
    public static final RawAnimation ANIM_KI_MASENKO = RawAnimation.begin().thenPlay("ki_masenko");
    public static final RawAnimation ANIM_KI_BARRIER = RawAnimation.begin().thenPlay("ki_barrier");
    public static final RawAnimation ANIM_KI_GALICK = RawAnimation.begin().thenPlay("ki_galick");
    public static final RawAnimation ANIM_KI_EXPLOSION = RawAnimation.begin().thenPlay("ki_explosion");
    public static final RawAnimation ANIM_KI_BIG_BANG = RawAnimation.begin().thenPlay("ki_bigbang");
    public static final RawAnimation ANIM_KI_FINALFLASH = RawAnimation.begin().thenPlay("ki_finalflash");
    public static final RawAnimation ANIM_KI_DISC = RawAnimation.begin().thenPlay("ki_kienzan");
    public static final RawAnimation ANIM_KI_LASER = RawAnimation.begin().thenPlay("ki_laser");
    public static final RawAnimation ANIM_KIOZARU = RawAnimation.begin().thenPlay("ki_oozaru");

    public static final RawAnimation ANIM_COMBO1 = RawAnimation.begin().thenPlay("combo1");
    public static final RawAnimation ANIM_COMBO2 = RawAnimation.begin().thenPlay("combo2");
    public static final RawAnimation ANIM_COMBO3 = RawAnimation.begin().thenPlay("combo3");
    public static final RawAnimation ANIM_COMBO4 = RawAnimation.begin().thenPlay("combo4");
    public static final RawAnimation ANIM_COMBO5 = RawAnimation.begin().thenPlay("combo5");
    public static final RawAnimation ANIM_COMBO6 = RawAnimation.begin().thenPlay("combo6");
    public static final RawAnimation ANIM_COMBO7 = RawAnimation.begin().thenPlay("combo7");

}
