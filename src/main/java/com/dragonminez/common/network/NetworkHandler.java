package com.dragonminez.common.network;

import com.dragonminez.Reference;
import com.dragonminez.common.network.C2S.*;
import com.dragonminez.common.network.S2C.PlayerAnimationsSync;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.SyncServerConfigS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
                .named(new ResourceLocation(Reference.MOD_ID, "network"))
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

		net.messageBuilder(UpgradeSkillC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UpgradeSkillC2S::new)
                .encoder(UpgradeSkillC2S::encode)
                .consumerMainThread(UpgradeSkillC2S::handle)
                .add();
        net.messageBuilder(StartQuestC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(StartQuestC2S::new)
                .encoder(StartQuestC2S::toBytes)
                .consumerMainThread(StartQuestC2S::handle)
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
}


