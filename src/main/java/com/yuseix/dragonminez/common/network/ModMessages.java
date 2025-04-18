package com.yuseix.dragonminez.common.network;

import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.network.C2S.*;
import com.yuseix.dragonminez.common.network.S2C.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {

	public static SimpleChannel INSTANCE;
	private static int packetID = 0;

	private static int id() {
		return packetID++;
	}

	public static void register() {
		SimpleChannel net = NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation(Reference.MOD_ID, "messagesold"))
				.networkProtocolVersion(() -> "1.0")
				.clientAcceptedVersions(s -> true)
				.serverAcceptedVersions(s -> true)
				.simpleChannel();

		INSTANCE = net;


		//ENVIAR DATOS AL SERVIDOR
		net.messageBuilder(StatsC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(StatsC2S::new)
				.encoder(StatsC2S::toBytes)
				.consumerMainThread(StatsC2S::handle)
				.add();
		net.messageBuilder(CharacterC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(CharacterC2S::new)
				.encoder(CharacterC2S::toBytes)
				.consumerMainThread(CharacterC2S::handle)
				.add();
		net.messageBuilder(MenuC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(MenuC2S::new)
				.encoder(MenuC2S::toBytes)
				.consumerMainThread(MenuC2S::handle)
				.add();
		net.messageBuilder(KarinC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(KarinC2S::new)
				.encoder(KarinC2S::toBytes)
				.consumerMainThread(KarinC2S::handle)
				.add();
		net.messageBuilder(DendeC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(DendeC2S::new)
				.encoder(DendeC2S::toBytes)
				.consumerMainThread(DendeC2S::handle)
				.add();
		net.messageBuilder(ShenlongC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(ShenlongC2S::new)
				.encoder(ShenlongC2S::toBytes)
				.consumerMainThread(ShenlongC2S::handle)
				.add();
		net.messageBuilder(PorungaC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(PorungaC2S::new)
				.encoder(PorungaC2S::toBytes)
				.consumerMainThread(PorungaC2S::handle)
				.add();
		net.messageBuilder(SpacePodC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(SpacePodC2S::decode)
				.encoder(SpacePodC2S::encode)
				.consumerMainThread(SpacePodC2S::handle)
				.add();
		net.messageBuilder(UtilityPanelC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(UtilityPanelC2S::new)
				.encoder(UtilityPanelC2S::toBytes)
				.consumerMainThread(UtilityPanelC2S::handle)
				.add();
		net.messageBuilder(PermaEffC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(PermaEffC2S::new)
				.encoder(PermaEffC2S::toBytes)
				.consumerMainThread(PermaEffC2S::handle)
				.add();
		net.messageBuilder(SkillActivateC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(SkillActivateC2S::new)
				.encoder(SkillActivateC2S::toBytes)
				.consumerMainThread(SkillActivateC2S::handle)
				.add();
		net.messageBuilder(FlyToggleC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(FlyToggleC2S::decode)
				.encoder(FlyToggleC2S::encode)
				.consumerMainThread(FlyToggleC2S::handle)
				.add();
		net.messageBuilder(DragonRadarC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(DragonRadarC2S::decode)
				.encoder(DragonRadarC2S::encode)
				.consumerMainThread(DragonRadarC2S::handle)
				.add();
		net.messageBuilder(FormSkillsC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(FormSkillsC2S::new)
				.encoder(FormSkillsC2S::toBytes)
				.consumerMainThread(FormSkillsC2S::handle)
				.add();
		net.messageBuilder(DescendFormC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(DescendFormC2S::new)
				.encoder(DescendFormC2S::toBytes)
				.consumerMainThread(DescendFormC2S::handle)
				.add();
		net.messageBuilder(MasterSkillsC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(MasterSkillsC2S::new)
				.encoder(MasterSkillsC2S::toBytes)
				.consumerMainThread(MasterSkillsC2S::handle)
				.add();
		net.messageBuilder(GuruC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(GuruC2S::new)
				.encoder(GuruC2S::toBytes)
				.consumerMainThread(GuruC2S::handle)
				.add();
		net.messageBuilder(SuperFormsC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(SuperFormsC2S::new)
				.encoder(SuperFormsC2S::toBytes)
				.consumerMainThread(SuperFormsC2S::handle)
				.add();
		net.messageBuilder(OtroMundoC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(OtroMundoC2S::new)
				.encoder(OtroMundoC2S::toBytes)
				.consumerMainThread(OtroMundoC2S::handle)
				.add();
		net.messageBuilder(SummonQuestC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(SummonQuestC2S::new)
				.encoder(SummonQuestC2S::encode)
				.consumerMainThread(SummonQuestC2S::handle)
				.add();
		net.messageBuilder(ConfigValuesC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(ConfigValuesC2S::new)
				.encoder(ConfigValuesC2S::toBytes)
				.consumerMainThread(ConfigValuesC2S::handle)
				.add();

		//ENVIAR DATOS AL CLIENTE
		net.messageBuilder(ZPointsS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(ZPointsS2C::new)
				.encoder(ZPointsS2C::toBytes)
				.consumerMainThread(ZPointsS2C::handle)
				.add();
		net.messageBuilder(StatsSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(StatsSyncS2C::new)
				.encoder(StatsSyncS2C::toBytes)
				.consumerMainThread(StatsSyncS2C::handle)
				.add();
		net.messageBuilder(MenuS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(MenuS2C::new)
				.encoder(MenuS2C::toBytes)
				.consumerMainThread(MenuS2C::handle)
				.add();
		net.messageBuilder(DMZTempEffectsS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(DMZTempEffectsS2C::toBytes)
				.decoder(DMZTempEffectsS2C::new)
				.consumerMainThread(DMZTempEffectsS2C::handle)
				.add();
		net.messageBuilder(DMZSkillsS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(DMZSkillsS2C::toBytes)
				.decoder(DMZSkillsS2C::new)
				.consumerMainThread(DMZSkillsS2C::handle)
				.add();
		net.messageBuilder(DMZFormsS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(DMZFormsS2C::toBytes)
				.decoder(DMZFormsS2C::new)
				.consumerMainThread(DMZFormsS2C::handle)
				.add();
		net.messageBuilder(DMZPermanentEffectsSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(DMZPermanentEffectsSyncS2C::toBytes)
				.decoder(DMZPermanentEffectsSyncS2C::new)
				.consumerMainThread(DMZPermanentEffectsSyncS2C::handle)
				.add();
		net.messageBuilder(DMZCompletedQuestsSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(DMZCompletedQuestsSyncS2C::toBytes)
				.decoder(DMZCompletedQuestsSyncS2C::new)
				.consumerMainThread(DMZCompletedQuestsSyncS2C::handle)
				.add();
		net.messageBuilder(UpdateDragonRadarS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(UpdateDragonRadarS2C::encode)
				.decoder(UpdateDragonRadarS2C::decode)
				.consumerMainThread(UpdateDragonRadarS2C::handle)
				.add();
		net.messageBuilder(UpdateNamekDragonRadarS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(UpdateNamekDragonRadarS2C::encode)
				.decoder(UpdateNamekDragonRadarS2C::decode)
				.consumerMainThread(UpdateNamekDragonRadarS2C::handle)
				.add();
		net.messageBuilder(FlyToggleS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(FlyToggleS2C::encode)
				.decoder(FlyToggleS2C::decode)
				.consumerMainThread(FlyToggleS2C::handle)
				.add();
		net.messageBuilder(SyncDragonBallsS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(SyncDragonBallsS2C::encode)
				.decoder(SyncDragonBallsS2C::decode)
				.consumerMainThread(SyncDragonBallsS2C::handle)
				.add();
		net.messageBuilder(StorySyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(StorySyncS2C::toBytes)
				.decoder(StorySyncS2C::new)
				.consumerMainThread(StorySyncS2C::handle)
				.add();
		net.messageBuilder(PacketSyncConfig.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.encoder(PacketSyncConfig::toBytes)
				.decoder(PacketSyncConfig::new)
				.consumerMainThread(PacketSyncConfig::handle)
				.add();
	}

	//Manda un paquete al Servidor (message no es un mensaje, es un paquete)
	public static <MSG> void sendToServer(MSG message) {
		INSTANCE.sendToServer(message);
	}

	//Manda un paquete a un jugador (message no es un mensaje, es un paquete)
	public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
		INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
	}

	public static <MSG> void sendToClients(MSG message) {
		INSTANCE.send(PacketDistributor.ALL.noArg(), message);
	}

	public static <MSG> void sendToAll(Player player, MSG message) {
		INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
	}
}