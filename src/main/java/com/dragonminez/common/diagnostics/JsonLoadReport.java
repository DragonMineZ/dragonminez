package com.dragonminez.common.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JsonLoadReport {

	public enum Kind { ERROR, UPDATE }

	public record Entry(Kind kind, String source, String file, String message) {}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	private JsonLoadReport() {}

	public static synchronized void clear(String source) {
		ENTRIES.removeIf(e -> e.source().equals(source));
	}

	public static synchronized void error(String source, String file, String message) {
		ENTRIES.add(new Entry(Kind.ERROR, source, file, message == null ? "" : message));
	}

	public static synchronized void update(String source, String file, String message) {
		ENTRIES.add(new Entry(Kind.UPDATE, source, file, message == null ? "" : message));
	}

	public static synchronized boolean isEmpty() {
		return ENTRIES.isEmpty();
	}

	public static synchronized List<Entry> entries() {
		return Collections.unmodifiableList(new ArrayList<>(ENTRIES));
	}

	public static synchronized long count(Kind kind) {
		return ENTRIES.stream().filter(e -> e.kind() == kind).count();
	}

	public static String rootCause(Throwable t) {
		Throwable cur = t;
		while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
		String msg = cur.getMessage();
		return msg != null && !msg.isBlank() ? msg : cur.getClass().getSimpleName();
	}
}
