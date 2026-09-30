package com.dragonminez.common.training;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class RhythmChart {
	public static final int MAX_NOTES = 8192;
	public static final int MAX_DURATION_MS = 20 * 60 * 1000;
	public static final int SUB_STEPS = 256;
	public static final int VARIANT_INSTRUMENTAL = 0;
	public static final int VARIANT_VOCAL = 1;

	public final String id;
	public final String item;
	public final String sound;
	public final float bpm;
	public final int durationMs;
	public final List<Note> notes;
	public final List<Note> vocalNotes;
	public final String audioHash;
	private final double[] sortedLevels;
	private final double[] sortedVocalLevels;

	public record Note(int timeMs, int lane, int lengthMs, int tier, int sub) {
		public Note(int timeMs, int lane, int lengthMs, int tier) {
			this(timeMs, lane, lengthMs, tier, 0);
		}

		public double level() {
			return (tier + sub / (double) SUB_STEPS) / 4.0;
		}
	}

	public RhythmChart(String id, String item, String sound, float bpm, int durationMs, List<Note> notes) {
		this(id, item, sound, bpm, durationMs, notes, List.of());
	}

	public RhythmChart(String id, String item, String sound, float bpm, int durationMs, List<Note> notes, List<Note> vocalNotes) {
		this(id, item, sound, bpm, durationMs, notes, vocalNotes, "");
	}

	public RhythmChart(String id, String item, String sound, float bpm, int durationMs, List<Note> notes, List<Note> vocalNotes, String audioHash) {
		this.id = id;
		this.audioHash = audioHash == null ? "" : audioHash;
		this.item = item == null ? "" : item;
		this.sound = sound == null ? "" : sound;
		this.bpm = bpm;
		this.durationMs = Math.max(0, Math.min(MAX_DURATION_MS, durationMs));
		this.notes = sortedCopy(notes);
		this.vocalNotes = vocalNotes == null ? List.of() : sortedCopy(vocalNotes);
		this.sortedLevels = levels(this.notes);
		this.sortedVocalLevels = levels(this.vocalNotes);
	}

	private static List<Note> sortedCopy(List<Note> source) {
		List<Note> sorted = new ArrayList<>(source);
		sorted.sort(Comparator.comparingInt(Note::timeMs).thenComparingInt(Note::lane));
		return Collections.unmodifiableList(sorted);
	}

	private static double[] levels(List<Note> list) {
		double[] levels = new double[list.size()];
		for (int i = 0; i < list.size(); i++) levels[i] = list.get(i).level();
		Arrays.sort(levels);
		return levels;
	}

	public static RhythmChart fromJson(String id, JsonObject root) {
		String item = root.has("item") ? root.get("item").getAsString() : "";
		String sound = root.has("sound") ? root.get("sound").getAsString() : "";
		float bpm = root.has("bpm") ? root.get("bpm").getAsFloat() : 120f;
		int duration = root.get("durationMs").getAsInt();
		List<Note> notes = parseNotes(root.getAsJsonArray("notes"), duration);
		List<Note> vocal = root.has("vocal") && root.get("vocal").isJsonArray() ? parseNotes(root.getAsJsonArray("vocal"), duration) : List.of();
		String hash = root.has("audioHash") ? root.get("audioHash").getAsString() : "";
		return new RhythmChart(id, item, sound, bpm, duration, notes, vocal, hash);
	}

	private static List<Note> parseNotes(JsonArray array, int duration) {
		List<Note> notes = new ArrayList<>();
		if (array == null) return notes;
		for (JsonElement element : array) {
			if (notes.size() >= MAX_NOTES) break;
			JsonArray n = element.getAsJsonArray();
			int time = n.get(0).getAsInt();
			int lane = n.get(1).getAsInt();
			int length = n.size() > 2 ? n.get(2).getAsInt() : 0;
			int tier = n.size() > 3 ? n.get(3).getAsInt() : 0;
			int sub = n.size() > 4 ? n.get(4).getAsInt() : 0;
			if (time < 0 || time > duration || lane < 0 || lane >= RhythmLogic.LANES) continue;
			notes.add(new Note(time, lane, Math.max(0, length), clampTier(tier), clampSub(sub)));
		}
		return notes;
	}

	private static int clampTier(int tier) {
		return Math.max(0, Math.min(3, tier));
	}

	private static int clampSub(int sub) {
		return Math.max(0, Math.min(SUB_STEPS - 1, sub));
	}

	public static int clampVariant(int variant) {
		return variant == VARIANT_VOCAL ? VARIANT_VOCAL : VARIANT_INSTRUMENTAL;
	}

	public boolean hasVocals() {
		return !vocalNotes.isEmpty();
	}

	public int effectiveVariant(int requested) {
		return clampVariant(requested) == VARIANT_VOCAL && hasVocals() ? VARIANT_VOCAL : VARIANT_INSTRUMENTAL;
	}

	public List<Note> notesFor(int variant) {
		return effectiveVariant(variant) == VARIANT_VOCAL ? vocalNotes : notes;
	}

	public RhythmChart withoutVocals() {
		return hasVocals() ? new RhythmChart(id, item, sound, bpm, durationMs, notes, List.of(), audioHash) : this;
	}

	public RhythmChart withAudioHash(String hash) {
		return new RhythmChart(id, item, sound, bpm, durationMs, notes, vocalNotes, hash);
	}

	public boolean matchesAudio(String hash) {
		return audioHash.isEmpty() || hash == null || hash.isEmpty() || audioHash.equalsIgnoreCase(hash);
	}

	public double playableSeconds() {
		return playableSeconds(VARIANT_INSTRUMENTAL);
	}

	public double playableSeconds(int variant) {
		List<Note> list = notesFor(variant);
		if (list.isEmpty()) return Math.max(10.0, durationMs / 1000.0);
		return Math.max(10.0, (list.get(list.size() - 1).timeMs() - list.get(0).timeMs()) / 1000.0);
	}

	public int countAt(double intensity) {
		return countAt(intensity, VARIANT_INSTRUMENTAL);
	}

	public int countAt(double intensity, int variant) {
		double[] levels = effectiveVariant(variant) == VARIANT_VOCAL ? sortedVocalLevels : sortedLevels;
		int lo = 0, hi = levels.length;
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (levels[mid] <= intensity) lo = mid + 1;
			else hi = mid;
		}
		return lo;
	}

	public double densityAt(double intensity) {
		return densityAt(intensity, VARIANT_INSTRUMENTAL);
	}

	public double densityAt(double intensity, int variant) {
		return countAt(intensity, variant) / playableSeconds(variant);
	}

	public double tierDensity(int tier) {
		return densityAt((tier + 1) / 4.0 - 1e-9);
	}

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("item", item);
		root.addProperty("sound", sound);
		root.addProperty("bpm", bpm);
		root.addProperty("durationMs", durationMs);
		if (!audioHash.isEmpty()) root.addProperty("audioHash", audioHash);
		root.add("notes", notesToJson(notes));
		if (hasVocals()) root.add("vocal", notesToJson(vocalNotes));
		return root;
	}

	private static JsonArray notesToJson(List<Note> list) {
		JsonArray array = new JsonArray();
		for (Note note : list) {
			JsonArray entry = new JsonArray();
			entry.add(note.timeMs());
			entry.add(note.lane());
			entry.add(note.lengthMs());
			entry.add(note.tier());
			entry.add(note.sub());
			array.add(entry);
		}
		return array;
	}

	public byte[] encode() {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeUTF(id);
			out.writeUTF(item);
			out.writeUTF(sound);
			out.writeFloat(bpm);
			out.writeInt(durationMs);
			writeNotes(out, notes);
			writeNotes(out, vocalNotes);
			out.writeUTF(audioHash);
			out.flush();
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void writeNotes(DataOutputStream out, List<Note> list) throws IOException {
		out.writeInt(list.size());
		int previous = 0;
		for (Note note : list) {
			out.writeInt(note.timeMs() - previous);
			previous = note.timeMs();
			out.writeByte((note.lane() & 0x0F) | (note.tier() << 4));
			out.writeByte(note.sub());
			out.writeInt(note.lengthMs());
		}
	}

	private static List<Note> readNotes(DataInputStream in) throws IOException {
		int count = Math.min(MAX_NOTES, Math.max(0, in.readInt()));
		List<Note> notes = new ArrayList<>(count);
		int time = 0;
		for (int i = 0; i < count; i++) {
			time += in.readInt();
			int packed = in.readUnsignedByte();
			int sub = in.readUnsignedByte();
			int length = in.readInt();
			notes.add(new Note(time, packed & 0x0F, length, (packed >> 4) & 0x03, sub));
		}
		return notes;
	}

	public static RhythmChart decode(byte[] data) {
		try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
			String id = in.readUTF();
			String item = in.readUTF();
			String sound = in.readUTF();
			float bpm = in.readFloat();
			int duration = in.readInt();
			List<Note> notes = readNotes(in);
			List<Note> vocal = in.available() > 0 ? readNotes(in) : List.of();
			String hash = in.available() > 0 ? in.readUTF() : "";
			return new RhythmChart(id, item, sound, bpm, duration, notes, vocal, hash);
		} catch (IOException e) {
			return null;
		}
	}
}
