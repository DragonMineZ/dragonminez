package com.dragonminez.common.network;

import com.dragonminez.Reference;
import com.dragonminez.common.network.C2S.*;
import com.dragonminez.common.network.S2C.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    public static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "network"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

		/*
		  CLIENT -> SERVER
		 */
		net.messageBuilder(CreateCharacterC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CreateCharacterC2S::decode)
                .encoder(CreateCharacterC2S::encode)
                .consumerMainThread(CreateCharacterC2S::handle)
                .add();

		net.messageBuilder(UpdateStatC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UpdateStatC2S::decode)
                .encoder(UpdateStatC2S::encode)
                .consumerMainThread(UpdateStatC2S::handle)
                .add();

		net.messageBuilder(IncreaseStatC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(IncreaseStatC2S::decode)
                .encoder(IncreaseStatC2S::encode)
                .consumerMainThread(IncreaseStatC2S::handle)
                .add();

		net.messageBuilder(UpdateSkillC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UpdateSkillC2S::new)
                .encoder(UpdateSkillC2S::encode)
                .consumerMainThread(UpdateSkillC2S::handle)
                .add();

        net.messageBuilder(StartQuestC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(StartQuestC2S::new)
                .encoder(StartQuestC2S::encode)
                .consumerMainThread(StartQuestC2S::handle)
                .add();

		net.messageBuilder(ClaimRewardC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(ClaimRewardC2S::new)
				.encoder(ClaimRewardC2S::encode)
				.consumerMainThread(ClaimRewardC2S::handle)
				.add();

		net.messageBuilder(UnlockSagaC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(UnlockSagaC2S::new)
				.encoder(UnlockSagaC2S::encode)
				.consumerMainThread(UnlockSagaC2S::handle)
				.add();

		net.messageBuilder(GrantWishC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(GrantWishC2S::decode)
				.encoder(GrantWishC2S::encode)
				.consumerMainThread(GrantWishC2S::handle)
				.add();

		net.messageBuilder(TravelToPlanetC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(TravelToPlanetC2S::decode)
				.encoder(TravelToPlanetC2S::encode)
				.consumerMainThread(TravelToPlanetC2S::handle)
				.add();

		net.messageBuilder(SwitchActionC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(SwitchActionC2S::new)
				.encoder(SwitchActionC2S::encode)
				.consumerMainThread(SwitchActionC2S::handle)
				.add();

		net.messageBuilder(ExecuteActionC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(ExecuteActionC2S::new)
				.encoder(ExecuteActionC2S::encode)
				.consumerMainThread(ExecuteActionC2S::handle)
				.add();

		net.messageBuilder(UpdateCustomHairC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(UpdateCustomHairC2S::decode)
				.encoder(UpdateCustomHairC2S::encode)
				.consumerMainThread(UpdateCustomHairC2S::handle)
				.add();

		net.messageBuilder(StatsSyncC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(StatsSyncC2S::decode)
				.encoder(StatsSyncC2S::encode)
				.consumerMainThread(StatsSyncC2S::handle)
				.add();
		/*
		  SERVER -> CLIENT
		 */
		net.messageBuilder(StatsSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(StatsSyncS2C::decode)
                .encoder(StatsSyncS2C::encode)
                .consumerMainThread(StatsSyncS2C::handle)
                .add();

		net.messageBuilder(PlayerAnimationsSync.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayerAnimationsSync::new)
                .encoder(PlayerAnimationsSync::encode)
                .consumerMainThread(PlayerAnimationsSync::handle)
                .add();

		net.messageBuilder(SyncServerConfigS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncServerConfigS2C::new)
                .encoder(SyncServerConfigS2C::encode)
                .consumerMainThread(SyncServerConfigS2C::handle)
                .add();

		net.messageBuilder(SyncSagasS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(SyncSagasS2C::new)
				.encoder(SyncSagasS2C::encode)
				.consumerMainThread(SyncSagasS2C::handle)
				.add();

		net.messageBuilder(SyncWishesS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(SyncWishesS2C::new)
				.encoder(SyncWishesS2C::encode)
				.consumerMainThread(SyncWishesS2C::handle)
				.add();

		net.messageBuilder(RadarSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(RadarSyncS2C::decode)
				.encoder(RadarSyncS2C::encode)
				.consumerMainThread(RadarSyncS2C::handle)
				.add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAllPlayers(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <MSG> void sendToTrackingEntityAndSelf(MSG message, Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

}


