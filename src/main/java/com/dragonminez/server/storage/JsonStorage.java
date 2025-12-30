package com.dragonminez.server.storage;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class JsonStorage implements IDataStorage {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private Path storageDir;

	@Override
	public void init() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			this.storageDir = server.getWorldPath(LevelResource.ROOT).resolve("dragonminez").resolve("playerdata_json");
			try {
				Files.createDirectories(storageDir);
				LogUtil.info(Env.SERVER, "JSON Storage initialized at: " + storageDir);
			} catch (IOException e) {
				LogUtil.error(Env.SERVER, "Failed to create JSON storage directory", e);
			}
		}
	}

	@Override
	public void shutdown() {
	}

	@Override
	public CompoundTag loadData(UUID playerUUID) {
		if (storageDir == null) return null;

		Path file = storageDir.resolve(playerUUID.toString() + ".json");
		if (!Files.exists(file)) return null;

		try (Reader reader = Files.newBufferedReader(file)) {
			JsonElement json = GSON.fromJson(reader, JsonElement.class);
			return (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json);
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "Failed to load JSON data for " + playerUUID, e);
			return null;
		}
	}

	@Override
	public boolean saveData(UUID playerUUID, String playerName, CompoundTag data) {
		if (storageDir == null) return false;

		Path file = storageDir.resolve(playerUUID.toString() + ".json");
		try (Writer writer = Files.newBufferedWriter(file)) {
			JsonElement json = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, data);
			GSON.toJson(json, writer);
			return true;
		} catch (IOException e) {
			LogUtil.error(Env.SERVER, "Failed to save JSON data for " + playerName, e);
			return false;
		}
	}

	@Override
	public String getName() {
		return "JSON";
	}
}