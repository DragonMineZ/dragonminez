package com.yuseix.dragonminez.core.common.config.util;

import com.google.gson.*;
import org.hjson.JsonValue;
import org.hjson.Stringify;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for handling JSON and HJSON file operations using Gson and Hjson.
 * Provides methods for creating directories, reading/writing JSON files,
 * listing JSON resources, and serializing/deserializing objects.
 */
public class GsonUtil {

    /**
     * Private constructor to prevent instantiation.
     * This class only contains static methods and should not be instantiated.
     */
    private GsonUtil() {}

    /**
     * Gson instance with pretty printing and HTML escaping disabled.
     */
    public final static Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    /**
     * Loads JSON data from an InputStream and triggers a callback with the parsed object.
     *
     * @param clazz The class type of the object to parse.
     * @param inputStream The InputStream containing the JSON data.
     * @param onFetched A Consumer that will be called with the parsed object.
     * @param <T> The type of the object to deserialize.
     */
    public static <T> void loadJsonFromStream(Class<T> clazz, InputStream inputStream, Consumer<T> onFetched) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            final String json = GsonUtil.readHjson(reader);
            final T data = GSON.fromJson(json, clazz);
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
     * @param <T> The type of the object to deserialize.
     */
    public static <T> void loadJsonFromFile(Class<T> clazz, File file, Consumer<T> onFetched) {
        try (FileReader reader = new FileReader(file)) {
            final String json = GsonUtil.readHjson(reader);
            final T data = GSON.fromJson(json, clazz);
            onFetched.accept(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON data from file", e);
        }
    }

    /**
     * Saves an object as an HJSON file.
     *
     * @param object The object to serialize.
     * @param filePath The directory where the file will be saved.
     * @param fileName The name of the file (without ".json" extension).
     * @throws IOException If an error occurs while writing the file.
     */
    public static void saveJson(Object object, String filePath, String fileName) throws IOException {
        final File file = new File(filePath, fileName + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            final String hjson = JsonValue.readJSON(GSON.toJson(object)).toString(Stringify.HJSON);
            writer.write(hjson);
        }
    }

    /**
     * Reads a JSON file and deserializes it into an object of the specified type.
     *
     * @param clazz The class of the object to deserialize.
     * @param filePath The directory where the JSON file is located.
     * @param fileName The name of the JSON file (without ".json" extension).
     * @return The deserialized object.
     */
    public static <T> T readJson(Class<T> clazz, String filePath, String fileName) {
        try (FileReader reader = new FileReader(filePath + File.separator + fileName + ".json")) {
            final String json = GsonUtil.readHjson(reader);
            return GSON.fromJson(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file", e);
        }
    }

    /**
     * Retrieves a list of files from a specified directory with an optional file extension filter.
     *
     * @param folderPath The directory to search.
     * @param extension The file extension filter (e.g., "json"). Null or empty retrieves all files.
     * @return A list of matching files.
     */
    public static List<File> getFilesInDirectory(String folderPath, String extension) {
        final File directory = new File(folderPath);
        if (!directory.exists()) {
            return new ArrayList<>();
        }
        final File[] rawFiles = directory.listFiles();
        return rawFiles == null ? new ArrayList<>() : Arrays.stream(rawFiles)
                .filter(File::isFile)
                .filter(file -> extension == null || extension.isEmpty() || file.getName().endsWith("." + extension))
                .toList();
    }

    /**
     * Deletes a JSON file.
     *
     * @param dataDir The directory where the file is located.
     * @param identifier The name of the file (without ".json" extension).
     * @return True if the file was deleted, false otherwise.
     */
    public static boolean deleteJson(String dataDir, String identifier) {
        final File file = new File(dataDir + File.separator + identifier + ".json");
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * Reads an HJSON string from a Reader and converts it to standard JSON.
     *
     * @param reader The input reader containing HJSON.
     * @return The converted JSON string.
     * @throws IOException If an error occurs while reading the input.
     */
    private static String readHjson(Reader reader) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return JsonValue.readHjson(sb.toString()).toString();
    }
}
