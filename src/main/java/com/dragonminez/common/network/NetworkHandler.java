package com.dragonminez.common.network;

import com.dragonminez.Reference;
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
		net.messageBuilder(com.dragonminez.common.network.C2S.CreateCharacterC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(com.dragonminez.common.network.C2S.CreateCharacterC2S::decode)
                .encoder(com.dragonminez.common.network.C2S.CreateCharacterC2S::encode)
                .consumerMainThread(com.dragonminez.common.network.C2S.CreateCharacterC2S::handle)
                .add();

		/*
		  SERVER -> CLIENT
		 */
		net.messageBuilder(com.dragonminez.common.network.S2C.StatsSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(com.dragonminez.common.network.S2C.StatsSyncS2C::decode)
                .encoder(com.dragonminez.common.network.S2C.StatsSyncS2C::encode)
                .consumerMainThread(com.dragonminez.common.network.S2C.StatsSyncS2C::handle)
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


