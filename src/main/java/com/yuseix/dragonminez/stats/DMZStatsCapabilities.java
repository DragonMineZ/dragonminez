package com.yuseix.dragonminez.stats;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.network.S2C.DMZPermanentEffectsSyncS2C;
import com.yuseix.dragonminez.network.S2C.DMZSkillsS2C;
import com.yuseix.dragonminez.network.S2C.DMZTempEffectsS2C;
import com.yuseix.dragonminez.network.S2C.StatsSyncS2C;
import com.yuseix.dragonminez.utils.DMZDatos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID)
public class DMZStatsCapabilities {

    private DMZDatos dmzdatos = new DMZDatos();

    public static final Capability<DMZStatsAttributes> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent event) {
        syncStats(event.getEntity());
        syncPermanentEffects(event.getEntity());
        syncTempEffects(event.getEntity());
        syncSkills(event.getEntity());

        event.getEntity().refreshDimensions();

        DMZStatsProvider.getCap(INSTANCE, event.getEntity()).ifPresent(cap -> {

            var vidaMC = event.getEntity().getAttribute(Attributes.MAX_HEALTH).getBaseValue();
            var con = cap.getConstitution();
            var raza = cap.getRace();

            event.getEntity().getAttribute(Attributes.MAX_HEALTH).setBaseValue(dmzdatos.calcularCON(raza, con, 20, cap.getDmzClass()));
        });


    }

    @SubscribeEvent
    public void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncStats(event.getEntity());
        syncPermanentEffects(event.getEntity());
        syncTempEffects(event.getEntity());
        syncSkills(event.getEntity());

    }

    @SubscribeEvent
    public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncStats(event.getEntity());
        syncPermanentEffects(event.getEntity());
        syncTempEffects(event.getEntity());
        syncSkills(event.getEntity());

        DMZStatsProvider.getCap(INSTANCE, event.getEntity()).ifPresent(cap -> {

            var vidaMC = 20;
            var con = cap.getConstitution();
            var raza = cap.getRace();
            var energia = cap.getEnergy();
            var maxVIDA = 0.0;

            //VIDAAAAAAA
            maxVIDA = dmzdatos.calcularCON(raza, con, vidaMC,cap.getDmzClass());
            event.getEntity().getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxVIDA);
            event.getEntity().heal((float) maxVIDA);
            cap.setCurStam(dmzdatos.calcularSTM(raza, (int) maxVIDA));

            //ENERGIAAA
            cap.setCurrentEnergy(dmzdatos.calcularENE(raza, energia, cap.getDmzClass()));
        });

    }

    @SubscribeEvent
    public void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(DMZStatsAttributes.class);
    }

    @SubscribeEvent
    public void onPlayerCloned(PlayerEvent.Clone event) {
        var player = event.getEntity();
        var original = event.getOriginal();

        original.reviveCaps();

        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(
                cap -> DMZStatsProvider.getCap(INSTANCE, original).ifPresent(originalcap ->
                        cap.loadNBTData(originalcap.saveNBTData())));


        original.invalidateCaps();

    }

    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking event) {
        var trackingplayer = event.getEntity();
        if (!(trackingplayer instanceof ServerPlayer serverplayer)) return;

        var tracked = event.getTarget();
        if (tracked instanceof ServerPlayer trackedplayer) {
            DMZStatsProvider.getCap(INSTANCE, tracked).ifPresent(cap -> {

                ModMessages.sendToPlayer(new StatsSyncS2C(trackedplayer), serverplayer);

                ModMessages.sendToPlayer(
                        new DMZPermanentEffectsSyncS2C(trackedplayer, cap.getDMZPermanentEffects()),
                        serverplayer
                );

                ModMessages.sendToPlayer(
                        new DMZTempEffectsS2C(trackedplayer, cap.getDMZTemporalEffects()),
                        serverplayer
                );

                ModMessages.sendToPlayer(
                        new DMZSkillsS2C(trackedplayer, cap.getDMZSkills()),
                        serverplayer
                );

            });

        }
    }

    public static void syncStats(Player player) {
        ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new StatsSyncS2C(player));

    }

    public static void syncPermanentEffects(Player player) {
        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
            ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new DMZPermanentEffectsSyncS2C(player, cap.getDMZPermanentEffects()));
        });
    }
    public static void syncTempEffects(Player player) {
        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
            ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new DMZTempEffectsS2C(player, cap.getDMZTemporalEffects()));
        });
    }
    public static void syncSkills(Player player) {
        DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
            ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new DMZSkillsS2C(player, cap.getDMZSkills()));
        });
    }

}
