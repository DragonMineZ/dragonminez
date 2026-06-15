package com.dragonminez.client.title;

import java.util.Locale;

public final class TitleFooterText {
	public static final String DRAGON_BALL_TRADEMARK_LINE_1 = "Dragon Ball is a trademark of";
	public static final String DRAGON_BALL_TRADEMARK_LINE_2 = "Bird Studio, Toei Animation.";

	private TitleFooterText() {}

	public static String buildVersionLine(String version) {
		String normalizedVersion = version == null || version.isBlank() ? "unknown" : version.trim();
		String lowerVersion = normalizedVersion.toLowerCase(Locale.ROOT);
		String buildType = lowerVersion.contains("beta") ? "Beta" : lowerVersion.contains("alpha") ? "Alpha" : "Release";
		String displayVersion = stripPreReleaseTag(normalizedVersion);
		return "DragonMineZ " + buildType + " v" + displayVersion;
	}

	private static String stripPreReleaseTag(String version) {
		int separatorIndex = version.indexOf('-');
		if (separatorIndex < 0) {
			return version;
		}
		return version.substring(0, separatorIndex);
	}
}