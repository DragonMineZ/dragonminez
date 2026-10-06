package com.dragonminez.common.network;

import com.dragonminez.client.animation.EvasionAnimations;
import com.dragonminez.client.animation.KiAnimations;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.events.RadarRenderEvent;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.flight.CombatFlightHandler;
import com.dragonminez.client.gui.InstantTransmissionScreen;
import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.client.gui.dialogue.NpcDialogueScreen;
import com.dragonminez.client.gui.quest.StoryNotificationManager;
import com.dragonminez.client.gui.hud.NotificationHUD;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.client.render.effects.AuraModeState;
import com.dragonminez.client.systems.DragonSkyState;
import com.dragonminez.common.network.S2C.BeamClashStateS2C;
import com.dragonminez.common.network.S2C.DialogueResultS2C;
import com.dragonminez.common.network.S2C.OpenDialogueNodeS2C;
import com.dragonminez.common.network.S2C.OpenQuestNPCDialogueS2C;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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

	public static void handleMinigameStart(com.dragonminez.common.network.S2C.MinigameStartS2C message) {
		if (Minecraft.getInstance().screen instanceof com.dragonminez.client.gui.character.minigames.BaseMinigameScreen screen
				&& screen.acceptsStart(message)) {
			screen.onServerStart(message);
			return;
		}
		if (message.isAccepted()) {
			com.dragonminez.common.network.NetworkHandler.sendToServer(new com.dragonminez.common.network.C2S.MinigameInputC2S(
					message.getSessionId(), 0, 0, true, 0L, List.of()));
		}
	}

	public static void handleOpenMinigame(String minigameId) {
		Minecraft.getInstance().setScreen(com.dragonminez.client.gui.character.minigames.BaseMinigameScreen.create(minigameId,
				com.dragonminez.common.training.MinigameOrigin.COMMAND));
	}

	public static void handleMinigameResult(com.dragonminez.common.network.S2C.MinigameResultS2C message) {
		if (Minecraft.getInstance().screen instanceof com.dragonminez.client.gui.character.minigames.BaseMinigameScreen screen) {
			screen.onServerResult(message);
		}
	}

	public static void handleReviveTargets(List<com.dragonminez.common.network.S2C.ReviveTargetsS2C.Entry> entries) {
		if (Minecraft.getInstance().screen instanceof com.dragonminez.client.gui.WishesScreen screen) {
			screen.setReviveTargets(entries);
		}
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
			AuraModeState.reconcileLocal(player);
		}
	}

	public static void handleSkinPixelsSync(int playerId, CompoundTag tag) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;
		if (clientLevel.getEntity(playerId) instanceof Player player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> data.getCharacter().getSkinPixels().load(tag));
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
			ClientBeamClashState.update(msg.getStartGameTime(), msg.getMeterSeed(), msg.getAdvantage(),
					msg.getSelfColor(), msg.getFoeColor(), msg.getOpponentEntityId(), msg.clashPoint());
			com.dragonminez.client.clash.BeamClashCinematicCamera.activate();
		} else {
			ClientBeamClashState.clear(msg.getExhaustTicks());
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

	public static void handleOpenQuestNpcDialoguePacket(OpenQuestNPCDialogueS2C msg) {
		NpcDialogueScreen.open(msg);
	}

	public static void handleDialogueNodePacket(OpenDialogueNodeS2C msg) {
		NpcDialogueScreen.handleNode(msg);
	}

	public static void handleDialogueResult(DialogueResultS2C msg) {
		NpcDialogueScreen.handleResult(msg);
	}

	public static void handleStoryToastPacket(StoryToastS2C message) {
		StoryNotificationManager.push(message);
	}

	public static void handlePartyInviteToastPacket(String inviterName) {
		Minecraft mc = Minecraft.getInstance();
		NotificationHUD.push(
				Component.translatable("toast.dragonminez.party.invite.title"),
				Component.literal(inviterName),
				Component.translatable("toast.dragonminez.party.invite.hint"),
				null,
				NotificationHUD.PRIORITY_NORMAL
		);
		if (mc.player != null) {
			mc.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F, 1.2F);
		}
	}

	public static void handleQuestActionFeedback(Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		NotificationHUD.push(
				Component.translatable("message.dragonminez.quest.start.failed_title"),
				null,
				message,
				null,
				NotificationHUD.PRIORITY_HIGH
		);
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

	public static void handleDragonSky(ResourceLocation dimension, boolean active) {
		DragonSkyState.update(dimension, active);
	}

	public static void handleTriggerAnimationPacket(UUID playerUUID, TriggerAnimationS2C.AnimationType animationType,
			int variant, int entityId, String stringPayload) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;

		Player player = clientLevel.getPlayerByUUID(playerUUID);
		if (player instanceof AbstractClientPlayer clientPlayer && clientPlayer instanceof IPlayerAnimatable animatable) {
			switch (animationType) {
				case EVASION -> animatable.dragonminez$triggerEvasion(variant);
				case DASH -> animatable.dragonminez$triggerDash(variant);
				case KI_BLAST_SHOT -> animatable.dragonminez$setShootingKi(variant == 0);
				case KI_ANIMATION -> {
					String anim = stringPayload.startsWith("evs.") ? EvasionAnimations.resolve(stringPayload) : KiAnimations.resolve(stringPayload);
					animatable.dragonminez$playKiAnimation(anim, variant == 1);
				}
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

	public static void handleRaidMusic(String soundId) {
		com.dragonminez.client.systems.raid.RaidMusicManager.play(soundId);
	}

}

