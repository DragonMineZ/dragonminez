package com.dragonminez.client.beta;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public final class BetaAccessVerification {
	public static final String DEFAULT_VERIFICATION_URL = "https://downloads.dragonminez.com/beta-access/start";
	public static final String VERIFICATION_URL_PROPERTY = "dragonminez.betaAccessUrl";

	private static final Pattern MINECRAFT_USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
	private static final Pattern FORMAT_CODE = Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");

	private BetaAccessVerification() {
	}

	public static boolean isValidMinecraftUsername(String username) {
		return username != null && MINECRAFT_USERNAME.matcher(username).matches();
	}

	public static String buildVerificationUrl(String username) {
		if (!isValidMinecraftUsername(username)) {
			throw new IllegalArgumentException("Invalid Minecraft username: " + username);
		}

		String baseUrl = System.getProperty(VERIFICATION_URL_PROPERTY, DEFAULT_VERIFICATION_URL).trim();
		String separator = baseUrl.contains("?") ? "&" : "?";
		String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
		return baseUrl + separator + "username=" + encodedUsername;
	}

	public static boolean isBetaAccessDisconnect(String reason) {
		if (reason == null || reason.isBlank()) {
			return false;
		}

		String normalizedReason = FORMAT_CODE.matcher(reason)
				.replaceAll("")
				.toLowerCase(Locale.ROOT);
		return normalizedReason.contains("[dragonmine z]")
				&& normalizedReason.contains("not allowed to play this beta/alpha version");
	}
}
