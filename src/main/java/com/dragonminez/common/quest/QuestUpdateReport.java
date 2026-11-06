package com.dragonminez.common.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuestUpdateReport {

	public record Conflict(String path, String userValue, String oldDefault, String newDefault) {}

	public record Retired(String relativePath, String backupPath, String fromVersion) {}

	public static final class FileReport {
		public final String relativePath;
		public final String fromVersion;
		public final String toVersion;
		public int appliedCount;
		public final List<Conflict> conflicts = new ArrayList<>();
		public final List<String> keptDeletions = new ArrayList<>();
		public final List<String> notes = new ArrayList<>();

		FileReport(String relativePath, String fromVersion, String toVersion) {
			this.relativePath = relativePath;
			this.fromVersion = fromVersion;
			this.toVersion = toVersion;
		}

		public boolean hasChanges() {
			return appliedCount > 0 || !conflicts.isEmpty() || !keptDeletions.isEmpty() || !notes.isEmpty();
		}
	}

	private static final Map<String, FileReport> REPORTS = new LinkedHashMap<>();
	private static final List<Retired> RETIRED = new ArrayList<>();

	private QuestUpdateReport() {}

	public static void clear() {
		REPORTS.clear();
		RETIRED.clear();
	}

	static FileReport forFile(String relativePath, String fromVersion, String toVersion) {
		return REPORTS.computeIfAbsent(relativePath, k -> new FileReport(relativePath, fromVersion, toVersion));
	}

	static void retired(String relativePath, String backupPath, String fromVersion) {
		RETIRED.add(new Retired(relativePath, backupPath, fromVersion));
	}

	public static List<Retired> retiredFiles() {
		return List.copyOf(RETIRED);
	}

	public static List<FileReport> changedFiles() {
		List<FileReport> result = new ArrayList<>();
		for (FileReport report : REPORTS.values()) {
			if (report.hasChanges()) result.add(report);
		}
		return result;
	}

	public static boolean isEmpty() {
		return changedFiles().isEmpty() && RETIRED.isEmpty();
	}

	public static int totalApplied() {
		int total = 0;
		for (FileReport report : REPORTS.values()) total += report.appliedCount;
		return total;
	}

	public static int totalConflicts() {
		int total = 0;
		for (FileReport report : REPORTS.values()) total += report.conflicts.size();
		return total;
	}

	public static int totalKeptDeletions() {
		int total = 0;
		for (FileReport report : REPORTS.values()) total += report.keptDeletions.size();
		return total;
	}
}
