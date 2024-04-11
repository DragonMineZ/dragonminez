package com.yuseix.dragonminez.events;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.character.LayerDMZBase;
import com.yuseix.dragonminez.character.LayerDMZPost;
import com.yuseix.dragonminez.config.DMCAttrConfig;
import com.yuseix.dragonminez.init.MainSounds;
import com.yuseix.dragonminez.stats.PlayerStatsAttrProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID)
public class StatsEvents {

    private static int tickcounter = 0;
    private static int energiacounter = 0;

    private static int getEnergyRemovalThreshold(int playerLevel) {
        // Asumiendo que cada nivel te da 0.1 bloques de altura de soporte
        return (int) (0.1 * (playerLevel - 100) + 10);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {


        //Regenerar stamina
        if (event.side == LogicalSide.SERVER) {

            energiacounter++;
            tickcounter++;

            PlayerStatsAttrProvider.getCap(ModEvents.INSTANCE, event.player).ifPresent(playerstats -> {

                int maxcon = (int) (playerstats.getConstitution() * 0.5) * DMCAttrConfig.MULTIPLIER_CON.get();
                int maxstamina = (playerstats.getStamina() + 3);
                int maxenergia = (int) (playerstats.getEnergy() * 0.5) * DMCAttrConfig.MULTIPLIER_ENERGY.get();


                if (playerstats.getCurStam() >= 0 && playerstats.getCurStam() <= maxstamina) {

                    if (tickcounter >= 60 * 3) {

                        int regenStamina = ((maxstamina) / 4);

                        playerstats.addCurStam(regenStamina);

                        tickcounter = 0;

                    }

                }
                if (playerstats.getCurrentEnergy() >= 0 && playerstats.getCurrentEnergy() <= maxenergia) {
                    if (energiacounter >= 60 * 5) {

                        int regenki = ((maxenergia) / 10);

                        playerstats.addCurEnergy(regenki);

                        energiacounter = 0;
                    }
                }

                event.player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxcon);

            });

        }


    }

