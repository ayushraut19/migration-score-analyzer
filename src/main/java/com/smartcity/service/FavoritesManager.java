package com.smartcity.service;

import java.io.*;
import java.util.*;

/**
 * Service for managing favorite localities
 */
public class FavoritesManager {
    
    private static final String FAVORITES_FILE = "data/favorites.txt";
    private Set<String> favorites;

    public FavoritesManager() {
        this.favorites = new HashSet<>();
        loadFavorites();
    }

    /**
     * Load favorites from file
     */
    private void loadFavorites() {
        try {
            File file = new File(FAVORITES_FILE);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        favorites.add(line.trim());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading favorites: " + e.getMessage());
        }
    }

    /**
     * Save favorites to file
     */
    public void saveFavorites() {
        try {
            File dir = new File("data");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(FAVORITES_FILE))) {
                for (String favorite : favorites) {
                    writer.println(favorite);
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving favorites: " + e.getMessage());
        }
    }

    /**
     * Add favorite
     */
    public void addFavorite(String localityId) {
        favorites.add(localityId);
        saveFavorites();
    }

    /**
     * Remove favorite
     */
    public void removeFavorite(String localityId) {
        favorites.remove(localityId);
        saveFavorites();
    }

    /**
     * Check if favorite
     */
    public boolean isFavorite(String localityId) {
        return favorites.contains(localityId);
    }

    /**
     * Get all favorites
     */
    public Set<String> getFavorites() {
        return new HashSet<>(favorites);
    }

    /**
     * Clear all favorites
     */
    public void clearFavorites() {
        favorites.clear();
        saveFavorites();
    }
}
