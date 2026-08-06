package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class MainSounds {

	public static final DeferredRegister<SoundEvent> SOUND_EVENTS_REGISTER =
			DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Reference.MOD_ID);

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> GOLPE1 = registerSoundEvent("punch1");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> GOLPE2 = registerSoundEvent("punch2");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> GOLPE3 = registerSoundEvent("punch3");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> GOLPE4 = registerSoundEvent("punch4");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> GOLPE5 = registerSoundEvent("punch5");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> GOLPE6 = registerSoundEvent("punch6");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> CRITICO1 = registerSoundEvent("critic_punch1");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> CRITICO2 = registerSoundEvent("critic_punch2");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> BLOCK1 = registerSoundEvent("block1");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> BLOCK2 = registerSoundEvent("block2");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> BLOCK3 = registerSoundEvent("block3");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> PARRY = registerSoundEvent("parry");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> UNBLOCK = registerSoundEvent("unblock");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> EVASION1 = registerSoundEvent("evasion1");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> EVASION2 = registerSoundEvent("evasion2");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KNOCKBACK_CHARACTER = registerSoundEvent("knockback_character");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> ZANZOKEN = registerSoundEvent("zanzoken");

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> ANCHOR_SLAM = registerSoundEvent("anchor_slam");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> AXE_SLASH = registerSoundEvent("axe_slash");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> CLAYMORE_SLAM = registerSoundEvent("claymore_slam");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> CLAYMORE_STAB = registerSoundEvent("claymore_stab");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> CLAYMORE_SWING = registerSoundEvent("claymore_swing");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> DAGGER_SLASH = registerSoundEvent("dagger_slash");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> DOUBLE_AXE_SWING = registerSoundEvent("double_axe_swing");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> FIST_PUNCH = registerSoundEvent("fist_punch");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> GLAIVE_SLASH_QUICK = registerSoundEvent("glaive_slash_quick");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> GLAIVE_SLASH_SLOW = registerSoundEvent("glaive_slash_slow");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> HAMMER_SLAM = registerSoundEvent("hammer_slam");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KATANA_SLASH = registerSoundEvent("katana_slash");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MACE_SLAM = registerSoundEvent("mace_slam");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MACE_SLASH = registerSoundEvent("mace_slash");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> PICKAXE_SWING = registerSoundEvent("pickaxe_swing");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> RAPIER_SLASH = registerSoundEvent("rapier_slash");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> RAPIER_STAB = registerSoundEvent("rapier_stab");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> SCYTHE_SLASH = registerSoundEvent("scythe_slash");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> SICKLE_SLASH = registerSoundEvent("sickle_slash");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> SPEAR_STAB = registerSoundEvent("spear_stab");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> STAFF_SLAM = registerSoundEvent("staff_slam");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> STAFF_SLASH = registerSoundEvent("staff_slash");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> STAFF_SPIN = registerSoundEvent("staff_spin");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> STAFF_STAB = registerSoundEvent("staff_stab");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> SWORD_SLASH = registerSoundEvent("sword_slash");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> WAND_SWING = registerSoundEvent("wand_swing");

    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> DRAGONRADAR = registerSoundEvent("dragonradar");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NUBE = registerSoundEvent("nube");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> SENZU_BEAN = registerSoundEvent("senzu");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> DRAGONBALLS = registerSoundEvent("dragonballssound");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> SHENRON = registerSoundEvent("shenron");

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NAVE_OPEN = registerSoundEvent("ship_open");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NAVE_LANDING_OPEN = registerSoundEvent("ship_landing_open");

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> FROG1 = registerSoundEvent("frogsound1");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> FROG2 = registerSoundEvent("frogsound2");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> FROG3 = registerSoundEvent("frogsound3");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> FROG_LAUGH = registerSoundEvent("froglaugh");

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC = registerSoundEvent("menu_music");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_1 = registerSoundEvent("menu_music_1");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_2 = registerSoundEvent("menu_music_2");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_3 = registerSoundEvent("menu_music_3");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_4 = registerSoundEvent("menu_music_4");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_5 = registerSoundEvent("menu_music_5");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_6 = registerSoundEvent("menu_music_6");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_7 = registerSoundEvent("menu_music_7");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_8 = registerSoundEvent("menu_music_8");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_9 = registerSoundEvent("menu_music_9");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_10 = registerSoundEvent("menu_music_10");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_11 = registerSoundEvent("menu_music_11");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_12 = registerSoundEvent("menu_music_12");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_13 = registerSoundEvent("menu_music_13");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_14 = registerSoundEvent("menu_music_14");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_15 = registerSoundEvent("menu_music_15");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_16 = registerSoundEvent("menu_music_16");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_17 = registerSoundEvent("menu_music_17");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_18 = registerSoundEvent("menu_music_18");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_19 = registerSoundEvent("menu_music_19");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_20 = registerSoundEvent("menu_music_20");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_21 = registerSoundEvent("menu_music_21");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_22 = registerSoundEvent("menu_music_22");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_23 = registerSoundEvent("menu_music_23");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_24 = registerSoundEvent("menu_music_24");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_25 = registerSoundEvent("menu_music_25");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_26 = registerSoundEvent("menu_music_26");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_27 = registerSoundEvent("menu_music_27");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_28 = registerSoundEvent("menu_music_28");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_29 = registerSoundEvent("menu_music_29");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_30 = registerSoundEvent("menu_music_30");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_31 = registerSoundEvent("menu_music_31");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_32 = registerSoundEvent("menu_music_32");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_33 = registerSoundEvent("menu_music_33");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_34 = registerSoundEvent("menu_music_34");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_35 = registerSoundEvent("menu_music_35");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_36 = registerSoundEvent("menu_music_36");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_37 = registerSoundEvent("menu_music_37");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_38 = registerSoundEvent("menu_music_38");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MENU_MUSIC_39 = registerSoundEvent("menu_music_39");

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> AURA_START = registerSoundEvent("aura_start");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_CHARGE_LOOP = registerSoundEvent("ki_charge_loop");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_SPARKS = registerSoundEvent("ki_sparks");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> TURBO_LOOP = registerSoundEvent("turbo_loop");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> TP = registerSoundEvent("tp");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> TP_SHORT = registerSoundEvent("tp_short");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> ABSORB1 = registerSoundEvent("absorb1");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> ABSORB2 = registerSoundEvent("absorb2");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> MAJIN_ABSORB = registerSoundEvent("majin_absorb");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> TRANSFORM_ON = registerSoundEvent("transform_on");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> TRANSFORM_OFF = registerSoundEvent("transform_off");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> INSTA_FORM_ON = registerSoundEvent("insta_form_on");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> INSTA_FORM_OFF = registerSoundEvent("insta_form_off");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NO_KI_FORM = registerSoundEvent("no_ki_form");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> STACK_FORM = registerSoundEvent("stack_form");

    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> FUSION = registerSoundEvent("fusion");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> OOZARU_FIST = registerSoundEvent("oozaru_fist");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> DRAGON_FIST = registerSoundEvent("dragon_fist");

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> UI_MENU_SWITCH = registerSoundEvent("ui_menu_switch");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> CONFIRM_MENU = registerSoundEvent("confirm_menu");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> PIP_MENU = registerSoundEvent("pip_menu");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> UI_NAVE_COOLDOWN = registerSoundEvent("ui_nave_cooldown");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> UI_NAVE_TAKEOFF = registerSoundEvent("ui_nave_takeoff");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> SWITCH_OFF = registerSoundEvent("switch_off");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> SWITCH_ON = registerSoundEvent("switch_on");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> LOCKON = registerSoundEvent("lockon");

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> SWORD_IN = registerSoundEvent("sword_in");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> SWORD_OUT = registerSoundEvent("sword_out");

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> FRIEZA_SOLDIER_AMBIENT = registerSoundEvent("entity.frieza.s.ambient");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> FRIEZA_SOLDIER_HURT = registerSoundEvent("entity.frieza.s.hurt");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> FRIEZA_SOLDIER_DEATH = registerSoundEvent("entity.frieza.s.death");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> FRIEZA_SOLDIER_ATTACK = registerSoundEvent("entity.frieza.s.attack");

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NAMEKIAN_VILLAGER_AMBIENT = registerSoundEvent("entity.namekian.vill.ambient");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NAMEKIAN_VILLAGER_HURT = registerSoundEvent("entity.namekian.vill.hurt");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NAMEKIAN_VILLAGER_DEATH = registerSoundEvent("entity.namekian.vill.death");

	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KIBLAST_ATTACK = registerSoundEvent("kiblast_shoot");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_EXPLOSION_IMPACT = registerSoundEvent("ki_explosion_impact");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_EXPLOSION_CHARGE = registerSoundEvent("ki_explosion_charge");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_LASER = registerSoundEvent("laserbeam");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_KAME_CHARGE = registerSoundEvent("ki_kame_charge");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_KAME_FIRE = registerSoundEvent("ki_kame_fire");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_DISK_CHARGE = registerSoundEvent("ki_disk_charge");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_DISK_FIRE = registerSoundEvent("ki_disk_fire");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_BURNING_CHARGE = registerSoundEvent("ki_burning_charge");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_BURNING_FIRE = registerSoundEvent("ki_burning_fire");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_FINALFLASH_CHARGE = registerSoundEvent("ki_finalflash_charge");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_FINALFLASH_FIRE = registerSoundEvent("ki_finalflash_fire");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_SPIRITBOMB_CHARGE = registerSoundEvent("ki_spiritbomb_charge");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_SPIRITBOMB_FIRE = registerSoundEvent("ki_spiritbomb_fire");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_SUPERNOVA_CHARGE = registerSoundEvent("ki_supernova_charge");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_SUPERNOVA_FIRE = registerSoundEvent("ki_supernova_fire");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_BEAM_CHARGE = registerSoundEvent("ki_beam_charge");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> KI_BEAM_FIRE = registerSoundEvent("ki_beam_fire");



    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> OOZARU_HEARTBEAT = registerSoundEvent("oozaru_heartbeat");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> OOZARU_GROWL_PLAYER = registerSoundEvent("oozaru_growl_player");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> VEGETA_OOZARU_GROWL = registerSoundEvent("vegeta_oozaru_growl");
	public static final DeferredHolder<SoundEvent, ? extends SoundEvent> VEGETA_OOZARU_DEATH = registerSoundEvent("vegeta_oozaru_death");

	private static DeferredHolder<SoundEvent, ? extends SoundEvent> registerSoundEvent(String name) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name);

		return SOUND_EVENTS_REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(id));
	}

	public static void register(IEventBus busEvent) {
		SOUND_EVENTS_REGISTER.register(busEvent);
	}
}
