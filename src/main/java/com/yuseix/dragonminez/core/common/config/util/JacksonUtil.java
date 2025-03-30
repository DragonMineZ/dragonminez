package com.yuseix.dragonminez.core.common.config.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for handling JSON5 file operations using Jackson.
 * This class provides methods for reading and writing JSON5 files with comment support,
 * automatic Java object mapping, and additional file operations.
 * It is designed for use in applications that need to work with JSON5 files,
 * including operations such as loading, saving, and deleting JSON5 files.
 *
 * <p>Key features of the Jackson ObjectMapper configured in this class:
 * - Allows comments (single-line and multi-line) in JSON5 files.
 * - Supports trailing commas and single quotes for JSON5 compatibility.
 * - Enables pretty printing of JSON5 files.
 * - Disables HTML escaping in JSON output.
 * </p>
 *
 * <p>Usage Example:</p>
 * <pre>
 * JacksonUtil.loadJsonFromFile(MyClass.class, new File("data.json5"), myObject -> {
 *     // Process the loaded object
 * });
 * </pre>
 */
public class JacksonUtil {

    // Private constructor to prevent instantiation.
    private JacksonUtil() {}

    /**
     * Jackson ObjectMapper configured for JSON5 compatibility:
     * - Allows comments (// and /*)
     * - Allows trailing commas
     * - Allows single quotes
     * - Pretty printing
     * - Disables HTML escaping
     */
    public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_TRAILING_COMMA)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
            .build()
            .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

    /**
     * The file extension used for JSON5 files.
     */
    public static final String FILE_EXTENSION = ".json5";

    /**
     * Loads a JSON5 file from an InputStream and maps it to an object of the specified class.
     *
     * @param clazz The class to map the JSON5 content to.
     * @param inputStream The InputStream containing the JSON5 data.
     * @param onFetched A Consumer that processes the loaded object.
     * @param <T> The type of the object to map the JSON5 data to.
     * @throws RuntimeException if there is an error during the deserialization process.
     */
    public static <T> void loadJsonFromStream(Class<T> clazz, InputStream inputStream, Consumer<T> onFetched) {
        try {
            T data = OBJECT_MAPPER.readValue(inputStream, clazz);
            onFetched.accept(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON5 data from InputStream", e);
        }
    }

    /**
     * Loads a JSON5 file from the given file and maps it to an object of the specified class.
     *
     * @param clazz The class to map the JSON5 content to.
     * @param file The file containing the JSON5 data.
     * @param onFetched A Consumer that processes the loaded object.
     * @param <T> The type of the object to map the JSON5 data to.
     * @throws RuntimeException if there is an error during the deserialization process.
     */
    public static <T> void loadJsonFromFile(Class<T> clazz, File file, Consumer<T> onFetched) {
        try {
            T data = OBJECT_MAPPER.readValue(file, clazz);
            onFetched.accept(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON5 data from file", e);
        }
    }

    /**
     * Saves an object as a JSON5 file at the specified file path with the given file name.
     *
     * @param object The object to be serialized and saved as a JSON5 file.
     * @param filePath The directory where the file should be saved.
     * @param fileName The name of the file to save the object to (without the extension).
     * @param <T> The type of the object to be saved.
     * @throws IOException if there is an error during the file writing process.
     */
    public static <T> void saveJson(T object, String filePath, String fileName) throws IOException {
        File file = new File(filePath, fileName + FILE_EXTENSION);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }
        OBJECT_MAPPER.writeValue(file, object);
    }

    /**
     * Reads a JSON5 file and maps it to an object of the specified class.
     *
     * @param clazz The class to map the JSON5 content to.
     * @param filePath The directory where the JSON5 file is located.
     * @param fileName The name of the JSON5 file (without the extension).
     * @param <T> The type of the object to map the JSON5 data to.
     * @return The deserialized object.
     * @throws RuntimeException if there is an error during the deserialization process.
     */
    public static <T> T readJson(Class<T> clazz, String filePath, String fileName) {
        try {
            File file = new File(filePath + File.separator + fileName + FILE_EXTENSION);
            return OBJECT_MAPPER.readValue(file, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON5 file", e);
        }
    }

    /**
     * Copies data from an InputStream to a file at the specified file path.
     *
     * @param inputStream The InputStream containing the data to copy.
     * @param filePath The file path to write the data to.
     * @throws IOException if there is an error during the file writing process.
     */
    public static void copyStreamToFile(InputStream inputStream, String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             FileWriter writer = new FileWriter(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        }
    }

    /**
     * Returns a list of files in a specified directory with a specific file extension.
     *
     * @param folderPath The path to the directory.
     * @param extension The file extension to filter by (can be null or empty to include all files).
     * @return A list of files in the directory matching the specified extension.
     */
    public static List<File> getFilesInDirectory(String folderPath, String extension) {
        File directory = new File(folderPath);
        if (!directory.exists()) {
            return new ArrayList<>();
        }
        File[] rawFiles = directory.listFiles();
        return rawFiles == null ? new ArrayList<>() : Arrays.stream(rawFiles)
                .filter(File::isFile)
                .filter(file -> extension == null || extension.isEmpty() || file.getName().endsWith(extension))
                .toList();
    }

    /**
     * Deletes a JSON5 file from the specified directory.
     *
     * @param dataDir The directory where the file is located.
     * @param identifier The identifier (name without extension) of the file to delete.
     * @return True if the file was successfully deleted, false otherwise.
     */
    public static boolean deleteJson(String dataDir, String identifier) {
        File file = new File(dataDir + File.separator + identifier + FILE_EXTENSION);
        return file.exists() && file.delete();
    }
}
