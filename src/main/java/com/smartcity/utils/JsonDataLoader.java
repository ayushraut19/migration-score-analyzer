package com.smartcity.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartcity.model.Locality;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads cached locality data from JSON.
 * This cache is only a resilience layer for API failure scenarios; it is not
 * the primary source of truth for live recommendation refreshes.
 */
public class JsonDataLoader {

    private static final String DATA_FILE = "data/localities.json";
    private static final Gson GSON = new Gson();

    public static List<Locality> loadLocalities() {
        Path path = Paths.get(DATA_FILE);
        if (!Files.exists(path)) {
            System.err.println("Cached locality file not found: " + DATA_FILE);
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<Locality>>() { }.getType();
            List<Locality> localities = GSON.fromJson(reader, listType);
            return localities != null ? localities : new ArrayList<>();
        } catch (IOException ex) {
            System.err.println("Error loading cached locality JSON: " + ex.getMessage());
            return new ArrayList<>();
        }
    }
}
