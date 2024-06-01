package com.yuseix.dragonminez.stats;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.config.DMCAttrConfig;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.network.S2C.StatsSyncS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID)
public class DMZCapabilities {

    public static final Capability<DMZStatsAttributes> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {
    });

    @SubscribeEvent
    public static void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
        event.getEntity().refreshDimensions();

        DMZStatsProvider.getCap(INSTANCE, event.getEntity()).ifPresent(cap ->
                event.getEntity().getAttribute(Attributes.MAX_HEALTH).setBaseValue((cap.getConstitution() * DMCAttrConfig.MULTIPLIER_CON.get())));
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());

        DMZStatsProvider.getCap(INSTANCE, event.getEntity()).ifPresent(cap -> {

            event.getEntity().getAttribute(Attributes.MAX_HEALTH).setBaseValue((cap.getConstitution()) * DMCAttrConfig.MULTIPLIER_CON.get());
            event.getEntity().heal((float) (cap.getConstitution() * DMCAttrConfig.MULTIPLIER_CON.get()));

            int maxEnergia = (int) (cap.getEnergy() * DMCAttrConfig.MULTIPLIER_ENERGY.get()) ;
            int maxStamina = cap.getStamina() + 3;

            cap.setCurrentEnergy(maxEnergia);
            cap.setStamina(maxStamina);

        });

    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(DMZStatsAttributes.class);
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {

        event.getOriginal().reviveCaps();

        DMZStatsProvider.getCap(DMZCapabilities.INSTANCE, event.getEntity()).ifPresent(
                cap -> DMZStatsProvider.getCap(INSTANCE, event.getOriginal()).ifPresent(originalcap ->
                        cap.loadNBTData(originalcap.saveNBTData())));


        event.getOriginal().invalidateCaps();

    }

    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking event) {
        var trackingplayer = event.getEntity();
        if (!(trackingplayer instanceof ServerPlayer serverplayer)) return;

        var tracked = event.getTarget();
        if (tracked instanceof ServerPlayer trackedplayer) {
            DMZStatsProvider.getCap(INSTANCE, tracked).ifPresent(cap -> ModMessages.sendToPlayer(
                    new StatsSyncS2C(trackedplayer), serverplayer));
        }
    }

    public static void sync(Player player) {
        ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new StatsSyncS2C(player));
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            if (event.getObject().getCapability(INSTANCE).isPresent()) {
                return;
            }
            //System.out.println("Añadiendo capability");
            final DMZStatsProvider provider = new DMZStatsProvider(player);

            event.addCapability(DMZStatsProvider.ID, provider);

        }
    }

}