    @SubscribeEvent
    public static void Recibirdano(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player)) {  //LA ENTIDAD QUE RECIBE EL GOLPE NO ES UN JUGADOR
            if (event.getSource().getEntity() instanceof Player jugadorpemrd) { //SI EL QUE HACE DANO ES UN JUGADOR

                PlayerStatsAttrProvider.getCap(ModEvents.INSTANCE, event.getSource().getEntity()).ifPresent(playerstats -> {

                    int raza = playerstats.getRace();

                    int staminacost, curstamina;

                    switch (raza) {
                        case 0: //TODO: HUMANO
                            int maxstrHUMANO = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR.get();

                            staminacost = (maxstrHUMANO / 4);

                            curstamina = playerstats.getCurStam();

                            if (curstamina >= staminacost) {
                                event.setAmount(maxstrHUMANO);
                                playerstats.removeCurStam(staminacost);
                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;
                        case 1: //TODO: SAIYAN
                            int maxstrSaiyan = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR_SAIYAN.get();

                            staminacost = (maxstrSaiyan / 4);

                            curstamina = playerstats.getCurStam();

                            if (curstamina >= staminacost) {
                                event.setAmount(maxstrSaiyan);
                                playerstats.removeCurStam(staminacost);
                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;
                        case 2: //TODO: NAMEKIANO
                            int maxstrNamek = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR.get(); //CAMBIAR ESTO POR NAMEKIANO

                            staminacost = (maxstrNamek / 4);

                            curstamina = playerstats.getCurStam();

                            if (curstamina >= staminacost) {
                                event.setAmount(maxstrNamek);
                                playerstats.removeCurStam(staminacost);
                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;
                        case 3: //TODO: BIOANDROIDE
                            int maxstrBioAndroide = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR.get(); //CAMBIAR ESTO POR BIOANDROIDE CONFIG

                            staminacost = (maxstrBioAndroide / 4);

                            curstamina = playerstats.getCurStam();

                            if (curstamina >= staminacost) {
                                event.setAmount(maxstrBioAndroide);
                                playerstats.removeCurStam(staminacost);
                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;
                        case 4: //TODO: COLDDEMON
                            int maxstrColdDemon = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR.get(); //CAMBIAR ESTO POR COLDDEMON CONFIG

                            staminacost = (maxstrColdDemon / 4);

                            curstamina = playerstats.getCurStam();

                            if (curstamina >= staminacost) {
                                event.setAmount(maxstrColdDemon);
                                playerstats.removeCurStam(staminacost);
                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;
                        case 5: //TODO: MAJIN
                            int maxstrMajin = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR.get(); //CAMBIAR ESTO POR MAJIN CONFIG

                            staminacost = (maxstrMajin / 4);

                            curstamina = playerstats.getCurStam();

                            if (curstamina >= staminacost) {
                                event.setAmount(maxstrMajin);
                                playerstats.removeCurStam(staminacost);
                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }
                            break;

                        default: //TODO: FUTURAS RAZAS

                            break;

                    }

                });

                sonidosGolpes(jugadorpemrd);
            }
        }

        if (event.getEntity() instanceof Player) { //SI LA ENTIDAD QUE RECIBE UN GOLPE ES UN JUGADOR

            if (!(event.getSource().getEntity() instanceof Player)) { //SI LA ENTIDAD QUE HACE DANO NO ES UN JUGADOR

                PlayerStatsAttrProvider.getCap(ModEvents.INSTANCE, event.getEntity()).ifPresent(playerstats -> {
                    int raza = playerstats.getRace();

                    switch (raza) {
                        case 0: //RAZA HUMANO

                            int defensaHumano = (int) (((playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF.get()) / 2);
                            event.setAmount(event.getAmount() - defensaHumano);

                            break;
                        case 1: //RAZA SAIYAN

                            int defensaSaiyan = (int) (((playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF_SAIYAN.get()) / 2);
                            event.setAmount(event.getAmount() - defensaSaiyan);

                            break;
                        case 2: //RAZA NAMEK

                            int defensaNamek = (int) (((playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF_SAIYAN.get()) / 2);
                            event.setAmount(event.getAmount() - defensaNamek);

                            break;
                        case 3: //RAZA BIOANDROIDE

                            int defensaBioAndroide = (int) (((playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF_SAIYAN.get()) / 2);
                            event.setAmount(event.getAmount() - defensaBioAndroide);

                            break;
                        case 4: //RAZA COLD DEMON

                            int defensaColdDemon = (int) (((playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF_SAIYAN.get()) / 2);
                            event.setAmount(event.getAmount() - defensaColdDemon);

                            break;
                        case 5: //RAZA MAJIN

                            int defensaMajin = (int) (((playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF_SAIYAN.get()) / 2);
                            event.setAmount(event.getAmount() - defensaMajin);

                            break;
                        default:

                            break;
                    }


                });
            }

            if ((event.getSource().getEntity() instanceof Player jugadorpemrd)) { //SI LA ENTIDAD QUE HACE DANO ES UN JUGADOR

                PlayerStatsAttrProvider.getCap(ModEvents.INSTANCE, event.getEntity()).ifPresent(playerstats -> {

                    int razas = playerstats.getRace();

                    int maxstr, maxdef;
                    int staminacost;
                    int curstamina = playerstats.getCurStam();
                    switch (razas) {
                        case 0: //RAZA HUMANO

                            maxstr = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR.get();
                            maxdef = (int) (playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF.get();

                            staminacost = (maxstr / 4);

                            if (curstamina >= staminacost) {

                                event.setAmount(event.getAmount() - ((float) maxdef / 3));
                                playerstats.removeCurStam(staminacost);

                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;
                        case 1: //RAZA SAIYAN

                            maxstr = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR_SAIYAN.get();
                            maxdef = (int) (playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF_SAIYAN.get();

                            staminacost = (maxstr / 4);

                            if (curstamina >= staminacost) {

                                event.setAmount(event.getAmount() - ((float) maxdef / 3));
                                playerstats.removeCurStam(staminacost);

                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;
                        case 2: //RAZA NAMEK

                            maxstr = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR_SAIYAN.get();
                            maxdef = (int) (playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF_SAIYAN.get();

                            staminacost = (maxstr / 4);

                            if (curstamina >= staminacost) {

                                event.setAmount(event.getAmount() - ((float) maxdef / 3));
                                playerstats.removeCurStam(staminacost);

                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;
                        case 3: //RAZA BIOANDROIDE

                            maxstr = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR_SAIYAN.get();
                            maxdef = (int) (playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF_SAIYAN.get();

                            staminacost = (maxstr / 4);

                            if (curstamina >= staminacost) {

                                event.setAmount(event.getAmount() - ((float) maxdef / 3));
                                playerstats.removeCurStam(staminacost);

                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;
                        case 4: //RAZA COLD DEMON

                            maxstr = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR_SAIYAN.get();
                            maxdef = (int) (playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF_SAIYAN.get();

                            staminacost = (maxstr / 4);

                            if (curstamina >= staminacost) {

                                event.setAmount(event.getAmount() - ((float) maxdef / 3));
                                playerstats.removeCurStam(staminacost);

                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;
                        case 5: //RAZA MAJIN

                            maxstr = (int) (playerstats.getStrength() * 0.5) * DMCAttrConfig.MULTIPLIER_STR_SAIYAN.get();
                            maxdef = (int) (playerstats.getDefense() * 0.5) * DMCAttrConfig.MULTIPLIER_DEF_SAIYAN.get();

                            staminacost = (maxstr / 4);

                            if (curstamina >= staminacost) {

                                event.setAmount(event.getAmount() - ((float) maxdef / 3));
                                playerstats.removeCurStam(staminacost);

                            }
                            if (staminacost >= curstamina) {
                                event.setAmount(1);
                            }

                            break;

                        default: //futuras razas

                            break;
                    }


                });

                sonidosGolpes(jugadorpemrd);
            }
        }
    }

    @SubscribeEvent
    public static void livingFallEvent(LivingFallEvent event) {
        float realDistance = event.getDistance();

        if (event.getEntity() instanceof ServerPlayer player) {
            if (realDistance > 4.5f) {

                PlayerStatsAttrProvider.getCap(ModEvents.INSTANCE, player).ifPresent(stats -> {
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


    @SuppressWarnings({"deprecation", "removal"})
    @SubscribeEvent
    public static void cambiarTamano(EntityEvent.Size event) {

        //Obtenemos el tamaño de los atributos maximos
        float atributosMaximos = DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get();

        PlayerStatsAttrProvider.getCap(ModEvents.INSTANCE, event.getEntity()).ifPresent(cap -> {

            //Obtenemos los puntos de constitucion del jugador
            int vidaJugador = cap.getConstitution();

            //Obtenemos las razas
            int razas = cap.getRace();


            if (razas == 0) { //TODO: HUMANO
                //tamaño default de la hitbox
                float xhitbox = 0.52f;
                float yhitbox = 1.65f;

                //Calculo de la estatura camara y hitbox
                float estaturaCon = Math.min(((0.21f * vidaJugador) / atributosMaximos), 0.21f);

                //Colocamos la nueva hitbox (ESTO ES IMPORTANTE PARA EL RENDERIZADO DE MODELOS A FUTURO! )
                EntityDimensions hitboxBase = new EntityDimensions((xhitbox + estaturaCon) - 0.02f, (yhitbox + estaturaCon) + 0.02f, event.getNewSize().fixed);

                event.setNewSize(hitboxBase);

                //Ponemos que la camara se coloque.
                event.setNewEyeHeight(yhitbox + estaturaCon);

                //Obtenemos el evento si el jugador esta en shift
                if (event.getEntity().isShiftKeyDown()) {
                    EntityDimensions hitboxShift = new EntityDimensions((xhitbox + estaturaCon) - 0.02f, (yhitbox + estaturaCon) - 0.07f, event.getNewSize().fixed);
                    event.setNewSize(hitboxShift);
                    event.setNewEyeHeight((yhitbox - 0.35f) + estaturaCon);
                }

                if (event.getEntity().isSwimming()) {
                    EntityDimensions hitboxSwimming = new EntityDimensions((xhitbox + estaturaCon) + 0.02f, (yhitbox + estaturaCon) - 1.01f, event.getNewSize().fixed);
                    event.setNewSize(hitboxSwimming);
                    event.setNewEyeHeight((yhitbox - 1.35f) + estaturaCon);
                }

                if (event.getEntity().isVisuallyCrawling()) {
                    EntityDimensions hitboxCrawling = new EntityDimensions((xhitbox + estaturaCon) + 0.02f, (yhitbox + estaturaCon) - 1.01f, event.getNewSize().fixed);
                    event.setNewSize(hitboxCrawling);
                    event.setNewEyeHeight((yhitbox - 1.35f) + estaturaCon);
                }

            }


        });


    }

    @SubscribeEvent
    public static void changeSizePRE(RenderPlayerEvent.Pre event) {

        PlayerStatsAttrProvider.getCap(ModEvents.INSTANCE, event.getEntity()).ifPresent(cap -> {

            //Tamaño del jugador dependiento lo atributos maximo de la config(ALPHA)
            int atributosMAX = DMCAttrConfig.MAX_ATTRIBUTE_VALUE.get();

            //Obtener los puntos de constitucion del jugador
            int vidaJugador = cap.getConstitution();

            //int porcentaje = (int) (vidaJugador / atributosMAX) * 100; //porcentaje del jugador respecto a la vida

            int razas = cap.getRace();

            event.getPoseStack().pushPose();

            if (razas == 0) {

                //Escala default
                float xyz = 1.0f;

                //Calculamos la estatura para que aumente pero no pase de 0.5f
                float estaturaCon = Math.min(((0.18f * vidaJugador) / atributosMAX), 0.18f);

                //Sumamos la escala default + el calculo del aumento
                float estaturaBase = xyz + estaturaCon;

                event.getPoseStack().scale(estaturaBase, estaturaBase, estaturaBase);


            }

        });


    }


    @SubscribeEvent
    public static void nose(RenderPlayerEvent.Pre event) {
        event.getRenderer().addLayer(new LayerDMZBase(event.getRenderer()));

    }


    @SubscribeEvent
    public static void changeSizePOST(RenderPlayerEvent.Post event) {

        event.getPoseStack().popPose();
        event.getRenderer().addLayer(new LayerDMZPost(event.getRenderer()));
    }

    private static double getEnergyToRemove(int level) {
        double energyRemovalValue;

        double baseReduction = DMCAttrConfig.MULTIPLIER_FALLDMG.get();

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

}



