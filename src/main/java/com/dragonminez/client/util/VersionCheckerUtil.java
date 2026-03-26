package com.dragonminez.client.util;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
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
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VersionCheckerUtil {

	private static final String CURSEFORGE_PROJECT_URL = "https://www.curseforge.com/minecraft/mc-mods/dragonminez";
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	private static final Pattern[] CURSEFORGE_VERSION_PATTERNS = new Pattern[] {
			Pattern.compile("(?i)Main\\s+File.*?v(?<version>\\d+(?:\\.\\d+)+(?:[-a-z0-9.]+)?)\\s+Latest\\s+release", Pattern.DOTALL),
			Pattern.compile("(?i)v(?<version>\\d+(?:\\.\\d+)+(?:[-a-z0-9.]+)?)\\s+Latest\\s+release")
	};

	private static volatile boolean checkStarted = false;
	private static volatile boolean checkCompleted = false;
	private static volatile boolean popupHandled = false;
	private static volatile UpdateInfo pendingUpdate = null;

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (popupHandled) return;

		Minecraft mc = Minecraft.getInstance();
		if (!(mc.screen instanceof TitleScreen)) return;
		if (mc.player != null) return;

		if (!checkStarted) {
			startCheck();
			return;
		}

		if (!checkCompleted) return;

		popupHandled = true;
		if (pendingUpdate == null) return;

		mc.setScreen(new ConfirmScreen(
				open -> {
					if (open) {
						Util.getPlatform().openUri(pendingUpdate.updateUrl());
					}
					mc.setScreen(new TitleScreen());
				},
				Component.translatable("screen.dragonminez.update.title"),
				Component.translatable(
						"screen.dragonminez.update.body",
						pendingUpdate.currentVersion(),
						pendingUpdate.latestVersion()
				),
				Component.translatable("screen.dragonminez.update.open"),
				CommonComponents.GUI_CANCEL
		));
	}

	private static synchronized void startCheck() {
		if (checkStarted) return;
		checkStarted = true;

		var modInfoOpt = ModList.get()
				.getModContainerById(Reference.MOD_ID)
				.map(ModContainer::getModInfo);

		if (modInfoOpt.isEmpty()) {
			checkCompleted = true;
			return;
		}

		IModInfo modInfo = modInfoOpt.get();
		String currentVersion = modInfo.getVersion().toString();

		CompletableFuture
				.supplyAsync(() -> fetchUpdateInfo(modInfo, currentVersion))
				.exceptionally(exception -> {
					LogUtil.warn(Env.CLIENT, "[DMZ-VERSION] Update check failed: {}", exception.getMessage());
					return null;
				})
				.thenAccept(updateInfo -> {
					pendingUpdate = updateInfo;
					checkCompleted = true;
				});
	}

	private static UpdateInfo fetchUpdateInfo(IModInfo modInfo, String currentVersion) {
		UpdateInfo curseForgeInfo = fetchFromCurseForge(currentVersion);
		if (curseForgeInfo != null) {
			return curseForgeInfo;
		}

		return fetchFromForgeVersionChecker(modInfo, currentVersion);
	}

	private static UpdateInfo fetchFromCurseForge(String currentVersion) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(CURSEFORGE_PROJECT_URL))
					.header("User-Agent", "DragonMineZ-VersionChecker/" + currentVersion)
					.GET()
					.build();

			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() != 200) {
				LogUtil.warn(Env.CLIENT, "[DMZ-VERSION] CurseForge update check returned status {}", response.statusCode());
				return null;
			}

			Optional<String> latestVersion = parseLatestVersion(response.body());
			if (latestVersion.isEmpty()) {
				LogUtil.warn(Env.CLIENT, "[DMZ-VERSION] Could not parse latest version from CurseForge page.");
				return null;
			}

			if (!isNewerThanCurrent(latestVersion.get(), currentVersion)) {
				return null;
			}

			LogUtil.info(Env.CLIENT, "[DMZ-VERSION] New CurseForge release detected: {} -> {}", currentVersion, latestVersion.get());
			return new UpdateInfo(currentVersion, latestVersion.get(), CURSEFORGE_PROJECT_URL);
		} catch (Exception exception) {
			LogUtil.warn(Env.CLIENT, "[DMZ-VERSION] CurseForge update check failed: {}", exception.getMessage());
			return null;
		}
	}

	private static UpdateInfo fetchFromForgeVersionChecker(IModInfo modInfo, String currentVersion) {
		try {
			VersionChecker.CheckResult result = null;
			VersionChecker.Status status = VersionChecker.Status.PENDING;
			for (int attempt = 0; attempt < 20 && status == VersionChecker.Status.PENDING; attempt++) {
				result = VersionChecker.getResult(modInfo);
				status = result.status();
				if (status == VersionChecker.Status.PENDING) {
					Thread.sleep(250L);
				}
			}

			if (result == null) {
				return null;
			}

			if (status != VersionChecker.Status.OUTDATED && status != VersionChecker.Status.BETA_OUTDATED) {
				return null;
			}

			String targetVersion = result.target() == null ? "unknown" : result.target().toString();
			String updateUrl = result.url() == null ? CURSEFORGE_PROJECT_URL : result.url();
			LogUtil.info(Env.CLIENT, "[DMZ-VERSION] Forge update metadata reports {} -> {}", currentVersion, targetVersion);
			return new UpdateInfo(currentVersion, targetVersion, updateUrl);
		} catch (Exception exception) {
			LogUtil.warn(Env.CLIENT, "[DMZ-VERSION] Forge update metadata check failed: {}", exception.getMessage());
			return null;
		}
	}

	private static Optional<String> parseLatestVersion(String html) {
		String normalized = html
				.replaceAll("(?is)<script.*?</script>", " ")
				.replaceAll("(?is)<style.*?</style>", " ")
				.replaceAll("(?i)<br\\s*/?>", "\n")
				.replaceAll("(?is)<[^>]+>", " ")
				.replace("&nbsp;", " ")
				.replace("&amp;", "&")
				.replace("&#39;", "'")
				.replace("&quot;", "\"")
				.replaceAll("\\s+", " ")
				.trim();

		for (Pattern pattern : CURSEFORGE_VERSION_PATTERNS) {
			Matcher matcher = pattern.matcher(normalized);
			if (matcher.find()) {
				return Optional.ofNullable(matcher.group("version"));
			}
		}

		return Optional.empty();
	}

	private static boolean isNewerThanCurrent(String latestVersion, String currentVersion) {
		try {
			return new DefaultArtifactVersion(latestVersion)
					.compareTo(new DefaultArtifactVersion(currentVersion)) > 0;
		} catch (Exception exception) {
			LogUtil.warn(Env.CLIENT, "[DMZ-VERSION] Could not compare versions '{}' and '{}': {}",
					latestVersion, currentVersion, exception.getMessage());
			return !latestVersion.equalsIgnoreCase(currentVersion);
		}
	}

	private record UpdateInfo(String currentVersion, String latestVersion, String updateUrl) {
	}
}
