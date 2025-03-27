package com.yuseix.dragonminez.common.config.util;

import com.google.gson.*;
import com.yuseix.dragonminez.common.util.LogUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for handling JSON file operations using Gson.
 * Provides methods for creating directories, reading/writing JSON files,
 * listing JSON resources, and serializing/deserializing objects.
 */
public class GsonUtil {

    /**
     * Gson instance with pretty printing and HTML escaping disabled.
     */
    public final static Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    /**
     * Creates a directory if it does not already exist.
     *
     * @param filePath The path of the directory to create.
     * @return The created directory as a File object, or null if creation failed.
     */
    public static File createDirectory(String filePath) {
        final File directory = new File(filePath);
        if (!directory.exists()) {
            LogUtil.info("Directory not found at {}. Creating it...", filePath);
            if (!directory.mkdirs()) {
                LogUtil.warn("Could not create data directory at {}", filePath);
                return null;
            }
            LogUtil.info("Directory created at {}", filePath);
        }
        return directory;
    }

    /**
     * Loads JSON data from an InputStream and triggers a callback with the parsed object.
     *
     * @param clazz The class type of the object to parse.
     * @param inputStream The InputStream containing the JSON data.
     * @param onFetched A Consumer that will be called with the parsed object.
     */
    public static <T> void loadJsonFromStream(Class<T> clazz, InputStream inputStream, Consumer<T> onFetched) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            // Parse the JSON using Gson
            final T data = GsonUtil.GSON.fromJson(reader, clazz);

            // Trigger the consumer with the fetched data
            onFetched.accept(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON data from InputStream", e);
        }
    }

    /**
     * Loads JSON data from a file and triggers a callback with the parsed object.
     *
     * @param clazz The class type of the object to parse.
     * @param file The File containing the JSON data.
     * @param onFetched A Consumer that will be called with the parsed object.
     */
    public static <T> void loadJsonFromFile(Class<T> clazz, File file, Consumer<T> onFetched) {
        try (FileReader reader = new FileReader(file)) {
            // Parse the JSON using Gson
            final T data = GsonUtil.GSON.fromJson(reader, clazz);

            // Trigger the consumer with the fetched data
            onFetched.accept(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON data from file", e);
        }
    }

    /**
     * Retrieves a list of files from a specified directory with an optional file extension filter.
     *
     * @param folderPath The directory to search.
     * @param extension The file extension filter (e.g., "json"). Null or empty string retrieves all files.
     * @return A list of matching files.
     */
    public static List<File> getFilesInDirectory(String folderPath, String extension) {
        final File directory = GsonUtil.createDirectory(folderPath);
        if (directory == null) {
            return new ArrayList<>();
        }
        final File[] rawFiles = directory.listFiles();
        return rawFiles == null ? new ArrayList<>() : Arrays.stream(rawFiles).filter(File::isFile)
                .filter(file -> extension == null || extension.isEmpty() || file.getName().endsWith("." + extension))
                .toList();
    }

    /**
     * Retrieves a list of all JSON files in the specified resource directory.
     *
     * @param resourceManager The resource manager to use for listing resources.
     * @param resourceDir The directory to search for JSON files.
     * @return A list of resource locations for all JSON files in the directory.
     */
    public static List<ResourceLocation> getJsonFiles(ResourceManager resourceManager, String resourceDir) {
        try {
            return new ArrayList<>(resourceManager.listResources(resourceDir, path -> path.toString().endsWith(".json"))
                    .keySet());
        } catch (Exception e) {
            LogUtil.error("Failed to list resources in directory: {}", resourceDir, e);
            return List.of();
        }
    }

    /**
     * Serializes an object and saves it as a JSON file.
     *
     * @param object The object to serialize.
     * @param filePath The target directory.
     * @param fileName The name of the JSON file (without ".json" extension).
     * @throws IOException If file creation or writing fails.
     */
    public static void saveJson(Object object, String filePath, String fileName) throws IOException {
        final File directory = new File(filePath);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException();
        }
        final File file = new File(filePath, fileName + ".json");
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException();
        }
        try (FileWriter writer = new FileWriter(filePath + File.separator + fileName + ".json")) {
            GsonUtil.GSON.toJson(object, writer);
        }
    }

    /**
     * Reads a JSON file and deserializes it into an object of the specified type.
     *
     * @param clazz The class of the object to deserialize.
     * @param filePath The directory where the JSON file is located.
     * @param fileName The name of the JSON file (without ".json" extension).
     * @return The deserialized object.
     * @throws JsonIOException If deserialization fails.
     * @throws FileNotFoundException If the file does not exist.
     */
    public static <T> T readJson(Class<T> clazz, String filePath, String fileName) throws JsonIOException, FileNotFoundException {
        final FileReader reader = new FileReader(filePath + File.separator + fileName + ".json");
        final T instance = GsonUtil.GSON.fromJson(reader, clazz);
        if (instance == null) {
            throw new JsonIOException("Unable to read file");
        }
        return instance;
    }

    /**
     * Reads and deserializes a JSON file.
     *
     * @param file The file to read.
     * @param clazz The class to deserialize into.
     * @return The deserialized object.
     */
    public static <T> T readJson(File file, Class<T> clazz) {
        try (FileReader reader = new FileReader(file)) {
            return GsonUtil.GSON.fromJson(reader, clazz);
        } catch (JsonSyntaxException | JsonIOException | IOException e) {
            throw new JsonIOException("Unable to read file");
        }
    }

    /**
     * Deletes a JSON file.
     *
     * @param dataDir The directory where the file is located.
     * @param identifier The name of the file (without ".json" extension).
     */
    public static void deleteJson(String dataDir, String identifier) {
        final File file = new File(dataDir + File.separator + identifier + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Serializes an object into a JSON string.
     *
     * @param object The object to serialize.
     * @return A JSON string representation of the object.
     */
    public static String serializeJson(Object object) {
        return GsonUtil.GSON.toJson(object);
    }

    /**
     * Deserializes a JSON string into an object.
     *
     * @param json The JSON string.
     * @param clazz The target class.
     * @return The deserialized object.
     */
    public static <T> T deserealizeJson(String json, Class<T> clazz) {
        return GsonUtil.GSON.fromJson(json, clazz);
    }

    /**
     * Deserializes JSON from a reader.
     *
     * @param reader The input stream reader.
     * @param clazz The target class.
     * @return The deserialized object.
     */
    public static <T> T deserealizeJson(InputStreamReader reader, Class<T> clazz) {
        return GsonUtil.GSON.fromJson(reader, clazz);
    }
}
