package com.yuseix.dragonminez.network;

import com.yuseix.dragonminez.client.gui.AttributesMenu;
import com.yuseix.dragonminez.client.gui.cc.CFirstPage;
import com.yuseix.dragonminez.client.hud.spaceship.SaiyanSpacePodOverlay;
import com.yuseix.dragonminez.network.C2S.InvocarAuraC2S;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class ClientPacketHandler {

	@OnlyIn(Dist.CLIENT)
	public static void handlePermanentEffectsPacket(Map<String, Boolean> effects, Supplier<NetworkEvent.Context> ctxSupplier) {
		Player player = Minecraft.getInstance().player;
		if (player != null) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
				cap.getDMZPermanentEffects().clear(); // Limpia los datos existentes
				cap.getDMZPermanentEffects().putAll(effects); // Añade los nuevos valores
			});
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void handleSkillsPacket(Map<String, Integer> skillsdmz, Supplier<NetworkEvent.Context> ctxSupplier) {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
				cap.getDMZSkills().clear();
				cap.getDMZSkills().putAll(skillsdmz);
			});
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void handleTempEffectsPacket(Map<String, Integer> tempEffects, Supplier<NetworkEvent.Context> ctxSupplier) {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
				cap.getDMZTemporalEffects().clear();
				cap.getDMZTemporalEffects().putAll(tempEffects);
			});
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void handleMenuPacket(boolean openCharacterMenu, Supplier<NetworkEvent.Context> ctxSupplier) {
		if (openCharacterMenu) {
			Minecraft.getInstance().setScreen(
					new AttributesMenu(Component.translatable("menu.title.dragonminez.menuzmzmzm"))
			);
		} else {
			Minecraft.getInstance().setScreen(new CFirstPage());
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void handleStatsSyncPacket(int playerId, CompoundTag nbt, Supplier<NetworkEvent.Context> ctxSupplier) {
		var clientLevel = Minecraft.getInstance().level;
		if (clientLevel == null) return;

		var entity = clientLevel.getEntity(playerId);
		if (entity instanceof Player player) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> {
				cap.loadNBTData(nbt);
				player.refreshDimensions();
			});
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void handleZPointsPacket(int zPoints, Supplier<NetworkEvent.Context> ctxSupplier) {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(cap -> cap.setZpoints(zPoints));
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void handleInvocarAuraPacket(UUID playerId, float transparency, Supplier<NetworkEvent.Context> ctxSupplier) {
		var aura = InvocarAuraC2S.playerAuraMap.get(playerId);
		if (aura != null) {
			aura.setTransparencia(transparency);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void handleKaioPlanetUnlockPacket(boolean isUnlocked, Supplier<NetworkEvent.Context> ctxSupplier) {
		SaiyanSpacePodOverlay.setKaioAvailable(isUnlocked);
	}

	@OnlyIn(Dist.CLIENT)
	public static void handleUpdatePlanetSelectionPacket(int selectedPlanet, Supplier<NetworkEvent.Context> ctxSupplier) {
		SaiyanSpacePodOverlay.updatePlanetTarget(selectedPlanet);
	}

}