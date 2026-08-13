package com.dragonminez.common.network;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.events.RadarRenderEvent;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.CombatFlightHandler;
import com.dragonminez.client.gui.InstantTransmissionScreen;
import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.client.gui.quest.QuestNPCDialogueScreen;
import com.dragonminez.client.gui.quest.StoryNotificationManager;
import com.dragonminez.client.gui.quest.StoryToast;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.client.gui.character.SkillsMenuScreen;
import com.dragonminez.client.render.shader.ClientGravityState;
import com.dragonminez.client.systems.impactframes.ImpactFrame;
import com.dragonminez.client.systems.impactframes.ImpactFramesHandler;
import com.dragonminez.client.systems.taiyoken.TaiyokenBlindState;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.S2C.BeamClashStateS2C;
import com.dragonminez.common.network.S2C.TechniqueImportResultS2C;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Status;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

	public static void handleOpenITMenu(List<com.dragonminez.common.network.ITTargetEntry> entries) {
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			var skill = data.getSkills().getSkill("instant_transmission");
			if (skill == null || skill.getLevel() < 5) return;
			Minecraft.getInstance().setScreen(new InstantTransmissionScreen(entries, skill.getLevel()));
		});
	}

	public static void handleStatsSyncPacket(int playerId, CompoundTag nbt) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;

		var entity = clientLevel.getEntity(playerId);
		if (entity instanceof Player player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				try {
					data.load(nbt);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
				player.refreshDimensions();
				player.refreshDisplayName();
			});
		}
	}

	public static void handleAppearanceSyncPacket(int playerId, CompoundTag nbt) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null || !nbt.contains("Character")) return;

		if (clientLevel.getEntity(playerId) instanceof Player player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getCharacter().load(nbt.getCompound("Character"));
				player.refreshDimensions();
				player.refreshDisplayName();
			});
		}
	}

	/**
	 * Applies the deliberately partial payload sent after progression changes.
	 * Feeding this tag through {@link com.dragonminez.common.stats.StatsData#load}
	 * also resets sections which are absent from the packet, and can make a
	 * newly-created technique appear to have been lost on the client.
	 */
	public static void handleProgressionSyncPacket(int playerId, CompoundTag nbt) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;

		if (clientLevel.getEntity(playerId) instanceof Player player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (nbt.contains("Stats")) data.getStats().load(nbt.getCompound("Stats"));
				if (nbt.contains("BonusStats")) data.getBonusStats().load(nbt.getCompound("BonusStats"));
				if (nbt.contains("Resources")) data.getResources().load(nbt.getCompound("Resources"));
				if (nbt.contains("Skills")) data.getSkills().load(nbt.getCompound("Skills"));
				if (nbt.contains("Techniques")) data.getTechniques().load(nbt.getCompound("Techniques"));
				if (nbt.contains("PlayerQuestData")) {
					data.getPlayerQuestData().deserializeNBT(nbt.getCompound("PlayerQuestData"));
				}
				player.refreshDimensions();
				player.refreshDisplayName();
			});
		}
	}

	public static void handleTechniqueChargeSync(int playerId, float percent, boolean charging) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;
		if (clientLevel.getEntity(playerId) instanceof Player player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getTechniques().setTechniqueChargePercent(percent);
				data.getTechniques().setTechniqueCharging(charging);
			});
		}
	}

	public static void handleBeamClashState(BeamClashStateS2C msg) {
		if (msg.isActive()) {
			ClientBeamClashState.update(true, msg.getMeterPhase(), msg.getSweetLow(),
					msg.getSweetHigh(), msg.getAdvantage(), msg.getBeamColor(), msg.getOpponentEntityId());
			com.dragonminez.client.clash.BeamClashCinematicCamera.activate();
		} else {
			ClientBeamClashState.clear();
			com.dragonminez.client.clash.BeamClashCinematicCamera.deactivate();
		}
	}

	public static void handleOpenRecustomizePacket() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
				mc.setScreen(new CharacterCustomizationScreen(null, data.getCharacter()));
			});
		}
	}

	public static void handleOpenQuestNpcDialoguePacket(String npcId, List<String> offerableQuestIds,
			List<String> turnInQuestIds, List<String> inProgressQuestIds, boolean masterNpc, int entityId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.setScreen(new QuestNPCDialogueScreen(npcId, offerableQuestIds, turnInQuestIds, inProgressQuestIds,
					masterNpc, entityId));
		}
	}

	public static void handleStoryToastPacket(StoryToastS2C message) {
		StoryNotificationManager.push(message);
	}

	public static void handlePartyInviteToastPacket(String inviterName) {
		Minecraft mc = Minecraft.getInstance();
		mc.getToasts().addToast(new StoryToast(
				Component.translatable("toast.dragonminez.party.invite.title"),
				Component.translatable("toast.dragonminez.party.invite.desc", Component.literal(inviterName)),
				StoryToast.Tone.INFO
		));
	}

	public static void handleQuestActionFeedback(Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		mc.getToasts().addToast(new StoryToast(
				Component.translatable("message.dragonminez.quest.start.failed_title"),
				message,
				StoryToast.Tone.FAILURE
		));
		mc.player.sendSystemMessage(message);
	}

	public static void handlePlayerAnimationsSyncPacket(UUID playerUUID, boolean isFlying) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;

		Player player = clientLevel.getPlayerByUUID(playerUUID);
		if (player instanceof AbstractClientPlayer clientPlayer && clientPlayer instanceof IPlayerAnimatable animatable) {
			animatable.dragonminez$setFlying(isFlying);
		}
	}

	public static void handleRadarSyncPacket(List<BlockPos> earthPositions, List<BlockPos> namekPositions, Map<String, List<BlockPos>> positionsBySet) {
		RadarRenderEvent.updateRadarData(earthPositions, namekPositions, positionsBySet);
	}

	public static void handleTriggerAnimationPacket(UUID playerUUID, TriggerAnimationS2C.AnimationType animationType,
			int variant, int entityId, String stringPayload) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;

		Player player = clientLevel.getPlayerByUUID(playerUUID);
		if (player instanceof AbstractClientPlayer clientPlayer && clientPlayer instanceof IPlayerAnimatable animatable) {
			switch (animationType) {
				case EVASION -> animatable.dragonminez$triggerEvasion();
				case DASH -> animatable.dragonminez$triggerDash(variant);
				case KI_BLAST_SHOT -> animatable.dragonminez$setShootingKi(variant == 0);
				case KI_ANIMATION -> animatable.dragonminez$playKiAnimation(stringPayload, variant == 1);
				case KI_ANIMATION_STOP -> animatable.dragonminez$stopKiAnimation();
			}
		}
	}

	public static void handleKnockbackFlightPacket(double x, double y, double z) {
		var player = Minecraft.getInstance().player;
		if (player == null) return;

		Vec3 knockback = new Vec3(x, y, z);
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getSkills().isSkillActive("fly")) return;
			if (data.getStatus().getFlightMode() == Status.FLIGHT_COMBAT) {
				CombatFlightHandler.injectKnockback(knockback);
			} else {
				FlySkillEvent.injectKnockback(knockback);
			}
		});
	}

	public static void handleMeleeAnimationPacket(int entityId, String animationName, boolean isOffhand, float speedMultiplier) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;

		Entity entity = clientLevel.getEntity(entityId);
		if (entity instanceof AbstractClientPlayer clientPlayer && clientPlayer instanceof IPlayerAnimatable animatable) {
			animatable.dragonminez$playMeleeAnimation(animationName, isOffhand, speedMultiplier);
		}
	}

	public static void handleGravityZoneSync(float machineGravity, float environmentalGravity, float netGravity,
			float statMult, float tpGravityMult, int idealWeight, int totalWeight, int effectiveWeight,
			float loadRatio, float weightTpMult, int zone) {
		ClientGravityState.update(machineGravity, environmentalGravity, netGravity, statMult, tpGravityMult,
				idealWeight, totalWeight, effectiveWeight, loadRatio, weightTpMult, zone);
	}

	public static void handleTaiyokenBlind(int durationTicks) {
		TaiyokenBlindState.startBlind(durationTicks);
	}

	public static void handleTechniqueImportResult(TechniqueImportResultS2C.Status status, int value) {
		SkillsMenuScreen.handleTechniqueImportResult(status, value);
	}

	public static void handleImpactFrame(float threshold, float lerp, int duration, boolean invert) {
		if (ConfigManager.getUserConfig().isImpactFramesEnabled()) {
			ImpactFramesHandler.addImpactFrame(new ImpactFrame(threshold, lerp, duration, invert));
		}
	}

}

