package com.dragonminez.client.util;

import com.dragonminez.Reference;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VersionCheckerUtil {

	private static boolean popupShown = false;

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (popupShown) return;

		Minecraft mc = Minecraft.getInstance();
		if (!(mc.screen instanceof TitleScreen)) return;
		if (mc.player != null) return;

		var modInfoOpt = ModList.get()
				.getModContainerById(Reference.MOD_ID)
				.map(ModContainer::getModInfo);

		if (modInfoOpt.isEmpty()) {
			popupShown = true;
			return;
		}

		var modInfo = modInfoOpt.get();
		var result = VersionChecker.getResult(modInfo);
		var status = result.status();

		if (status == VersionChecker.Status.PENDING) return;

		popupShown = true;

		if (status == VersionChecker.Status.OUTDATED || status == VersionChecker.Status.BETA_OUTDATED) {
			String currentVersion = modInfo.getVersion().toString();
			String targetVersion = result.target() == null ? "unknown" : result.target().toString();
			String updateUrl = result.url() == null
					? "https://www.curseforge.com/minecraft/mc-mods/dragonminez/files"
					: result.url();

			Component title = Component.literal("DragonMineZ update available");
			Component body = Component.literal(
					"Installed: " + currentVersion + "\n" +
							"Latest: " + targetVersion + "\n\n" +
							"Open the download page?"
			);

			mc.setScreen(new ConfirmScreen(
					open -> {
						if (open) {
							Util.getPlatform().openUri(updateUrl);
						}
						mc.setScreen(new TitleScreen());
					},
					title,
					body,
					Component.literal("Open page"),
					CommonComponents.GUI_CANCEL
			));
		}
	}
}
