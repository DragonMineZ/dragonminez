package com.dragonminez.common.training;

import java.util.List;

public final class BuiltinSongs {
	public static final String PREFIX = "song:";

	public record Song(String key, String sound, String title, String artist, int durationMs) {
		public String fileStem() {
			return key.substring(PREFIX.length());
		}
	}

	public static final List<Song> ALL = List.of(
			new Song(PREFIX + "ill_forget_about_you_in_time", "dragonminez:minigame.ill_forget_about_you_in_time", "I'll Forget About You (In Time)", "Laufey", 252169),
			new Song(PREFIX + "oh_yeah", "dragonminez:minigame.oh_yeah", "oh yeah?", "Steve Lacy", 169685)
	);

	private BuiltinSongs() {}

	public static boolean isBuiltin(String key) {
		return key != null && key.startsWith(PREFIX);
	}

	public static Song find(String key) {
		if (key == null) return null;
		for (Song song : ALL) if (song.key().equals(key)) return song;
		return null;
	}
}
