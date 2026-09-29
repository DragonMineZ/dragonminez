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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class RhythmChart {
	public static final int MAX_NOTES = 8192;
	public static final int MAX_DURATION_MS = 20 * 60 * 1000;

	public final String id;
	public final String item;
	public final String sound;
	public final float bpm;
	public final int durationMs;
	public final List<Note> notes;

	public record Note(int timeMs, int lane, int lengthMs, int tier) {}

	public RhythmChart(String id, String item, String sound, float bpm, int durationMs, List<Note> notes) {
		this.id = id;
		this.item = item == null ? "" : item;
		this.sound = sound == null ? "" : sound;
		this.bpm = bpm;
		this.durationMs = Math.max(0, Math.min(MAX_DURATION_MS, durationMs));
		List<Note> sorted = new ArrayList<>(notes);
		sorted.sort(Comparator.comparingInt(Note::timeMs).thenComparingInt(Note::lane));
		this.notes = Collections.unmodifiableList(sorted);
	}

	public static RhythmChart fromJson(String id, JsonObject root) {
		String item = root.has("item") ? root.get("item").getAsString() : "";
		String sound = root.has("sound") ? root.get("sound").getAsString() : "";
		float bpm = root.has("bpm") ? root.get("bpm").getAsFloat() : 120f;
		int duration = root.get("durationMs").getAsInt();
		JsonArray array = root.getAsJsonArray("notes");
		List<Note> notes = new ArrayList<>();
		for (JsonElement element : array) {
			if (notes.size() >= MAX_NOTES) break;
			JsonArray n = element.getAsJsonArray();
			int time = n.get(0).getAsInt();
			int lane = n.get(1).getAsInt();
			int length = n.size() > 2 ? n.get(2).getAsInt() : 0;
			int tier = n.size() > 3 ? n.get(3).getAsInt() : 0;
			if (time < 0 || time > duration || lane < 0 || lane >= RhythmLogic.LANES) continue;
			notes.add(new Note(time, lane, Math.max(0, length), Math.max(0, Math.min(3, tier))));
		}
		return new RhythmChart(id, item, sound, bpm, duration, notes);
	}

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("item", item);
		root.addProperty("sound", sound);
		root.addProperty("bpm", bpm);
		root.addProperty("durationMs", durationMs);
		JsonArray array = new JsonArray();
		for (Note note : notes) {
			JsonArray entry = new JsonArray();
			entry.add(note.timeMs());
			entry.add(note.lane());
			entry.add(note.lengthMs());
			entry.add(note.tier());
			array.add(entry);
		}
		root.add("notes", array);
		return root;
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
			out.writeInt(notes.size());
			int previous = 0;
			for (Note note : notes) {
				out.writeInt(note.timeMs() - previous);
				previous = note.timeMs();
				out.writeByte((note.lane() & 0x0F) | (note.tier() << 4));
				out.writeInt(note.lengthMs());
			}
			out.flush();
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static RhythmChart decode(byte[] data) {
		try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
			String id = in.readUTF();
			String item = in.readUTF();
			String sound = in.readUTF();
			float bpm = in.readFloat();
			int duration = in.readInt();
			int count = Math.min(MAX_NOTES, Math.max(0, in.readInt()));
			List<Note> notes = new ArrayList<>(count);
			int time = 0;
			for (int i = 0; i < count; i++) {
				time += in.readInt();
				int packed = in.readUnsignedByte();
				int length = in.readInt();
				notes.add(new Note(time, packed & 0x0F, length, (packed >> 4) & 0x03));
			}
			return new RhythmChart(id, item, sound, bpm, duration, notes);
		} catch (IOException e) {
			return null;
		}
	}
}
