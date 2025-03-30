package com.yuseix.dragonminez.core.common.config.util;

import com.google.gson.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for handling JSON5 file operations using Gson.
 * Provides methods for creating directories, reading/writing JSON5 files,
 * listing JSON5 resources, and serializing/deserializing objects.
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
     * The file extension used for JSON5 configuration files.
     */
    public static final String FILE_EXTENSION = ".json5";

    /**
     * Loads JSON5 data from an InputStream and triggers a callback with the parsed object.
     *
     * @param clazz The class type of the object to parse.
     * @param inputStream The InputStream containing the JSON5 data.
     * @param onFetched A Consumer that will be called with the parsed object.
     * @param <T> The type of the object to deserialize.
     */
    public static <T> void loadJsonFromStream(Class<T> clazz, InputStream inputStream, Consumer<T> onFetched) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            final String json = GsonUtil.readJson5(reader);
            final T data = GSON.fromJson(json, clazz);
            onFetched.accept(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON5 data from InputStream", e);
        }
    }

    /**
     * Loads JSON5 data from a file and triggers a callback with the parsed object.
     *
     * @param clazz The class type of the object to parse.
     * @param file The File containing the JSON5 data.
     * @param onFetched A Consumer that will be called with the parsed object.
     * @param <T> The type of the object to deserialize.
     */
    public static <T> void loadJsonFromFile(Class<T> clazz, File file, Consumer<T> onFetched) {
        try (FileReader reader = new FileReader(file)) {
            final String json = GsonUtil.readJson5(reader);
            final T data = GSON.fromJson(json, clazz);
            onFetched.accept(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON5 data from file", e);
        }
    }

    /**
     * Saves an object as a JSON5 file.
     *
     * @param object The object to serialize.
     * @param filePath The directory where the file will be saved.
     * @param fileName The name of the file (without {@link GsonUtil#FILE_EXTENSION} extension).
     * @throws IOException If an error occurs while writing the file.
     */
    public static <T> void saveJson(T object, String filePath, String fileName) throws IOException {
        final File file = new File(filePath, fileName + GsonUtil.FILE_EXTENSION);
        try (FileWriter writer = new FileWriter(file)) {
            final String json = GSON.toJson(object);
            writer.write(json);
        }
    }

    /**
     * Reads a JSON5 file and deserializes it into an object of the specified type.
     *
     * @param clazz The class of the object to deserialize.
     * @param filePath The directory where the JSON5 file is located.
     * @param fileName The name of the JSON5 file (without {@link GsonUtil#FILE_EXTENSION} extension).
     * @return The deserialized object.
     */
    public static <T> T readJson(Class<T> clazz, String filePath, String fileName) {
        try (FileReader reader = new FileReader(filePath + File.separator + fileName + GsonUtil.FILE_EXTENSION)) {
            final String json = GsonUtil.readJson5(reader);
            return GSON.fromJson(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON5 file", e);
        }
    }

    /**
     * Copies the content of an InputStream to a file at the specified path.
     *
     * @param inputStream The InputStream containing the data to copy.
     * @param filePath The path to the file where the data will be saved.
     * @throws IOException If an error occurs while writing the file.
     */
    public static void copyStreamToFile(InputStream inputStream, String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             FileWriter writer = new FileWriter(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        }
    }

    /**
     * Retrieves a list of files from a specified directory with an optional file extension filter.
     *
     * @param folderPath The directory to search.
     * @param extension The file extension filter (e.g., {@link GsonUtil#FILE_EXTENSION}). Null or empty retrieves all files.
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
                .filter(file -> extension == null || extension.isEmpty() || file.getName().endsWith(extension))
                .toList();
    }

    /**
     * Deletes a JSON5 file.
     *
     * @param dataDir The directory where the file is located.
     * @param identifier The name of the file (without {@link GsonUtil#FILE_EXTENSION} extension).
     * @return True if the file was deleted, false otherwise.
     */
    public static boolean deleteJson(String dataDir, String identifier) {
        final File file = new File(dataDir + File.separator + identifier + GsonUtil.FILE_EXTENSION);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * Reads a JSON5 string from a Reader, removes comments and trailing commas, and converts it to standard JSON.
     *
     * @param reader The input reader containing JSON5.
     * @return The converted JSON string.
     * @throws IOException If an error occurs while reading the input.
     */
    private static String readJson5(Reader reader) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }

        // Remove single-line comments (//)
        String json5 = sb.toString().replaceAll("//.*", "");

        // Remove multi-line comments (/*...*/)
        json5 = json5.replaceAll("/\\*.*?\\*/", "");

        // Trim trailing commas (e.g., "key1": "value",, -> "key1": "value")
        json5 = json5.replaceAll(",\\s*}", "}");
        json5 = json5.replaceAll(",\\s*\\]", "]");

        return json5;
    }
}
