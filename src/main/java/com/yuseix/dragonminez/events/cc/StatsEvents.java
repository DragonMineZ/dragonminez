package com.yuseix.dragonminez.events.cc;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.config.DMZGeneralConfig;
import com.yuseix.dragonminez.init.MainSounds;
import com.yuseix.dragonminez.init.entity.custom.fpcharacters.AuraEntity;
import com.yuseix.dragonminez.network.C2S.CharacterC2S;
import com.yuseix.dragonminez.network.C2S.InvocarAuraC2S;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import com.yuseix.dragonminez.utils.DMZDatos;
import com.yuseix.dragonminez.utils.Keys;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID)
public class StatsEvents {

    private static int tickcounter = 0;
    private static int energyRegen = 0;
    private static int energiaConsumecounter = 0;
    private static int Senzu_countdown = 0;

    private static int chargeTimer = 0; // Aca calculamos el tiempo de espera
    private static final int CHARGE_INTERVAL = 1 * (20); // No borrar el 20, eso es el tiempo en ticks lo que si puedes configurar es lo que esta la lado


    //Teclas
    private static boolean isActionKeyPressed = false;

    private static int getEnergyRemovalThreshold(int playerLevel) {
        // Asumiendo que cada nivel te da 0.1 bloques de altura de soporte
        return (int) (0.1 * (playerLevel - 100) + 10);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        // Regenerar stamina
        if (event.side == LogicalSide.SERVER) {

            energyRegen++;
            tickcounter++;
            energiaConsumecounter++;

            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, event.player).ifPresent(playerstats -> {
                var vidaMC = 20;
                var con = playerstats.getConstitution();
                var raza = playerstats.getRace();
                var energia = playerstats.getEnergy();

                int maxenergia = DMZDatos.calcularENE(raza, energia, playerstats.getDmzClass());
                int maxstamina = DMZDatos.calcularSTM(raza, DMZDatos.calcularCON(raza, con, vidaMC, playerstats.getDmzClass()));

                // Ajustar la salud máxima del jugador
                event.player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(DMZDatos.calcularCON(raza, con, vidaMC, playerstats.getDmzClass()));

                // Regeneración de stamina
                if (playerstats.getCurStam() >= 0 && playerstats.getCurStam() <= maxstamina) {
                    if (tickcounter >= 60 * 3) { // Cada 3 segundos
                        int regenStamina = (maxstamina / 4);
                        playerstats.addCurStam(regenStamina);
                        tickcounter = 0;
                    }
                }

                // Regeneración de energía Warrior
                if (playerstats.getCurrentEnergy() >= 0 && playerstats.getCurrentEnergy() <= maxenergia) {
                    if (energyRegen >= 60 * 5) { // Cada 5 segundos
                        int regenki = DMZDatos.calcularKiRegen(raza, maxenergia, playerstats.getDmzClass()); // Regenerar 10% de la energía máxima
                        playerstats.addCurEnergy(regenki);
                        energyRegen = 0;
                    }
                }


                //Consumo de energia
                if (playerstats.getCurrentEnergy() >= 0 && playerstats.getCurrentEnergy() <= maxenergia) {
                    if (energiaConsumecounter >= 60 * 3) { // Cada 3 segundos
                        int consumeki = DMZDatos.calcularKiConsume(raza, playerstats.getEnergy(), playerstats.getDmzState());
                        playerstats.removeCurEnergy(consumeki);
                        energiaConsumecounter = 0;
                    }
                }

                //Tiempo para reclamar una senzu
                if (Senzu_countdown > 0) {
                    playerstats.setDmzSenzuDaily(Senzu_countdown / 20);
                    Senzu_countdown--;
                }

                if (Senzu_countdown == 0) {
                    playerstats.setDmzSenzuDaily(0);
                }


                if (playerstats.isAuraOn() || isActionKeyPressed) {
                    // Incrementa el temporizador en cada tick
                    chargeTimer++;

                    // Solo actúa cuando el temporizador ha alcanzado el intervalo definido
                    //Es decir si llega a x ticks carga osea por defecto un segundo
                    if (chargeTimer >= CHARGE_INTERVAL) {
                        if (playerstats.isAuraOn() && isActionKeyPressed) {
                            // Disminuir el valor de charge si ambas teclas están presionadas
                            if (playerstats.getDmzRelease() > 0) {
                                playerstats.setDmzRelease(playerstats.getDmzRelease() - 5);
                                if (playerstats.getDmzRelease() < 0) {
                                    playerstats.setDmzRelease(0); // Asegura que no baje de 1
                                }
                            }
                        } else if (playerstats.isAuraOn()) {
                            // Incrementar el valor de charge si solo KI_CHARGE está presionada

                            if (playerstats.getDmzRelease() < 100) {
                                playerstats.setDmzRelease(playerstats.getDmzRelease() + 5);
                                if (playerstats.getDmzRelease() > 100) {
                                    playerstats.setDmzRelease(100); // Asegura que no pase de 100
                                }
                            }

                            playerstats.addCurEnergy(DMZDatos.calcularCargaKi(maxenergia, playerstats.getDmzClass()));

                        }
                        // Reiniciar el temporizador después de cada acción
                        chargeTimer = 0;
                    }
                }


                updateDMZPermanentEffects((ServerPlayer) event.player);
            });


        }

    }

    private static void updateDMZPermanentEffects(ServerPlayer player) {
        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerStats -> {
            // Obtén el mapa actual de efectos permanentes
            Map<String, Boolean> permanentEffects = playerStats.getDMZPermanentEffects();

            // Aquí puedes realizar la actualización. Por ejemplo, si deseas verificar o refrescar cada efecto
            for (Map.Entry<String, Boolean> entry : permanentEffects.entrySet()) {
                String effectName = entry.getKey();
                Boolean effectState = entry.getValue();

                // Realiza cualquier lógica adicional que necesites, si el estado cambia
                if (effectState == null) {
                    // Asegúrate de manejar efectos nulos
                    permanentEffects.put(effectName, false); // o algún valor predeterminado
                }
            }

            // Asegúrate de sincronizar los datos después de la actualización
            DMZStatsCapabilities.syncStats(player);  // Sincronizamos para asegurarnos de que el cliente reciba los cambios
        });
    }

    @SubscribeEvent
    public static void Recibirdano(LivingHurtEvent event) {
        // Si el que hace el daño es un jugador
        if (event.getSource().getEntity() instanceof Player atacante) {
            // Obtener las estadísticas del atacante
            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, atacante).ifPresent(statsAtacante -> {
                int raza = statsAtacante.getRace();
                int curStamina = statsAtacante.getCurStam();
                float danoDefault = event.getAmount(); // Capturamos el daño original

                // Calcular el daño basado en la fuerza del atacante
                int maxStr = DMZDatos.calcularSTR(raza, statsAtacante.getStrength(), danoDefault, statsAtacante.getDmzState(), statsAtacante.getDmzRelease(), statsAtacante.getDmzClass());
                int staminacost = maxStr / 12;

                if (curStamina >= staminacost) {
                    // Si el atacante tiene suficiente stamina, aplicar el daño basado en la fuerza
                    event.setAmount(maxStr);
                    // Descontar stamina del atacante
                    statsAtacante.removeCurStam(staminacost);
                    sonidosGolpes(atacante);
                } else {
                    // Daño por defecto si al atacante le falta stamina
                    event.setAmount(danoDefault);
                }
            });

            // Si la entidad que recibe el daño es un jugador
            if (event.getEntity() instanceof Player objetivo) {
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, objetivo).ifPresent(statsObjetivo -> {
                    int defObjetivo = DMZDatos.calcularDEF(statsObjetivo.getRace(), statsObjetivo.getDefense(), statsObjetivo.getDmzState(), statsObjetivo.getDmzRelease(), statsObjetivo.getDmzClass());
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
                    int defObjetivo = DMZDatos.calcularDEF(statsObjetivo.getRace(), statsObjetivo.getDefense(), statsObjetivo.getDmzState(), statsObjetivo.getDmzRelease(), statsObjetivo.getDmzClass());
                    // Restar la defensa del objetivo al daño
                    float danoFinal = event.getAmount() - defObjetivo;
                    event.setAmount(Math.max(danoFinal, 1)); // Asegurarse de que al menos se haga 1 de daño
                });
            }
        }
    }


    @SubscribeEvent
    public static void livingFallEvent(LivingFallEvent event) {
        float realDistance = event.getDistance();

        if (event.getEntity() instanceof ServerPlayer player) {
            if (realDistance > 4.5f) {

                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
                    int level = (stats.getStrength() +
                            stats.getDefense() +
                            stats.getConstitution() +
                            stats.getKiPower() +
                            stats.getEnergy()) / 5;

                    double energyToRemove = getEnergyToRemove(level);

                    // Checar si la distancia de caída es menor al soporte que puedes tener por x nivel
                    if ((int) realDistance <= getEnergyRemovalThreshold(level) /* && stats.getCurrentEnergy() >= energyToRemove */) {
                        stats.removeCurEnergy((int) Math.round(energyToRemove));
                        event.setCanceled(true);
                    } //else hacer algo si te haces más daño de ki del que puedes soportar
                });
            }
        }

    }

    @SubscribeEvent
    public static void onKeyInputEvent(InputEvent.Key event) {
        // Detecta si la tecla KI_CHARGE está presionada o liberada
        if (Keys.KI_CHARGE.isDown()) {
            ModMessages.sendToServer(new CharacterC2S("isAuraOn", 1));
            ModMessages.sendToServer(new InvocarAuraC2S());

        } else {
            ModMessages.sendToServer(new CharacterC2S("isAuraOn", 0));
            ModMessages.sendToServer(new InvocarAuraC2S());
        }

        // Detecta si la tecla DESCEND_KEY está presionada o liberada
        isActionKeyPressed = Keys.DESCEND_KEY.isDown();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            AuraEntity aura = InvocarAuraC2S.playerAuraMap.remove(playerId); // Elimina el aura del mapa

            if (aura != null) {
                aura.remove(Entity.RemovalReason.DISCARDED); // Remueve el aura del mundo si aún existe
            }
        }
    }

    private static double getEnergyToRemove(int level) {
        double energyRemovalValue;

        double baseReduction = DMZGeneralConfig.MULTIPLIER_FALLDMG.get();

        if (level >= 100) {
            // Porcentaje calculado en base a la config (Default 0.03)
            energyRemovalValue = level * baseReduction;

        } else {
            energyRemovalValue = 0; // Niveles menores a 100 no reciben daño de ki
        }

        // Devuelve el valor actual de energía sacada
        return energyRemovalValue;
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

    // Método para iniciar la cuenta regresiva
    public static void startSenzuCountdown(Player player, int seconds) {
        player.getCapability(DMZStatsCapabilities.INSTANCE).ifPresent(playerstats -> {
            // Convertir segundos a ticks y asignar a Senzu_countdown
            Senzu_countdown = seconds * 20;
            playerstats.setDmzSenzuDaily(seconds); // Inicializa el valor en segundos
        });
    }


}



