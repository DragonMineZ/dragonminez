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

		net.messageBuilder(QuestActionC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(QuestActionC2S::new)
				.encoder(QuestActionC2S::encode)
				.consumerMainThread(QuestActionC2S::handle)
				.add();

		net.messageBuilder(ClaimQuestRewardC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(ClaimQuestRewardC2S::new)
				.encoder(ClaimQuestRewardC2S::encode)
				.consumerMainThread(ClaimQuestRewardC2S::handle)
				.add();

		net.messageBuilder(ClaimAllQuestRewardsC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(ClaimAllQuestRewardsC2S::new)
				.encoder(ClaimAllQuestRewardsC2S::encode)
				.consumerMainThread(ClaimAllQuestRewardsC2S::handle)
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

		net.messageBuilder(FlyToggleC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(FlyToggleC2S::decode)
				.encoder(FlyToggleC2S::encode)
				.consumerMainThread(FlyToggleC2S::handle)
				.add();

		net.messageBuilder(FlightModeC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(FlightModeC2S::decode)
				.encoder(FlightModeC2S::encode)
				.consumerMainThread(FlightModeC2S::handle)
				.add();

		net.messageBuilder(CombatFlyImpulseC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(CombatFlyImpulseC2S::decode)
				.encoder(CombatFlyImpulseC2S::encode)
				.consumerMainThread(CombatFlyImpulseC2S::handle)
				.add();

		net.messageBuilder(DashC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(DashC2S::new)
				.encoder(DashC2S::encode)
				.consumerMainThread(DashC2S::handle)
				.add();

		net.messageBuilder(KiBlastC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(KiBlastC2S::new)
				.encoder(KiBlastC2S::encode)
				.consumerMainThread(KiBlastC2S::handle)
				.add();

		net.messageBuilder(NPCActionC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(NPCActionC2S::new)
				.encoder(NPCActionC2S::toBytes)
				.consumerMainThread(NPCActionC2S::handle)
				.add();

		net.messageBuilder(TrainingRewardC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(TrainingRewardC2S::new)
				.encoder(TrainingRewardC2S::toBytes)
				.consumerMainThread(TrainingRewardC2S::handle)
				.add();

		net.messageBuilder(TrainingAnimationC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(TrainingAnimationC2S::new)
				.encoder(TrainingAnimationC2S::toBytes)
				.consumerMainThread(TrainingAnimationC2S::handle)
				.add();

		net.messageBuilder(SummonPlayerShadowDummyC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(SummonPlayerShadowDummyC2S::new)
				.encoder(SummonPlayerShadowDummyC2S::toBytes)
				.consumerMainThread(SummonPlayerShadowDummyC2S::handle)
				.add();

		net.messageBuilder(UpdateCharacterC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(UpdateCharacterC2S::decode)
				.encoder(UpdateCharacterC2S::encode)
				.consumerMainThread(UpdateCharacterC2S::handle)
				.add();

		net.messageBuilder(SetTrackedQuestC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(SetTrackedQuestC2S::new)
				.encoder(SetTrackedQuestC2S::encode)
				.consumerMainThread(SetTrackedQuestC2S::handle)
				.add();

		net.messageBuilder(SetStoryDifficultyC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(SetStoryDifficultyC2S::new)
				.encoder(SetStoryDifficultyC2S::encode)
				.consumerMainThread(SetStoryDifficultyC2S::handle)
				.add();

		net.messageBuilder(CreatePartyC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(CreatePartyC2S::new)
				.encoder(CreatePartyC2S::encode)
				.consumerMainThread(CreatePartyC2S::handle)
				.add();

		net.messageBuilder(InvitePartyMemberC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(InvitePartyMemberC2S::new)
				.encoder(InvitePartyMemberC2S::encode)
				.consumerMainThread(InvitePartyMemberC2S::handle)
				.add();

		net.messageBuilder(AcceptPartyInviteC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(AcceptPartyInviteC2S::new)
				.encoder(AcceptPartyInviteC2S::encode)
				.consumerMainThread(AcceptPartyInviteC2S::handle)
				.add();

		net.messageBuilder(RejectPartyInviteC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(RejectPartyInviteC2S::new)
				.encoder(RejectPartyInviteC2S::encode)
				.consumerMainThread(RejectPartyInviteC2S::handle)
				.add();

		net.messageBuilder(LeavePartyC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(LeavePartyC2S::new)
				.encoder(LeavePartyC2S::encode)
				.consumerMainThread(LeavePartyC2S::handle)
				.add();

		net.messageBuilder(SokidanControlC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(SokidanControlC2S::new)
				.encoder(SokidanControlC2S::toBytes)
				.consumerMainThread(SokidanControlC2S::handle)
				.add();

		net.messageBuilder(EquipTechniqueC2S.class,  id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(EquipTechniqueC2S::new)
				.encoder(EquipTechniqueC2S::toBytes)
				.consumerMainThread(EquipTechniqueC2S::handle)
				.add();

		net.messageBuilder(CreateTechniqueC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(CreateTechniqueC2S::new)
				.encoder(CreateTechniqueC2S::toBytes)
				.consumerMainThread(CreateTechniqueC2S::handle)
				.add();

		net.messageBuilder(UpgradeTechniqueC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(UpgradeTechniqueC2S::new)
				.encoder(UpgradeTechniqueC2S::toBytes)
				.consumerMainThread(UpgradeTechniqueC2S::handle)
				.add();

		net.messageBuilder(ImportTechniqueC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(ImportTechniqueC2S::new)
				.encoder(ImportTechniqueC2S::toBytes)
				.consumerMainThread(ImportTechniqueC2S::handle)
				.add();

		net.messageBuilder(SelectTechniqueSlotC2S.class,  id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(SelectTechniqueSlotC2S::new)
				.encoder(SelectTechniqueSlotC2S::toBytes)
				.consumerMainThread(SelectTechniqueSlotC2S::handle)
				.add();

		net.messageBuilder(TechniqueChargeC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(TechniqueChargeC2S::new)
				.encoder(TechniqueChargeC2S::toBytes)
				.consumerMainThread(TechniqueChargeC2S::handle)
				.add();

		net.messageBuilder(com.dragonminez.common.network.C2S.StrikeAttackC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(com.dragonminez.common.network.C2S.StrikeAttackC2S::new)
				.encoder(com.dragonminez.common.network.C2S.StrikeAttackC2S::toBytes)
				.consumerMainThread(com.dragonminez.common.network.C2S.StrikeAttackC2S::handle)
				.add();

		net.messageBuilder(TaiyokenCastC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(TaiyokenCastC2S::new)
				.encoder(TaiyokenCastC2S::toBytes)
				.consumerMainThread(TaiyokenCastC2S::handle)
				.add();

		net.messageBuilder(CombatAttackRequestC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(CombatAttackRequestC2S::new)
				.encoder(CombatAttackRequestC2S::encode)
				.consumerMainThread(CombatAttackRequestC2S::handle)
				.add();

		net.messageBuilder(DamageCurioC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(DamageCurioC2S::new)
				.encoder(DamageCurioC2S::toBytes)
				.consumerMainThread(DamageCurioC2S::handle)
				.add();

		net.messageBuilder(InstantTransmissionTapC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(InstantTransmissionTapC2S::new)
				.encoder(InstantTransmissionTapC2S::toBytes)
				.consumerMainThread(InstantTransmissionTapC2S::handle)
				.add();

		net.messageBuilder(InstantTransmissionTravelC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(InstantTransmissionTravelC2S::new)
				.encoder(InstantTransmissionTravelC2S::toBytes)
				.consumerMainThread(InstantTransmissionTravelC2S::handle)
				.add();

		net.messageBuilder(InstantTransmissionTravelToPlayerC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(InstantTransmissionTravelToPlayerC2S::new)
				.encoder(InstantTransmissionTravelToPlayerC2S::toBytes)
				.consumerMainThread(InstantTransmissionTravelToPlayerC2S::handle)
				.add();

		net.messageBuilder(RequestITTargetsC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(RequestITTargetsC2S::new)
				.encoder(RequestITTargetsC2S::encode)
				.consumerMainThread(RequestITTargetsC2S::handle)
				.add();

		net.messageBuilder(DeleteMasterC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(DeleteMasterC2S::new)
				.encoder(DeleteMasterC2S::toBytes)
				.consumerMainThread(DeleteMasterC2S::handle)
				.add();

		net.messageBuilder(DeleteTechniqueC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(DeleteTechniqueC2S::new)
				.encoder(DeleteTechniqueC2S::toBytes)
				.consumerMainThread(DeleteTechniqueC2S::handle)
				.add();

		net.messageBuilder(BeamClashInputC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(BeamClashInputC2S::new)
				.encoder(BeamClashInputC2S::toBytes)
				.consumerMainThread(BeamClashInputC2S::handle)
				.add();

		net.messageBuilder(GravityDeviceUpdateC2S.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(GravityDeviceUpdateC2S::new)
				.encoder(GravityDeviceUpdateC2S::encode)
				.consumerMainThread(GravityDeviceUpdateC2S::handle)
				.add();

		/*
		  SERVER -> CLIENT
		 */
		net.messageBuilder(StatsSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(StatsSyncS2C::decode)
				.encoder(StatsSyncS2C::encode)
				.consumerMainThread(StatsSyncS2C::handle)
				.add();

		net.messageBuilder(ResourceSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(ResourceSyncS2C::decode)
				.encoder(ResourceSyncS2C::encode)
				.consumerMainThread(ResourceSyncS2C::handle)
				.add();

		net.messageBuilder(ProgressionSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(ProgressionSyncS2C::decode)
				.encoder(ProgressionSyncS2C::encode)
				.consumerMainThread(ProgressionSyncS2C::handle)
				.add();

		net.messageBuilder(com.dragonminez.common.network.S2C.TechniqueChargeSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(com.dragonminez.common.network.S2C.TechniqueChargeSyncS2C::decode)
				.encoder(com.dragonminez.common.network.S2C.TechniqueChargeSyncS2C::encode)
				.consumerMainThread(com.dragonminez.common.network.S2C.TechniqueChargeSyncS2C::handle)
				.add();

		net.messageBuilder(AppearanceSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(AppearanceSyncS2C::decode)
				.encoder(AppearanceSyncS2C::encode)
				.consumerMainThread(AppearanceSyncS2C::handle)
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

		net.messageBuilder(OpenQuestNPCDialogueS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(OpenQuestNPCDialogueS2C::new)
				.encoder(OpenQuestNPCDialogueS2C::encode)
				.consumerMainThread(OpenQuestNPCDialogueS2C::handle)
				.add();

		net.messageBuilder(SyncWishesS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(SyncWishesS2C::new)
				.encoder(SyncWishesS2C::encode)
				.consumerMainThread(SyncWishesS2C::handle)
				.add();

		net.messageBuilder(SyncQuestRegistryS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(SyncQuestRegistryS2C::new)
				.encoder(SyncQuestRegistryS2C::encode)
				.consumerMainThread(SyncQuestRegistryS2C::handle)
				.add();

		net.messageBuilder(RadarSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(RadarSyncS2C::decode)
				.encoder(RadarSyncS2C::encode)
				.consumerMainThread(RadarSyncS2C::handle)
				.add();

		net.messageBuilder(TriggerAnimationS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(TriggerAnimationS2C::new)
				.encoder(TriggerAnimationS2C::encode)
				.consumerMainThread(TriggerAnimationS2C::handle)
				.add();

		net.messageBuilder(OpenRecustomizeS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(OpenRecustomizeS2C::new)
				.encoder(OpenRecustomizeS2C::encode)
				.consumerMainThread(OpenRecustomizeS2C::handle)
				.add();

		net.messageBuilder(StoryToastS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(StoryToastS2C::decode)
				.encoder(StoryToastS2C::encode)
				.consumerMainThread(StoryToastS2C::handle)
				.add();

		net.messageBuilder(PartyInviteToastS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(PartyInviteToastS2C::new)
				.encoder(PartyInviteToastS2C::encode)
				.consumerMainThread(PartyInviteToastS2C::handle)
				.add();

		net.messageBuilder(QuestActionFeedbackS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(QuestActionFeedbackS2C::new)
				.encoder(QuestActionFeedbackS2C::encode)
				.consumerMainThread(QuestActionFeedbackS2C::handle)
				.add();

		net.messageBuilder(SyncWeaponRegistryS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(SyncWeaponRegistryS2C::new)
				.encoder(SyncWeaponRegistryS2C::encode)
				.consumerMainThread(SyncWeaponRegistryS2C::handle)
				.add();

		net.messageBuilder(MeleeAnimationS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(MeleeAnimationS2C::new)
				.encoder(MeleeAnimationS2C::encode)
				.consumerMainThread(MeleeAnimationS2C::handle)
				.add();

		net.messageBuilder(TriggerImpactFrameS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(TriggerImpactFrameS2C::new)
				.encoder(TriggerImpactFrameS2C::encode)
				.consumerMainThread(TriggerImpactFrameS2C::handle)
				.add();

		net.messageBuilder(TaiyokenBlindS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(TaiyokenBlindS2C::new)
				.encoder(TaiyokenBlindS2C::encode)
				.consumerMainThread(TaiyokenBlindS2C::handle)
				.add();

		net.messageBuilder(SyncSpacePodDestinationsS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(SyncSpacePodDestinationsS2C::new)
				.encoder(SyncSpacePodDestinationsS2C::encode)
				.consumerMainThread(SyncSpacePodDestinationsS2C::handle)
				.add();

		net.messageBuilder(TechniqueImportResultS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(TechniqueImportResultS2C::new)
				.encoder(TechniqueImportResultS2C::encode)
				.consumerMainThread(TechniqueImportResultS2C::handle)
				.add();

		net.messageBuilder(BeamClashStateS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(BeamClashStateS2C::decode)
				.encoder(BeamClashStateS2C::encode)
				.consumerMainThread(BeamClashStateS2C::handle)
				.add();

		net.messageBuilder(OpenITMenuS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(OpenITMenuS2C::new)
				.encoder(OpenITMenuS2C::encode)
				.consumerMainThread(OpenITMenuS2C::handle)
				.add();

		net.messageBuilder(GravityZoneSyncS2C.class, id(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(GravityZoneSyncS2C::new)
				.encoder(GravityZoneSyncS2C::encode)
				.consumerMainThread(GravityZoneSyncS2C::handle)
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

	public static <MSG> void sendToTrackingEntity(MSG message, Entity entity) {
		INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
	}
}
