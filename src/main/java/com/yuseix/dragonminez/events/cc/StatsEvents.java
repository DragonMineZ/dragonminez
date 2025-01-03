package com.yuseix.dragonminez.events.cc;


import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.config.DMZGeneralConfig;
import com.yuseix.dragonminez.init.MainSounds;
import com.yuseix.dragonminez.network.C2S.CharacterC2S;
import com.yuseix.dragonminez.network.C2S.PermaEffC2S;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import com.yuseix.dragonminez.utils.DMZDatos;
import com.yuseix.dragonminez.utils.Keys;
import com.yuseix.dragonminez.utils.TickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StatsEvents {

    private static final Map<UUID, TickHandler> playerTickHandlers = new HashMap<>();

    //Teclas
    private static boolean previousKeyDescendState = false;
    private static boolean previousKiChargeState = false;
    private static boolean turboOn = false;

    //Sonidos
    private static SimpleSoundInstance kiChargeLoop;
    private static SimpleSoundInstance turboLoop;


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
                var vidaMC = 20;
                var con = playerstats.getConstitution();
                var raza = playerstats.getRace();
                var energia = playerstats.getEnergy();

                int maxenergia = dmzdatos.calcularENE(raza, energia, playerstats.getDmzClass());

                // Ajustar la salud máxima del jugador
                serverPlayer.getAttribute(Attributes.MAX_HEALTH).setBaseValue(dmzdatos.calcularCON(raza, con, vidaMC, playerstats.getDmzClass()));

                // Tickhandler
                tickHandler.tickRegenConsume(playerstats, dmzdatos);

                //Tiempo para reclamar una senzu
                playerstats.setDmzSenzuDaily(senzuContador(playerstats.getDmzSenzuDaily()));

                //Aca manejamos la carga de aura
                tickHandler.manejarCargaDeAura(playerstats, maxenergia);

                //Restar el tiempo que se pone en el comando dmztempeffect
                updateTemporaryEffects(serverPlayer);


            });
    }


    private static void updateTemporaryEffects(Player player) {
        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
            Iterator<Map.Entry<String, Integer>> iterator = playerstats.getDMZTemporalEffects().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Integer> entry = iterator.next();
                int timeLeft = entry.getValue() - 1;  // Reducir en 1 tick cada vez
                if (timeLeft <= 0) {
                    playerstats.removeTemporalEffect(entry.getKey());  // Usa el método para eliminar el efecto
                } else {
                    entry.setValue(timeLeft);  // Actualiza el tiempo restante
                }
            }
        });
    }

    @SubscribeEvent
    public static void Recibirdano(LivingHurtEvent event) {

        DMZDatos dmzdatos = new DMZDatos();

        // Si el que hace el daño es un jugador
        if (event.getSource().getEntity() instanceof Player atacante) {
            // Obtener las estadísticas del atacante
            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, atacante).ifPresent(cap -> {
                int raza = cap.getRace();
                int curStamina = cap.getCurStam();
                var majinOn = cap.hasDMZPermaEffect("majin");
                var mightfruitOn = cap.hasDMZTemporalEffect("mightfruit");

                float danoDefault = event.getAmount(); // Capturamos el daño original

                // Calcular el daño basado en la fuerza del atacante
                int maxStr = dmzdatos.calcularSTR(raza, cap.getStrength(), danoDefault, cap.getDmzState(),
                        cap.getDmzRelease(), cap.getDmzClass(), majinOn, mightfruitOn);

                int staminacost = maxStr / 12;

                if (curStamina >= staminacost) {
                    // Si el atacante tiene suficiente stamina, aplicar el daño basado en la fuerza
                    event.setAmount(maxStr);
                    // Descontar stamina del atacante
                    cap.removeCurStam(staminacost);
                    sonidosGolpes(atacante);
                } else {
                    // Daño por defecto si al atacante le falta stamina
                    event.setAmount(danoDefault);
                }
            });

            // Si la entidad que recibe el daño es un jugador
            if (event.getEntity() instanceof Player objetivo) {
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, objetivo).ifPresent(statsObjetivo -> {
                    var majinOn = statsObjetivo.hasDMZPermaEffect("majin");
                    var fruta = statsObjetivo.hasDMZTemporalEffect("mightfruit");

                    int defObjetivo = dmzdatos.calcularDEF(objetivo, statsObjetivo.getRace(), statsObjetivo.getDefense(), statsObjetivo.getDmzState(), statsObjetivo.getDmzRelease(), statsObjetivo.getDmzClass(), majinOn, fruta);
                    // Restar la defensa del objetivo al daño
                    float danoFinal = event.getAmount() - defObjetivo;
                    event.setAmount(Math.max(danoFinal, 1)); // Asegurarse de que al menos se haga 1 de daño
                });
            } else {
                // Si golpeas a otra entidad (no jugador), aplica el daño máximo basado en la fuerza
                event.setAmount(event.getAmount()); // Aplica tu máximo daño
            }
        } else {
            // Aquí manejamos el caso donde el atacante no es un jugador
            if (event.getEntity() instanceof Player objetivo) {
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, objetivo).ifPresent(statsObjetivo -> {
                    var majinOn = statsObjetivo.hasDMZPermaEffect("majin");
                    var fruta = statsObjetivo.hasDMZTemporalEffect("mightfruit");

                    int defObjetivo = dmzdatos.calcularDEF(objetivo, statsObjetivo.getRace(), statsObjetivo.getDefense(),
                            statsObjetivo.getDmzState(), statsObjetivo.getDmzRelease(),
                            statsObjetivo.getDmzClass(), majinOn, fruta);

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

                    int maxEnergy = dmzdatos.calcularENE(stats.getRace(), stats.getEnergy(), stats.getDmzClass());

                    // drenaje de config
                    int baseEnergyDrain = (int) Math.ceil(maxEnergy * DMZGeneralConfig.MULTIPLIER_FALLDMG.get());

                    // Incrementar el drenaje por altura
                    int extraEnergyDrain = (int) ((fallDistance - 4.5f) * baseEnergyDrain / 4.5f);

                    int totalEnergyDrain = baseEnergyDrain + extraEnergyDrain;

                    // Solo drenar energía si el jugador tiene suficiente y cancelar el daño
                    if (stats.getCurrentEnergy() >= totalEnergyDrain) {
                        stats.removeCurEnergy(totalEnergyDrain);
                        event.setCanceled(true);
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

        LocalPlayer player = Minecraft.getInstance().player;

        //Cargar Ki
        if (isKiChargeKeyPressed && !previousKiChargeState) {
            ModMessages.sendToServer(new CharacterC2S("isAuraOn", 1));
            previousKiChargeState = true; // Actualiza el estado previo
            playSoundOnce(MainSounds.AURA_START.get());
            startLoopSound(MainSounds.KI_CHARGE_LOOP.get(), true);
        } else if (!isKiChargeKeyPressed && previousKiChargeState) {
            ModMessages.sendToServer(new CharacterC2S("isAuraOn", 0));
            previousKiChargeState = false; // Actualiza el estado previo
            stopLoopSound(true);
        }

        //Turbo
        if (player != null) {
            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
                int curEne = stats.getCurrentEnergy();
                int maxEne = stats.getMaxEnergy();
                int porcentaje = (int) Math.ceil((curEne * 100) / maxEne);

                if (isTurboKeypressed) {
                    if (!turboOn && porcentaje > 10) {
                        // Solo activar Turbo si tiene más del 10% de energía
                        turboOn = true;
                        ModMessages.sendToServer(new CharacterC2S("isTurboOn", 1));
                        ModMessages.sendToServer(new PermaEffC2S("add", "turbo", 1));
                        playSoundOnce(MainSounds.AURA_START.get());
                        startLoopSound(MainSounds.TURBO_LOOP.get(), false);
                        setTurboSpeed(player, true);
                    } else if (turboOn) {
                        // Permitir desactivar Turbo incluso si el porcentaje es menor al 10%
                        turboOn = false;
                        ModMessages.sendToServer(new CharacterC2S("isTurboOn", 0));
                        ModMessages.sendToServer(new PermaEffC2S("remove", "turbo", 1));
                        stopLoopSound(false);
                        setTurboSpeed(player, false);
                    } else {
                        player.displayClientMessage(Component.translatable("ui.dmz.turbo_fail"), true);
                    }
                }

                // Desactivar Turbo automáticamente si la energía llega a 1
                if (turboOn && curEne <= 1) {
                    turboOn = false;
                    ModMessages.sendToServer(new CharacterC2S("isTurboOn", 0));
                    ModMessages.sendToServer(new PermaEffC2S("remove", "turbo", 1));
                    stopLoopSound(false);
                    setTurboSpeed(player, false);
                }
            });

            // Descender de ki
            if (isDescendKeyPressed && !previousKeyDescendState) {
                ModMessages.sendToServer(new CharacterC2S("isDescendOn", 1));
                previousKeyDescendState = true; // Actualiza el estado previo
            } else if (!isDescendKeyPressed && previousKeyDescendState) {
                ModMessages.sendToServer(new CharacterC2S("isDescendOn", 0));
                previousKeyDescendState = false; // Actualiza el estado previo
            }
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
    private static void startLoopSound(SoundEvent soundEvent, boolean isKiCharge) {
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
            if (isKiCharge) {
                kiChargeLoop = loopSound;
            } else {
                turboLoop = loopSound;
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void stopLoopSound(boolean isKiCharge) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (isKiCharge && kiChargeLoop != null) {
                Minecraft.getInstance().getSoundManager().stop(kiChargeLoop);
                kiChargeLoop = null;
            } else if (!isKiCharge && turboLoop != null) {
                Minecraft.getInstance().getSoundManager().stop(turboLoop);
                turboLoop = null;
            }
        });
    }

    private static final double originalSpeed = 0.10000000149011612;

    private static void setTurboSpeed(Player player, boolean enable) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        if (enable) {
            speedAttribute.setBaseValue(originalSpeed + 0.06);
        } else {
            speedAttribute.setBaseValue(originalSpeed);
        }
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
        if (segundos > 0) {
            return segundos - 1; // si es mayor a 0 resta
        }
        return 0; // Si es 0 o menor, retorna 0
    }


}



