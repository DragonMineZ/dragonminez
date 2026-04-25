package com.dragonminez.common.network;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.events.RadarRenderEvent;
import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.client.gui.quest.QuestNPCDialogueScreen;
import com.dragonminez.client.gui.quest.StoryNotificationManager;
import com.dragonminez.client.gui.quest.StoryToast;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

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
			});
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

	public static void handlePlayerAnimationsSyncPacket(UUID playerUUID, boolean isFlying) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;

		Player player = clientLevel.getPlayerByUUID(playerUUID);
		if (player instanceof AbstractClientPlayer clientPlayer && clientPlayer instanceof IPlayerAnimatable animatable) {
			animatable.dragonminez$setFlying(isFlying);
		}
	}

	public static void handleRadarSyncPacket(List<BlockPos> earthPositions, List<BlockPos> namekPositions) {
		RadarRenderEvent.updateRadarData(earthPositions, namekPositions);
	}

	public static void handleRadarSyncPacket(List<BlockPos> earthPositions, List<BlockPos> namekPositions, Map<String, List<BlockPos>> positionsBySet) {
		RadarRenderEvent.updateRadarData(earthPositions, namekPositions, positionsBySet);
	}

	public static void handleTriggerAnimationPacket(UUID playerUUID, TriggerAnimationS2C.AnimationType animationType,
			int variant, int entityId) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;

		Player player = clientLevel.getPlayerByUUID(playerUUID);
		if (player instanceof AbstractClientPlayer clientPlayer && clientPlayer instanceof IPlayerAnimatable animatable) {
			switch (animationType) {
				case EVASION -> animatable.dragonminez$triggerEvasion();
				case DASH -> animatable.dragonminez$triggerDash(variant);
				case KI_BLAST_SHOT -> animatable.dragonminez$setShootingKi(variant == 0);
			}
		}
	}

	public static void handleMeleeAnimationPacket(int entityId, String animationName, boolean isOffhand, float speedMultiplier) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;

		Entity entity = clientLevel.getEntity(entityId);
		if (entity instanceof AbstractClientPlayer clientPlayer && clientPlayer instanceof IPlayerAnimatable animatable) {
			animatable.dragonminez$playMeleeAnimation(animationName, isOffhand, speedMultiplier);
		}
	}

}

