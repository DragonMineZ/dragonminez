package com.dragonminez.common.config;

import com.google.gson.*;

import java.io.Reader;
import java.util.regex.Pattern;


public class Json5Parser {
    private static final Gson GSON = new GsonBuilder()
            .setLenient()
            .setPrettyPrinting()
            .create();

    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("//.*");
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern TRAILING_COMMA = Pattern.compile(",\\s*([}\\]])");
    private static final Pattern UNQUOTED_KEY = Pattern.compile("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)\\s*:");

    public static <T> T parse(String json5Content, Class<T> clazz) {
        String jsonContent = convertJson5ToJson(json5Content);
        return GSON.fromJson(jsonContent, clazz);
    }

    public static <T> T parse(Reader reader, Class<T> clazz) {
        StringBuilder sb = new StringBuilder();
        try {
            int ch;
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading JSON5", e);
        }
        return parse(sb.toString(), clazz);
    }

    public static String toJson5(Object obj) {
        return GSON.toJson(obj);
    }

    private static String convertJson5ToJson(String json5) {
        String json = json5;

        json = MULTI_LINE_COMMENT.matcher(json).replaceAll("");
        json = SINGLE_LINE_COMMENT.matcher(json).replaceAll("");
        json = TRAILING_COMMA.matcher(json).replaceAll("$1");
        json = UNQUOTED_KEY.matcher(json).replaceAll("$1\"$2\":");

        return json;
    }

    public static JsonElement parseToJsonElement(String json5Content) {
        String jsonContent = convertJson5ToJson(json5Content);
        return JsonParser.parseString(jsonContent);
    }
}

