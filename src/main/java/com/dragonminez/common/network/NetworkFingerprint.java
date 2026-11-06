package com.dragonminez.common.network;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

final class NetworkFingerprint {
	private static final String[] NETWORK_PACKAGE = {"com", "dragonminez", "common", "network"};
	private static final String UNKNOWN = "unknown";
	private static String cached;

	private NetworkFingerprint() {}

	static synchronized String get() {
		if (cached == null) cached = compute();
		return cached;
	}

	private static String compute() {
		try {
			IModFileInfo info = ModList.get().getModFileById(Reference.MOD_ID);
			if (info == null) return UNKNOWN;
			Path root = info.getFile().findResource(NETWORK_PACKAGE);
			if (root == null || !Files.isDirectory(root)) return UNKNOWN;
			List<Path> classes;
			try (Stream<Path> walk = Files.walk(root)) {
				classes = walk.filter(path -> path.toString().endsWith(".class"))
						.sorted(Comparator.comparing(path -> relative(root, path)))
						.toList();
			}
			if (classes.isEmpty()) return UNKNOWN;
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			for (Path path : classes) {
				digest.update(relative(root, path).getBytes(StandardCharsets.UTF_8));
				digest.update(Files.readAllBytes(path));
			}
			return HexFormat.of().formatHex(digest.digest()).substring(0, 12);
		} catch (Exception e) {
			LogUtil.warn(Env.COMMON, "Could not fingerprint the network classes: {}", e.toString());
			return UNKNOWN;
		}
	}

	private static String relative(Path root, Path path) {
		return root.relativize(path).toString().replace('\\', '/');
	}
}
