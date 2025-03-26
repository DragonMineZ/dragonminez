package com.yuseix.dragonminez.common.events.characters;

import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.config.old.races.DMZBioAndroidConfig;
import com.yuseix.dragonminez.common.init.MainSounds;
import com.yuseix.dragonminez.common.network.C2S.CharacterC2S;
import com.yuseix.dragonminez.common.network.C2S.DescendFormC2S;
import com.yuseix.dragonminez.common.network.C2S.PermaEffC2S;
import com.yuseix.dragonminez.common.network.ModMessages;
import com.yuseix.dragonminez.common.stats.DMZStatsAttributes;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import com.yuseix.dragonminez.common.stats.forms.FormsData;
import com.yuseix.dragonminez.common.stats.skills.DMZSkill;
import com.yuseix.dragonminez.common.util.DMZDatos;
import com.yuseix.dragonminez.client.util.Keys;
import com.yuseix.dragonminez.common.util.TickHandler;
import com.yuseix.dragonminez.server.worldgen.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StatsEvents {

    private static final Map<UUID, TickHandler> playerTickHandlers = new HashMap<>();

    //Teclas
    private static boolean previousKeyDescendState = false;
    private static boolean previousKiChargeState = false;
    private static boolean turboOn = false, hasPressedTurbo = false;
    private static boolean transformOn = false;
    private static double previousFov = 1.0;
    private static boolean hasHealed = false;

    //Sonidos
    private static SimpleSoundInstance kiChargeLoop, turboLoop, oozaruLoop;


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Verificar que estamos en el servidor y en la fase final
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        Player player = event.player;

        // Verificar que el jugador es un ServerPlayer
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        DMZDatos dmzdatos = new DMZDatos();

        TickHandler tickHandler = playerTickHandlers.computeIfAbsent(player.getUUID(), uuid -> new TickHandler());

        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, serverPlayer).ifPresent(playerstats -> {
            var raza = playerstats.getIntValue("race");
            boolean isDmzUser = playerstats.getBoolean("dmzuser");

            int maxenergia = dmzdatos.calcEnergy(playerstats);

            // Verificar que haya creado su personaje antes de comenzar a hacer cosas referentes a las stats
            if (isDmzUser) {
                if (!playerstats.hasFormSkill("super_form")) {
                    FormsData skill = new FormsData("dmz.dmzforms.super_form.name", 0);
                    playerstats.addFormSkill("super_form", skill);
                }
                if (serverPlayer.getAttribute(Attributes.MAX_HEALTH).getBaseValue() != dmzdatos.calcConstitution(playerstats)) {
                    serverPlayer.getAttribute(Attributes.MAX_HEALTH).setBaseValue(dmzdatos.calcConstitution(playerstats));
                    if (!hasHealed) {
                        serverPlayer.setHealth(dmzdatos.calcConstitution(playerstats));
                        hasHealed = true;
                    }
                }
                if (serverPlayer.getAttribute(Attributes.MAX_HEALTH).getBaseValue() > dmzdatos.calcConstitution(playerstats)
                        && (serverPlayer.getEffect(MobEffects.ABSORPTION) == null || serverPlayer.getEffect(MobEffects.HEALTH_BOOST) == null)) {
                    serverPlayer.getAttribute(Attributes.MAX_HEALTH).setBaseValue(dmzdatos.calcConstitution(playerstats));
                }
                // Tickhandler
                tickHandler.tickRegenConsume(playerstats, dmzdatos, serverPlayer);

                if (raza == 5) {
                    // Pasiva Majin
                    tickHandler.manejarPasivaMajin(playerstats, serverPlayer);
                } else if (raza == 1) {
                    playerstats.setIntValue("zenkaitimer", zenkaiContador(playerstats.getIntValue("zenkaitimer")));
                    if (playerstats.getIntValue("zenkaitimer") == 0 && player.getHealth() < (playerstats.getIntValue("maxhealth") * 0.10)) {
                        // Pasiva Saiyan
                        tickHandler.manejarPasivaSaiyan(playerstats, serverPlayer);
                    }
                }
                serverPlayer.refreshDimensions();
                //Aca manejamos la carga de aura
                tickHandler.manejarCargaDeAura(playerstats, maxenergia);
                //Aca manejamos la carga de la transformacion
                tickHandler.manejarCargaForma(playerstats, serverPlayer);


                //Restar el tiempo que se pone en el comando dmztempeffect
                updateTemporaryEffects(serverPlayer);
                DMZSkill flySkill = playerstats.getDMZSkills().get("fly");

                if (flySkill != null) {
                    if (flySkill.isActive()) {
                        // Consumo de Ki del Fly
                        tickHandler.manejarFlyConsume(playerstats, maxenergia, serverPlayer);

                        if (player.onGround() && !player.getFeetBlockState().is(Blocks.WATER)) { // Desactivar vuelo si toca el suelo
                            flySkill.setActive(false);
                            if (!player.isCreative() && !player.isSpectator()) player.getAbilities().mayfly = false;
                            player.getAbilities().flying = false;
                            player.fallDistance = 0;
                            player.onUpdateAbilities(); // IMPORTANTE: Aplicar cambios de habilidades
                            playerstats.removePermanentEffect("fly");

                        }
                    }
                }

                if (DMZGeneralConfig.OTHERWORLD_ENABLED.get()) {
                    if (!playerstats.getBoolean("alive") && hasHealed) hasHealed = false;
                    if (!playerstats.getBoolean("alive") && playerstats.getIntValue("babaalivetimer") <= 0) {
                        if (serverPlayer.level().dimension() != ModDimensions.OTHERWORLD_DIM_LEVEL_KEY) {
                            ServerLevel serverLevel = serverPlayer.getServer().getLevel(ModDimensions.OTHERWORLD_DIM_LEVEL_KEY);
                            if (serverLevel != null) {
                                serverPlayer.teleportTo(serverLevel, 0, 41, -101, serverPlayer.getYRot(), serverPlayer.getXRot());
                                serverPlayer.displayClientMessage(Component.translatable("dmz.otherworld.cannot_leave"), true);
                            }
                        }
                    }
                }

                if (serverPlayer.getY() < -70) {
                    float restarVida = 1;
                    float cantVida = ((float) dmzdatos.calcConstitution(playerstats) / 100);
                    if (cantVida > 1) restarVida = cantVida;
                    serverPlayer.setHealth(serverPlayer.getHealth() - restarVida);
                    // Si el jugador está en y < -70, perderá 1% hp cada tick hasta matarlo (por si se cae al vacío)
                }

                if (playerstats.getIntValue("babacooldown") > 0) playerstats.setIntValue("babacooldown", babaCooldown(playerstats.getIntValue("babacooldown")));
                if (playerstats.getIntValue("babaalivetimer") > 0) playerstats.setIntValue("babaalivetimer", babaDuration(playerstats.getIntValue("babaalivetimer")));

                AttributeInstance reachAttribute = serverPlayer.getAttribute(ForgeMod.ENTITY_REACH.get());
                AttributeInstance blockReachAttribute = serverPlayer.getAttribute(ForgeMod.BLOCK_REACH.get());

                if (playerstats.getStringValue("form").equals("oozaru") || playerstats.getStringValue("form").equals("giant")) {
                    if (reachAttribute != null && blockReachAttribute != null) {
                        if (reachAttribute.getBaseValue() != 7.0 && blockReachAttribute.getBaseValue() != 8.0) {
                            reachAttribute.setBaseValue(7.0);
                            blockReachAttribute.setBaseValue(8.0);
                        }
                    }
                } else {
                    if (reachAttribute != null && blockReachAttribute != null) {
                        if (reachAttribute.getBaseValue() != 3.0 && blockReachAttribute.getBaseValue() != 4.5) {
                            reachAttribute.setBaseValue(3.0);
                            blockReachAttribute.setBaseValue(4.5);
                        }
                    }
                }

            } else {
                if (hasHealed) hasHealed = false;
                if (serverPlayer.getAttribute(Attributes.MAX_HEALTH).getBaseValue() != 20) serverPlayer.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
            }

            //Tiempo para reclamar una senzu
            playerstats.setIntValue("senzutimer", senzuContador(playerstats.getIntValue("senzutimer")));

        });
    }


    private static void updateTemporaryEffects(Player player) {
        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
            for (Map.Entry<String, Integer> entry : playerstats.getDMZTemporalEffects().entrySet()) {
                int timeLeft = entry.getValue() - 1;  // Reducir en 1 tick cada vez
                if (timeLeft <= 0) {
                    playerstats.removeTemporalEffect(entry.getKey());  // Usalo para eliminar el efecto
                } else {
                    entry.setValue(timeLeft);  // Actualiza el tiempo restante
                }
            }
        });
    }

    @SubscribeEvent
    public static void recibirDaño(LivingHurtEvent event) {

        DMZDatos dmzdatos = new DMZDatos();

        // Si el que hace el daño es un jugador
        if (event.getSource().getEntity() instanceof Player atacante) {
            // Verificar si el ataque es melee (no proyectil)
            if (event.getSource().getMsgId().equals("player")) {
                // Obtener las estadísticas del atacante
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, atacante).ifPresent(cap -> {
                    int curStamina = cap.getIntValue("curstam");
                    boolean isDmzUser = cap.getBoolean("dmzuser");

                    float danoDefault = event.getAmount(); // Capturamos el daño original

                    // Calcular el daño basado en la fuerza del atacante
                    float baseMCDamage = event.getAmount();
                    int baseDamage = dmzdatos.getBaseStrength(cap);
                    int maxDamage = dmzdatos.calcStrength(cap);

                    int danoKiWeapon = dmzdatos.calcKiPower(cap) / 2;
                    var ki_control = cap.hasSkill("ki_control");
                    var ki_manipulation = cap.hasSkill("ki_manipulation");
                    var meditation = cap.hasSkill("meditation");
                    var is_kimanipulation = cap.isActiveSkill("ki_manipulation");
                    int kiManipLevel = cap.getSkillLevel("ki_manipulation");
                    int maxKi = cap.getIntValue("maxenergy");
                    int currKi = cap.getIntValue("curenergy");
                    int staminaCost = (int) (baseDamage / 4.5);

                    // Si el usuario creó su personaje, entonces aplica la lógica del Daño del Mod + Consumo de Stamina
                    if (isDmzUser) {
                        if (curStamina > 0) {
                            int staminaToConsume = Math.min(curStamina, staminaCost);
                            if (staminaCost <= 2) {
                                staminaCost = 2;
                                staminaToConsume = 2; // Mínimo 2 de Stamina
                            }
                            // Consumir Stamina proporcional al daño
                            // Consume lo que se puede
                            float damageMultiplier = (float) staminaToConsume / staminaCost; // Factor de daño basado en Stamina disponible

                            if (curStamina >= staminaCost) {
                                // Aplicar daño ajustado si la Stamina no alcanza
                                float adjustedDamage = maxDamage * damageMultiplier;
                                if (cap.getIntValue("race") == 3) {
                                    if (atacante.getHealth() < (cap.getIntValue("maxhealth") * 0.25)) {
                                        float passiveBioAndroid = (float) DMZBioAndroidConfig.QUARTER_HEALTH_LIFESTEAL.get() / 100;
                                        float lifesteal = (baseMCDamage + adjustedDamage) * passiveBioAndroid;
                                        if (lifesteal < 1) lifesteal = 1;
                                        atacante.heal(lifesteal);
                                    } else if (atacante.getHealth() < (cap.getIntValue("maxhealth") * 0.50)) {
                                        float passiveBioAndroid = (float) DMZBioAndroidConfig.HALF_HEALTH_LIFESTEAL.get() / 100;
                                        float lifesteal = (baseMCDamage + adjustedDamage) * passiveBioAndroid;
                                        if (lifesteal < 1) lifesteal = 1;
                                        atacante.heal(lifesteal);
                                    }
                                }
                                // Verificar si el atacante tiene algún arma de Ki activa, si las tiene, revisa su cantidad de Ki para hacer daño extra.
                                if (ki_control && ki_manipulation && meditation && is_kimanipulation) {
                                    if (cap.getStringValue("kiweapon").equals("scythe")) {
                                        float danoFinal = (float) (adjustedDamage + (((float) danoKiWeapon / 2) * (0.3 * kiManipLevel)));
                                        int kiCost = (int) (maxKi * 0.09);
                                        if (currKi > kiCost) {
                                            event.setAmount(baseMCDamage + danoFinal);
                                            if (!atacante.isCreative() || !atacante.isSpectator())
                                                cap.removeIntValue("curenergy", kiCost);
                                        } else {
                                            event.setAmount(baseMCDamage + adjustedDamage);
                                            sonidosGolpes(atacante);
                                        }
                                    } else if (cap.getStringValue("kiweapon").equals("trident")) {
                                        float danoFinal = (float) (adjustedDamage + (((float) danoKiWeapon) * (0.3 * kiManipLevel)));
                                        int kiCost = (int) (maxKi * 0.18);
                                        if (currKi > kiCost) {
                                            event.setAmount(baseMCDamage + danoFinal);
                                            if (!atacante.isCreative() || !atacante.isSpectator())
                                                cap.removeIntValue("curenergy", kiCost);
                                        } else {
                                            event.setAmount(baseMCDamage + adjustedDamage);
                                            sonidosGolpes(atacante);
                                        }
                                    } else {
                                        float danoFinal = (float) (adjustedDamage + (((float) danoKiWeapon / 4) * (0.3 * kiManipLevel)));
                                        int kiCost = (int) (maxKi * 0.045);
                                        if (currKi > kiCost) {
                                            event.setAmount(baseMCDamage + danoFinal);
                                            if (!atacante.isCreative() || !atacante.isSpectator())
                                                cap.removeIntValue("curenergy", kiCost);
                                        } else {
                                            event.setAmount(baseMCDamage + adjustedDamage);
                                            sonidosGolpes(atacante);
                                        }
                                    }
                                } else {
                                    event.setAmount(baseMCDamage + adjustedDamage);
                                    sonidosGolpes(atacante);
                                }
                                // Descontar stamina del atacante
                                if (!atacante.isCreative()) {
                                    cap.removeIntValue("curstam", staminaToConsume);
                                }
                            } else {
                                // Daño por defecto si al atacante le falta stamina
                                event.setAmount(danoDefault + baseMCDamage);
                            }
                            // Si la entidad que recibe el daño es un jugador
                            if (event.getEntity() instanceof Player objetivo) {
                                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, objetivo).ifPresent(statsObjetivo -> {

                                    int defObjetivo = dmzdatos.calcDefense(statsObjetivo, objetivo);
                                    // Restar la defensa del objetivo al daño
                                    float danoFinal = event.getAmount() - defObjetivo;
                                    event.setAmount(Math.max(danoFinal, 1)); // Asegurarse de que al menos se haga 1 de daño
                                });
                            } else {
                                // Si golpeas a otra entidad (no jugador), aplica el daño máximo basado en la fuerza
                                event.setAmount(event.getAmount()); // Aplica tu máximo daño
                            }
                        }
                    }
                });
            }
        } else {
            // Aquí manejamos el caso donde el atacante no es un jugador
            if (event.getEntity() instanceof Player objetivo) {
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, objetivo).ifPresent(statsObjetivo -> {

                    int defObjetivo = dmzdatos.calcDefense(statsObjetivo, objetivo);

                    // Restar la defensa del objetivo al daño
                    float danoFinal = event.getAmount() - defObjetivo;
                    event.setAmount(Math.max(danoFinal, 1)); // Asegurarse de que al menos se haga 1 de daño
                });
            }
        }
    }

    @SubscribeEvent
    public static void livingFallEvent(LivingFallEvent event) {
        float fallDistance = event.getDistance();

        DMZDatos dmzdatos = new DMZDatos();

        if (event.getEntity() instanceof ServerPlayer player) {
            if (fallDistance > 3.0f) {

                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
                    boolean isDmzUser = stats.getBoolean("dmzuser");

                    DMZSkill jump = stats.getDMZSkills().get("jump");
                    DMZSkill fly = stats.getDMZSkills().get("fly");

                    if (jump != null && jump.isActive() || fly != null && fly.isActive() || stats.getBoolean("turbo")) {
                        event.setCanceled(true);
                    } else {
                        int maxEnergy = dmzdatos.calcEnergy(stats);

                        // drenaje de config
                        int baseEnergyDrain = (int) Math.ceil(maxEnergy * DMZGeneralConfig.MULTIPLIER_FALLDMG.get());

                        // Incrementar el drenaje por altura
                        int extraEnergyDrain = (int) ((fallDistance - 4.5f) * baseEnergyDrain / 4.5f);

                        int totalEnergyDrain = baseEnergyDrain + extraEnergyDrain;

                        // Solo drenar energía si el jugador tiene suficiente y cancelar el daño
                        if (isDmzUser && stats.getIntValue("curenergy") >= totalEnergyDrain) {
                            stats.removeIntValue("curenergy", totalEnergyDrain);
                            event.setCanceled(true);
                        }
                    }
                });
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyInputEvent(InputEvent.Key event) {
        boolean isKiChargeKeyPressed = Keys.KI_CHARGE.isDown();
        boolean isDescendKeyPressed = Keys.DESCEND_KEY.isDown();
        boolean isTurboKeypressed = Keys.TURBO_KEY.consumeClick();
        boolean isTransformKeyPressed = Keys.TRANSFORM.isDown();

        LocalPlayer player = Minecraft.getInstance().player;

        if (player != null) {
            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
                int curEne = stats.getIntValue("curenergy");
                int maxEne = stats.getIntValue("maxenergy");
                int porcentaje = (int) Math.ceil((double) (curEne * 100) / maxEne);

                if (stats.getBoolean("dmzuser")) {

                    //Cargar Ki
                    if (isKiChargeKeyPressed && !previousKiChargeState && !transformOn) {
                        ModMessages.sendToServer(new CharacterC2S("isAuraOn", 1));
                        previousKiChargeState = true; // Actualiza el estado previo
                        playSoundOnce(MainSounds.AURA_START.get());
                        startLoopSound(MainSounds.KI_CHARGE_LOOP.get(), "ki");
                    } else if (!isKiChargeKeyPressed && previousKiChargeState) {
                        ModMessages.sendToServer(new CharacterC2S("isAuraOn", 0));
                        previousKiChargeState = false; // Actualiza el estado previo
                        stopLoopSound("ki");
                    }

                    // Descender de ki
                    if (isDescendKeyPressed && !previousKeyDescendState) {
                        ModMessages.sendToServer(new CharacterC2S("isDescendOn", 1));
                        previousKeyDescendState = true; // Actualiza el estado previo
                    } else if (!isDescendKeyPressed && previousKeyDescendState) {
                        ModMessages.sendToServer(new CharacterC2S("isDescendOn", 0));
                        previousKeyDescendState = false; // Actualiza el estado previo
                    }

                    //Turbo
                    if (isTurboKeypressed) {
                        if (!hasPressedTurbo) {
                            previousFov = Minecraft.getInstance().options.fovEffectScale().get();
                            hasPressedTurbo = true;
                        }
                        if (!turboOn && porcentaje > 10) {
                            if (Minecraft.getInstance().options.fovEffectScale().get() != 0.0) Minecraft.getInstance().options.fovEffectScale().set(0.0);
                            // Solo activar Turbo si tiene más del 10% de energía
                            turboOn = true;
                            ModMessages.sendToServer(new CharacterC2S("isTurboOn", 1));
                            ModMessages.sendToServer(new PermaEffC2S("add", "turbo", 1));
                            playSoundOnce(MainSounds.AURA_START.get());
                            startLoopSound(MainSounds.TURBO_LOOP.get(), "turbo");
                        } else if (turboOn) {
                            // Permitir desactivar Turbo incluso si el porcentaje es menor al 10%
                            turboOn = false;
                            ModMessages.sendToServer(new CharacterC2S("isTurboOn", 0));
                            ModMessages.sendToServer(new PermaEffC2S("remove", "turbo", 1));
                            stopLoopSound("turbo");
                            hasPressedTurbo = false;
                        } else {
                            player.displayClientMessage(Component.translatable("ui.dmz.turbo_fail"), true);
                        }
                    }

                    // Desactivar Turbo automáticamente si la energía llega a 1
                    if (turboOn && curEne <= 1) {
                        turboOn = false;
                        ModMessages.sendToServer(new CharacterC2S("isTurboOn", 0));
                        ModMessages.sendToServer(new PermaEffC2S("remove", "turbo", 1));
                        stopLoopSound("turbo");
                        hasPressedTurbo = false;
                    }

                    // Transformación
                    if (isDescendKeyPressed && isTransformKeyPressed) {
                        ModMessages.sendToServer(new CharacterC2S("isTransform", 0));
                        transformOn = false;
                        ModMessages.sendToServer(new CharacterC2S("isAuraOn", 0));
                        stopLoopSound("ki");
                        ModMessages.sendToServer(new DescendFormC2S());
                    } else if (transformOn && !isTransformKeyPressed) { // Al soltar la tecla, desactiva transformación
                        ModMessages.sendToServer(new CharacterC2S("isTransform", 0));
                        ModMessages.sendToServer(new CharacterC2S("isAuraOn", 0));
                        stopLoopSound("ki");
                        stopLoopSound("oozaru");
                        transformOn = false;
                    } else if (getNextForm(stats) != null) {
                        if (isTransformKeyPressed && !transformOn) { // Solo activa si no estaba transformado
                            ModMessages.sendToServer(new CharacterC2S("isTransform", 1));
                            transformOn = true;
                            if (getNextForm(stats).equals("oozaru")) {
                                if (player.getXRot() <= -45.0F && player.level().getMoonPhase() == 0 && player.level().getDayTime() % 24000 >= 13500) {
                                    startLoopSound(MainSounds.OOZARU_HEARTBEAT.get(), "oozaru");
                                }
                            } else {
                                ModMessages.sendToServer(new CharacterC2S("isAuraOn", 1));
                                playSoundOnce(MainSounds.AURA_START.get());
                                startLoopSound(MainSounds.KI_CHARGE_LOOP.get(), "ki");
                            }
                        }

                        if (transformOn && stats.getIntValue("formrelease") >= 100) {
                            ModMessages.sendToServer(new CharacterC2S("isAuraOn", 0));
                            stopLoopSound("ki");
                            stopLoopSound("oozaru");
                        }
                    } else if (isTransformKeyPressed && stats.getIntValue("formrelease") >= 100) {
                        ModMessages.sendToServer(new CharacterC2S("isAuraOn", 0));
                        stopLoopSound("ki");
                        stopLoopSound("oozaru");
                    }

                }
            });
        }
    }

    public static double getPreviousFov() {
        return previousFov;
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onPlayerSizeChange(EntityEvent.Size event) {
        if(event.getEntity() instanceof Player player){
            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
                var raza = cap.getIntValue("race");
                var transf = cap.getStringValue("form");

                switch (raza) {
                    case 1: // Saiyan
                        switch (transf) {
                            case "oozaru", "goldenoozaru":
                                // Ajustamos el ancho y la altura según el estado del jugador
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(2.3F, 2.4F, true));
                                    event.setNewEyeHeight(1.7F);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(2.3F, 6.2F, true));
                                    event.setNewEyeHeight(5.5F);
                                } else if(player.isVisuallyCrawling()){
                                    event.setNewSize(new EntityDimensions(2.3F, 2.4F, true));
                                    event.setNewEyeHeight(1.7F);
                                } else {
                                    event.setNewSize(new EntityDimensions(2.3F, 7.2F, true));
                                    event.setNewEyeHeight(6.7F);
                                }
                                break;
                            case "ssj1":
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(0.6F, 0.6F, true));
                                    event.setNewEyeHeight(0.4f);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(0.5F, 1.6F, true));
                                    event.setNewEyeHeight(1.3f);
                                } else if(player.isVisuallyCrawling()){
                                    event.setNewSize(new EntityDimensions(0.6F, 0.6F, true));
                                    event.setNewEyeHeight(0.4f);
                                } else {
                                    event.setNewSize(new EntityDimensions(0.6F, 1.9F, true));
                                    event.setNewEyeHeight(1.65f);
                                }

                                break;
                            case "ssgrade2":
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(0.7F, 0.6F, true));
                                    event.setNewEyeHeight(0.4F);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(0.7F, 1.5F, true));
                                    event.setNewEyeHeight(1.4F);
                                } else if(player.isVisuallyCrawling()) {
                                    event.setNewSize(new EntityDimensions(0.7F, 0.6F, true));
                                    event.setNewEyeHeight(0.4F);
                                }else {
                                    event.setNewSize(new EntityDimensions(0.7F, 2.0F, true));
                                    event.setNewEyeHeight(1.7F);
                                }
                                break;
                            case "ssgrade3":
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(0.8F, 0.8F, true));
                                    event.setNewEyeHeight(0.5F);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(0.7F, 1.7F, true));
                                    event.setNewEyeHeight(1.6F);
                                } else if(player.isVisuallyCrawling()) {
                                    event.setNewSize(new EntityDimensions(0.8F, 0.8F, true));
                                    event.setNewEyeHeight(0.5F);
                                }else {
                                    event.setNewSize(new EntityDimensions(0.7F, 2.2F, true));
                                    event.setNewEyeHeight(1.9F);
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    case 2: // Namek
                        switch (transf) {
                            case "orange_giant", "giant":
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(2.3F, 2.4F, true));
                                    event.setNewEyeHeight(1.7F);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(2.3F, 6.2F, true));
                                    event.setNewEyeHeight(5.5F);
                                } else if(player.isVisuallyCrawling()){
                                    event.setNewSize(new EntityDimensions(2.3F, 2.4F, true));
                                    event.setNewEyeHeight(1.7F);
                                } else {
                                    event.setNewSize(new EntityDimensions(2.3F, 7.2F, true));
                                    event.setNewEyeHeight(6.7F);
                                }
                                break;
                            case "super_namek":
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(0.6F, 0.6F, true));
                                    event.setNewEyeHeight(0.4f);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(0.5F, 1.6F, true));
                                    event.setNewEyeHeight(1.3f);
                                } else if(player.isVisuallyCrawling()){
                                    event.setNewSize(new EntityDimensions(0.6F, 0.6F, true));
                                    event.setNewEyeHeight(0.4f);
                                } else {
                                    event.setNewSize(new EntityDimensions(0.6F, 1.9F, true));
                                    event.setNewEyeHeight(1.65f);
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    case 3: // BioAndroide
                        switch (transf) {
                            case "semi_perfect":
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(0.7F, 0.6F, true));
                                    event.setNewEyeHeight(0.4F);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(0.7F, 1.5F, true));
                                    event.setNewEyeHeight(1.4F);
                                } else if(player.isVisuallyCrawling()) {
                                    event.setNewSize(new EntityDimensions(0.7F, 0.6F, true));
                                    event.setNewEyeHeight(0.4F);
                                }else {
                                    event.setNewSize(new EntityDimensions(0.7F, 2.0F, true));
                                    event.setNewEyeHeight(1.7F);
                                }
                                break;
                            default:
                                break;
                        }

                        break;
                    case 4: // ColdDemon
                        switch (transf) {
                            case "second_form":
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(1.0F, 0.9F, true));
                                    event.setNewEyeHeight(0.5F);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(0.9F, 2.4F, true));
                                    event.setNewEyeHeight(2.0F);
                                } else if(player.isVisuallyCrawling()) {
                                    event.setNewSize(new EntityDimensions(1.0F, 0.9F, true));
                                    event.setNewEyeHeight(0.5F);
                                }else {
                                    event.setNewSize(new EntityDimensions(0.9F, 2.8F, true));
                                    event.setNewEyeHeight(2.4F);
                                }
                                break;
                            case "third_form":
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(1.0F, 0.9F, true));
                                    event.setNewEyeHeight(0.5F);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(0.9F, 2.2F, true));
                                    event.setNewEyeHeight(1.8F);
                                } else if(player.isVisuallyCrawling()) {
                                    event.setNewSize(new EntityDimensions(1.0F, 0.9F, true));
                                    event.setNewEyeHeight(0.5F);
                                }else {
                                    event.setNewSize(new EntityDimensions(0.8F, 2.6F, true));
                                    event.setNewEyeHeight(2.1F);
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    case 5: // Majin
                        switch (transf){
                            case "kid":
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(0.4F, 0.4F, true));
                                    event.setNewEyeHeight(0.3F);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(0.4F, 1.5F, true));
                                    event.setNewEyeHeight(1.2F);
                                } else if(player.isVisuallyCrawling()) {
                                    event.setNewSize(new EntityDimensions(0.4F, 0.4F, true));
                                    event.setNewEyeHeight(0.3F);
                                }else {
                                    event.setNewSize(new EntityDimensions(0.4F, 1.7F, true));
                                    event.setNewEyeHeight(1.4F);
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    default: //Humano
                        switch (transf) {
                            case "buffed":
                                if (player.isSwimming()) {
                                    event.setNewSize(new EntityDimensions(0.8F, 0.8F, true));
                                    event.setNewEyeHeight(0.5F);
                                } else if (player.isCrouching()) {
                                    event.setNewSize(new EntityDimensions(0.7F, 1.7F, true));
                                    event.setNewEyeHeight(1.6F);
                                } else if(player.isVisuallyCrawling()) {
                                    event.setNewSize(new EntityDimensions(0.8F, 0.8F, true));
                                    event.setNewEyeHeight(0.5F);
                                }else {
                                    event.setNewSize(new EntityDimensions(0.7F, 2.2F, true));
                                    event.setNewEyeHeight(1.9F);
                                }
                                break;
                        }
                        break;
                }


            });
        }

    }

    @OnlyIn(Dist.CLIENT)
    private static void playSoundOnce(SoundEvent soundEvent) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F, false);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void startLoopSound(SoundEvent soundEvent, String sonido) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            SimpleSoundInstance loopSound = new SimpleSoundInstance(
                    soundEvent.getLocation(),
                    SoundSource.PLAYERS,
                    1.0F, 1.0F,
                    player.level().random,
                    true, 0,
                    SimpleSoundInstance.Attenuation.LINEAR,
                    player.getX(), player.getY(), player.getZ(),
                    false
            );

            Minecraft.getInstance().getSoundManager().play(loopSound);
            switch (sonido) {
                case "ki":
                    kiChargeLoop = loopSound;
                    break;
                case "turbo":
                    turboLoop = loopSound;
                    break;
                case "oozaru":
                    oozaruLoop = loopSound;
                    break;
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void stopLoopSound(String sonido) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (sonido.equals("ki") && kiChargeLoop != null) {
                Minecraft.getInstance().getSoundManager().stop(kiChargeLoop);
                kiChargeLoop = null;
            } else if (sonido.equals("turbo") && turboLoop != null) {
                Minecraft.getInstance().getSoundManager().stop(turboLoop);
                turboLoop = null;
            } else if (sonido.equals("oozaru") && oozaruLoop != null) {
                Minecraft.getInstance().getSoundManager().stop(oozaruLoop);
                oozaruLoop = null;
            }
        });
    }

    private static void sonidosGolpes(Player player) {

        SoundEvent[] golpeSounds = {
                MainSounds.GOLPE1.get(),
                MainSounds.GOLPE2.get(),
                MainSounds.GOLPE3.get(),
                MainSounds.GOLPE4.get(),
                MainSounds.GOLPE5.get(),
                MainSounds.GOLPE6.get()
        };

        Random rand = new Random();
        int randomIndex = rand.nextInt(golpeSounds.length);
        SoundEvent randomGolpeSound = golpeSounds[randomIndex];

        player.level().playSound(null, player.getOnPos(), randomGolpeSound, SoundSource.PLAYERS, 2.2F, 0.9F);

    }

    public static int senzuContador(int segundos) {
        if (segundos > 0) return segundos - 1;
        return 0;
    }

    public static int zenkaiContador(int segundos) {
        if (segundos > 0) return segundos - 1;
        return 0;
    }

    public static int babaCooldown(int segundos) {
        if (segundos > 0) return segundos - 1;
        return 0;
    }

    public static int babaDuration(int segundos) {
        if (segundos > 0) return segundos - 1;
        return 0;
    }

    public static String getNextForm(DMZStatsAttributes playerstats) {
        int race = playerstats.getIntValue("race");
        int superFormLvl = playerstats.getFormSkillLevel("super_form");
        String groupForm = playerstats.getStringValue("groupform");
        String dmzForm = playerstats.getStringValue("form");

        // Definir las transformaciones por grupo
        Map<String, String[]> saiyanForms = Map.of(
                "", new String[]{"oozaru", "goldenoozaru"},
                "ssgrades", new String[]{"ssj1", "ssgrade2", "ssgrade3"},
                "ssj", new String[]{"ssjfp", "ssj2", "ssj3"}
        );
        // Lógica de transformación para Humanos
        if (race == 0 && groupForm.equals("")) {
            if (superFormLvl >= 1 && dmzForm.equals("base")) return "buffed";
            if (superFormLvl >= 2 && dmzForm.equals("buffed")) return "full_power";
            if (superFormLvl >= 4 && dmzForm.equals("full_power")) return "potential_unleashed";
        }
        // Lógica de transformación para Saiyans
        if (race == 1) {
            if (!saiyanForms.containsKey(groupForm)) return null;

            // Lógica de transformación para Saiyans
            if (superFormLvl >= 1 && groupForm.equals("") && dmzForm.equals("base")) {
                return "oozaru";
            }
            if (superFormLvl >= 8 && groupForm.equals("") && dmzForm.equals("oozaru")) {
                return "goldenoozaru";
            }
            if (superFormLvl >= 2 && groupForm.equals("ssgrades")) {
                if (superFormLvl >= 2 && dmzForm.equals("base")) return "ssj1";
                if (superFormLvl >= 3 && dmzForm.equals("ssj1")) return "ssgrade2";
                if (superFormLvl >= 4 && dmzForm.equals("ssgrade2")) return "ssgrade3";
            }
            if (superFormLvl >= 5 && groupForm.equals("ssj")) {
                if (superFormLvl >= 5 && dmzForm.equals("base")) return "ssjfp";
                if (superFormLvl >= 6 && dmzForm.equals("ssjfp")) return "ssj2";
                if (superFormLvl >= 7 && dmzForm.equals("ssj2")) return "ssj3";
            }
        }
        // Lógica de transformación para Namekianos
        if (race == 2 && groupForm.equals("")) {
            if (superFormLvl >= 1 && dmzForm.equals("base")) return "giant";
            if (superFormLvl >= 2 && dmzForm.equals("giant")) return "full_power";
            if (superFormLvl >= 4 && dmzForm.equals("full_power")) return "super_namek";
        }

        // Lógica de transformación para Bioandroides
        if (race == 3 && groupForm.equals("")) {
            if (superFormLvl >= 2 && dmzForm.equals("base")) return "semi_perfect";
            if (superFormLvl >= 4 && dmzForm.equals("semi_perfect")) return "perfect";
        }

        // Lógica de transformación para Cold Demons
        if (race == 4 && groupForm.equals("")) {
            if (superFormLvl >= 2 && dmzForm.equals("base")) return "second_form";
            if (superFormLvl >= 4 && dmzForm.equals("second_form")) return "third_form";
            if (superFormLvl >= 6 && dmzForm.equals("third_form")) return "final_form";
            if (superFormLvl >= 8 && dmzForm.equals("final_form")) return "full_power";
        }

        // Lógica de transformación para Majins
        if (race == 5 && groupForm.equals("")) {
            if (superFormLvl >= 2 && dmzForm.equals("base")) return "evil";
            if (superFormLvl >= 4 && dmzForm.equals("evil")) return "kid";
            if (superFormLvl >= 6 && dmzForm.equals("kid")) return "super";
            if (superFormLvl >= 8 && dmzForm.equals("super")) return "ultra";
        }

        return null; // No hay transformación disponible
    }

}


