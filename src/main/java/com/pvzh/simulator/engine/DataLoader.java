package com.pvzh.simulator.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvzh.simulator.model.CardDefinition;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Handles JSON ingestion to populate the CardRegistry.
 */
public class DataLoader {
    private final ObjectMapper mapper;
    private final CardRegistry registry;

    public DataLoader(CardRegistry registry) {
        this.mapper = new ObjectMapper();
        this.registry = registry;
    }

    /**
     * Loads an array of CardDefinitions from a JSON file and registers them.
     */
    public void loadCardsFromJson(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("JSON file not found: " + filePath);
        }

        List<CardDefinition> cards = mapper.readValue(file, new TypeReference<List<CardDefinition>>() {});

        for (CardDefinition def : cards) {
            registry.registerCard(def);
        }
    }
}
