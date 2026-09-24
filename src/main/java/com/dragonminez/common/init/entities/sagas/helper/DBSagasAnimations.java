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

    // DUAL SWORDS = DBZSTYLE 5
    public static final RawAnimation ANIM_ATTACK1_6 = RawAnimation.begin().thenPlay("attack1_6");
    public static final RawAnimation ANIM_ATTACK2_6 = RawAnimation.begin().thenPlay("attack2_6");
    public static final RawAnimation ANIM_ATTACK3_6 = RawAnimation.begin().thenPlay("attack3_6");

    // WORLD BOSS = DBZSTYLE 6
    public static final RawAnimation ANIM_IDLE_7 = RawAnimation.begin().thenLoop("idle7");
    public static final RawAnimation ANIM_WALK_7 = RawAnimation.begin().thenLoop("walk7");
    public static final RawAnimation ANIM_RUN_7 = RawAnimation.begin().thenLoop("run7");
    public static final RawAnimation ANIM_TRANSFORMATION7 = RawAnimation.begin().thenPlay("transformation_7");
    public static final RawAnimation ANIM_BOSS_SLEEP = RawAnimation.begin().thenLoop("boss1.sleep");
    public static final RawAnimation ANIM_BOSS_SPECIAL2 = RawAnimation.begin().thenPlay("boss1.special2");
    public static final RawAnimation ANIM_BOSS_SPECIAL3 = RawAnimation.begin().thenLoop("boss1.special3");
    public static final RawAnimation ANIM_BOSS_SPECIAL1 = RawAnimation.begin().thenPlay("skp.dimensional_punch");
    public static final RawAnimation ANIM_BOSS_DESTRUCTION = RawAnimation.begin().thenPlay("ki.destruction_balls_fire");
    public static final RawAnimation ANIM_BOSS_CUTS = RawAnimation.begin().thenLoop("skp.dimensional_sword_attack");

    // POWER POLE = DBZSTYLE 7
    public static final RawAnimation ANIM_ATTACK1_8 = RawAnimation.begin().thenPlay("attack1_8");
    public static final RawAnimation ANIM_ATTACK2_8 = RawAnimation.begin().thenPlay("attack2_8");
    public static final RawAnimation ANIM_ATTACK3_8 = RawAnimation.begin().thenPlay("attack3_8");

    // DEFAULT
    public static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("fly");
    public static final RawAnimation ANIM_FLY_FAST = RawAnimation.begin().thenLoop("fly_fast");
    // HILDEGARN
    public static final RawAnimation ANIM_FLY4 = RawAnimation.begin().thenLoop("fly_idle4");
    public static final RawAnimation ANIM_FLY_FAST4 = RawAnimation.begin().thenLoop("fly_fast4");

    public static final RawAnimation ANIM_EVADE = RawAnimation.begin().thenPlay("evasion1");
    public static final RawAnimation ANIM_HURT_LEFT = RawAnimation.begin().thenPlay("base.hurt_left");
    public static final RawAnimation ANIM_HURT_RIGHT = RawAnimation.begin().thenPlay("base.hurt_right");
    public static final RawAnimation ANIM_HURT_GODFIST = RawAnimation.begin().thenPlayAndHold("base.hurt_supergodfist");
    public static final RawAnimation ANIM_HURT_TOP = RawAnimation.begin().thenPlayAndHold("base.hurt_top");
    public static final RawAnimation ANIM_HURT_TOP2 = RawAnimation.begin().thenPlayAndHold("base.hurt_top2");
    public static final RawAnimation ANIM_HURT_DOWN = RawAnimation.begin().thenPlayAndHold("base.hurt_down");
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

    public static final RawAnimation ANIM_KAMEHA_CAST = RawAnimation.begin().thenPlay("ki.kameha_cast");
    public static final RawAnimation ANIM_KAMEHA_FIRE = RawAnimation.begin().thenPlay("ki.kameha_fire");
    public static final RawAnimation ANIM_GALICK_CAST = RawAnimation.begin().thenPlay("ki.galick_cast");
    public static final RawAnimation ANIM_GALICK_FIRE = RawAnimation.begin().thenPlay("ki.galick_fire");
    public static final RawAnimation ANIM_MAKKAKO_CAST = RawAnimation.begin().thenPlay("ki.makkako_cast");
    public static final RawAnimation ANIM_MAKKAKO_FIRE = RawAnimation.begin().thenPlay("ki.makkako_fire");
    public static final RawAnimation ANIM_EXPLOSION_CAST = RawAnimation.begin().thenPlay("ki.explosion_cast");
    public static final RawAnimation ANIM_EXPLOSION_FIRE = RawAnimation.begin().thenPlay("ki.explosion_fire");
    public static final RawAnimation ANIM_BARRIER_CAST = RawAnimation.begin().thenPlay("ki.barrier_cast");
    public static final RawAnimation ANIM_BARRIER_FIRE = RawAnimation.begin().thenPlay("ki.barrier_fire");
    public static final RawAnimation ANIM_BARRAGE_CAST = RawAnimation.begin().thenPlay("ki.barrage_cast");
    public static final RawAnimation ANIM_BARRAGE_FIRE = RawAnimation.begin().thenPlay("ki.barrage_fire");
    public static final RawAnimation ANIM_KIENZAN_CAST = RawAnimation.begin().thenPlay("ki.kienzan_cast");
    public static final RawAnimation ANIM_KIENZAN_FIRE = RawAnimation.begin().thenPlay("ki.kienzan_fire");
    public static final RawAnimation ANIM_LARGE_BALL_CAST = RawAnimation.begin().thenPlay("ki.large_ball_cast");
    public static final RawAnimation ANIM_LARGE_BALL_FIRE = RawAnimation.begin().thenPlay("ki.large_ball_fire");
    public static final RawAnimation ANIM_MASENKO_CAST = RawAnimation.begin().thenPlay("ki.masenko_cast");
    public static final RawAnimation ANIM_MASENKO_FIRE = RawAnimation.begin().thenPlay("ki.masenko_fire");
    public static final RawAnimation ANIM_BIGBANG_CAST = RawAnimation.begin().thenPlay("ki.bigbang_cast");
    public static final RawAnimation ANIM_BIGBANG_FIRE = RawAnimation.begin().thenPlay("ki.bigbang_fire");
    public static final RawAnimation ANIM_FINALFLASH_CAST = RawAnimation.begin().thenPlay("ki.finalflash_cast");
    public static final RawAnimation ANIM_FINALFLASH_FIRE = RawAnimation.begin().thenPlay("ki.finalflash_fire");
    public static final RawAnimation ANIM_MOUTH_BLAST_CAST = RawAnimation.begin().thenLoop("ki.mouth_blast_cast");
    public static final RawAnimation ANIM_MOUTH_BLAST_FIRE = RawAnimation.begin().thenLoop("ki.mouth_blast_fire");
    public static final RawAnimation ANIM_DODONPA_FIRE = RawAnimation.begin().thenPlay("ki.dodonpa_fire");
    public static final RawAnimation ANIM_LASER_FIRE = RawAnimation.begin().thenPlay("ki.laser_fire");
    public static final RawAnimation ANIM_LASER_FIRE_LOOP = RawAnimation.begin().thenLoop("ki.laser_fire");
    public static final RawAnimation ANIM_BURNING_ATTACK_CAST = RawAnimation.begin().thenPlay("ki.burning_attack_cast");
    public static final RawAnimation ANIM_BURNING_ATTACK_FIRE = RawAnimation.begin().thenPlay("ki.burning_attack_fire");
    public static final RawAnimation ANIM_SUPERNOVA_COOLER_CAST = RawAnimation.begin().thenPlay("ki.supernova_cooler_cast");
    public static final RawAnimation ANIM_SUPERNOVA_COOLER_FIRE = RawAnimation.begin().thenPlay("ki.supernova_cooler_fire");
    public static final RawAnimation ANIM_ASSAULT_RAIN_CAST = RawAnimation.begin().thenPlay("ki.assault_rain_cast");
    public static final RawAnimation ANIM_ASSAULT_RAIN_FIRE = RawAnimation.begin().thenPlay("ki.assault_rain_fire");
    public static final RawAnimation ANIM_BLASTER_METEOR_CAST = RawAnimation.begin().thenLoop("ki.blaster_meteor_cast");
    public static final RawAnimation ANIM_BLASTER_METEOR_FIRE = RawAnimation.begin().thenLoop("ki.blaster_meteor_fire");
    public static final RawAnimation ANIM_WOLF_FANG = RawAnimation.begin().thenPlay("skp.wolf_fang");
    public static final RawAnimation ANIM_DRAGON_FIST = RawAnimation.begin().thenPlay("skp.dragon_fist");
    public static final RawAnimation ANIM_GUM_PUNCH = RawAnimation.begin().thenPlay("skp.gum_punch");
    public static final RawAnimation ANIM_SLEEP_RECOVERY = RawAnimation.begin().thenPlay("evs.sleep_recovery");
    public static final RawAnimation ANIM_RAGE_SCREAM = RawAnimation.begin().thenLoop("evs.rage_scream");
    public static final RawAnimation ANIM_TAIYOKEN = RawAnimation.begin().thenPlay("evs.taiyoken");

    public static final RawAnimation ANIM_COMBO1 = RawAnimation.begin().thenPlay("combo1");
    public static final RawAnimation ANIM_COMBO2 = RawAnimation.begin().thenPlay("combo2");
    public static final RawAnimation ANIM_COMBO3 = RawAnimation.begin().thenPlay("combo3");
    public static final RawAnimation ANIM_COMBO4 = RawAnimation.begin().thenPlay("combo4");
    public static final RawAnimation ANIM_COMBO5 = RawAnimation.begin().thenPlay("combo5");
    public static final RawAnimation ANIM_COMBO6 = RawAnimation.begin().thenPlay("combo6");
    public static final RawAnimation ANIM_COMBO7 = RawAnimation.begin().thenPlay("combo7");
    public static final RawAnimation ANIM_SPIRIT_BREAKING_CANNON = RawAnimation.begin().thenPlay("skp.spirit_breaking_cannon");
    public static final RawAnimation ANIM_SUPER_GOD_FIST = RawAnimation.begin().thenPlay("skp.super_god_fist");
    public static final RawAnimation ANIM_DEADLY_DANCE_VEGETTO = RawAnimation.begin().thenPlay("skp.deadly_dance_vegetto");

}
