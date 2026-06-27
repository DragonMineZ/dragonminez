package com.dragonminez.client.util;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.gui.quest.StoryToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.concurrent.CompletableFuture;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VersionCheckerUtil {

	private static final int MAX_PENDING_POLLS = 40;
	private static final long PENDING_POLL_DELAY_MS = 250L;

	private static volatile boolean checkStarted = false;
	private static volatile boolean updateToastShown = false;

	@SubscribeEvent
	public static void onTitleScreenInit(ScreenEvent.Init.Post event) {
		if (checkStarted || updateToastShown) return;
		if (!(event.getScreen() instanceof TitleScreen)) return;
		startCheck();
	}

	private static synchronized void startCheck() {
		if (checkStarted) return;
		checkStarted = true;

		var modInfoOpt = ModList.get()
				.getModContainerById(Reference.MOD_ID)
				.map(ModContainer::getModInfo);

		if (modInfoOpt.isEmpty()) {
			LogUtil.warn(Env.CLIENT, "[DMZ-VERSION] Could not locate mod metadata for update check.");
			return;
		}

		IModInfo modInfo = modInfoOpt.get();
		String currentVersion = modInfo.getVersion().toString();
		if (isPreReleaseBuild(currentVersion)) {
			LogUtil.info(Env.CLIENT, "[DMZ-VERSION] Skipping update notification for pre-release build: {}", currentVersion);
			return;
		}

		CompletableFuture
				.supplyAsync(() -> awaitForgeResult(modInfo))
				.thenAccept(result -> Minecraft.getInstance().execute(() -> showOutdatedToastIfNeeded(currentVersion, result)))
				.exceptionally(exception -> {
					LogUtil.warn(Env.CLIENT, "[DMZ-VERSION] Update check failed: {}", exception.getMessage());
					return null;
				});
	}

	private static VersionChecker.CheckResult awaitForgeResult(IModInfo modInfo) {
		VersionChecker.CheckResult result = VersionChecker.getResult(modInfo);
		for (int attempt = 0; attempt < MAX_PENDING_POLLS && result.status() == VersionChecker.Status.PENDING; attempt++) {
			try {
				// Ignore warning in async environment
				Thread.sleep(PENDING_POLL_DELAY_MS);
			} catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				break;
			}
			result = VersionChecker.getResult(modInfo);
		}
		return result;
	}

	private static void showOutdatedToastIfNeeded(String currentVersion, VersionChecker.CheckResult result) {
		if (updateToastShown) return;
		if (result == null) return;

		VersionChecker.Status status = result.status();
		if (status == VersionChecker.Status.PENDING) {
			LogUtil.warn(Env.CLIENT, "[DMZ-VERSION] Update check is still pending after timeout.");
			return;
		}
		if (status == VersionChecker.Status.FAILED) {
			LogUtil.warn(Env.CLIENT, "[DMZ-VERSION] Forge update check failed to retrieve metadata.");
			return;
		}
		if (status != VersionChecker.Status.OUTDATED) return;

		String targetVersion = result.target() == null ? "unknown" : result.target().toString();
		LogUtil.info(Env.CLIENT, "[DMZ-VERSION] Update available: {} -> {}", currentVersion, targetVersion);
		Minecraft.getInstance().getToasts().addToast(new StoryToast(
				Component.translatable("toast.dragonminez.update.title"),
				Component.translatable("toast.dragonminez.update.desc", currentVersion, targetVersion),
				StoryToast.Tone.INFO
		));
		updateToastShown = true;
	}

	private static boolean isPreReleaseBuild(String currentVersion) {
		String normalized = currentVersion.toLowerCase(Locale.ROOT);
		return normalized.contains("alpha") || normalized.contains("beta");
	}
}
