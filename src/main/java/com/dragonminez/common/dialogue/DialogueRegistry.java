package com.dragonminez.common.dialogue;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class DialogueRegistry {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, DialogueTree> TREES = new ConcurrentHashMap<>();

	private DialogueRegistry() {
	}

	public static void loadAll(MinecraftServer server) {
		TREES.clear();
		Path dialogueDir = server.getWorldPath(LevelResource.ROOT).resolve("dragonminez").resolve("dialogues");

		try {
			if (!Files.exists(dialogueDir)) {
				Files.createDirectories(dialogueDir);
				writeExampleFile(dialogueDir);
			}
		} catch (IOException e) {
			LogUtil.error(Env.SERVER, "Could not create dialogue folder '{}'", dialogueDir, e);
			return;
		}

		try (Stream<Path> files = Files.list(dialogueDir)) {
			files.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
					.sorted()
					.forEach(DialogueRegistry::loadFile);
		} catch (IOException e) {
			LogUtil.error(Env.SERVER, "Could not list dialogue folder '{}'", dialogueDir, e);
		}

		LogUtil.info(Env.SERVER, "Loaded {} NPC dialogue tree(s)", TREES.size());
	}

	private static void loadFile(Path path) {
		String fileName = path.getFileName().toString();
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonObject json = GSON.fromJson(reader, JsonObject.class);
			DialogueTree tree = DialogueParser.parse(json, fileName);
			if (tree != null && !tree.getNpcId().startsWith("_")) {
				TREES.put(tree.getNpcId(), tree);
			}
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "Failed to load dialogue file '{}': {}", fileName, e.toString());
		}
	}

	public static DialogueTree getTree(String npcId) {
		return npcId == null ? null : TREES.get(npcId.toLowerCase(Locale.ROOT));
	}

	public static boolean hasTree(String npcId) {
		return getTree(npcId) != null;
	}

	private static void writeExampleFile(Path dialogueDir) {
		String example = """
				{
				  "_comment": "Example dialogue tree. The leading underscore in 'npc' keeps it inactive - copy this file, set 'npc' to a real NPC id (e.g. 'roshi', 'kaiosama'), and reload with /dmzreload. Lines/choice texts accept translation keys or literal text, and %player%-style placeholders.",
				  "npc": "_example",
				  "start": "greet",
				  "nodes": {
				    "greet": {
				      "line": "Well, if it isn't %player%! What brings you here?",
				      "choices": [
				        {
				          "text": "I want to get stronger.",
				          "goto": "training",
				          "conditions": { "operator": "AND", "conditions": [ { "type": "LEVEL", "minLevel": 5 } ] }
				        },
				        {
				          "text": "Give me your money!",
				          "goto": "angry",
				          "actions": [ { "type": "ALIGNMENT", "amount": -5 } ]
				        },
				        { "text": "Show me your quests.", "actions": [ { "type": "OPEN_QUESTS" } ] },
				        { "text": "Goodbye." }
				      ]
				    },
				    "training": {
				      "line": "Ha! I like your spirit. Take these training points and come back later.",
				      "choices": [
				        { "text": "Thank you!", "actions": [ { "type": "TPS", "amount": 50 } ] }
				      ]
				    },
				    "angry": {
				      "line": "How dare you! Leave my sight at once!"
				    }
				  }
				}
				""";
		try {
			Files.writeString(dialogueDir.resolve("_example.json"), example);
		} catch (IOException e) {
			LogUtil.error(Env.SERVER, "Could not write example dialogue file", e);
		}
	}
}
