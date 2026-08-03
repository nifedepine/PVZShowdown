package com.pvzh.simulator.engine;

import com.pvzh.simulator.model.CardDefinition;
import com.pvzh.simulator.model.CardType;
import com.pvzh.simulator.model.HeroClass;
import com.pvzh.simulator.model.Side;
import com.pvzh.simulator.model.Tribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Acts as the global database for all loaded CardDefinitions.
 */
public class CardRegistry {
    private final List<CardDefinition> allCards = new ArrayList<>();
    private final Random random = new Random();

    public void registerCard(CardDefinition def) {
        allCards.add(def);
    }

    /**
     * Gets a completely random card from a provided list using Weighted Selection.
     * Essential for the Conjure mechanic.
     */
    public CardDefinition getRandomCard(List<CardDefinition> pool) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (CardDefinition def : pool) {
            totalWeight += def.getRngWeight();
        }

        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (CardDefinition def : pool) {
            currentWeight += def.getRngWeight();
            if (randomValue < currentWeight) {
                return def;
            }
        }

        return pool.get(pool.size() - 1); // Fallback
    }

    /**
     * Gets a random card definition matching the exact cost.
     * Often used for mechanics like Leap or Transformation.
     */
    public CardDefinition getRandomCardWithCost(Side side, CardType type, int targetCost) {
        List<CardDefinition> candidates = new ArrayList<>();
        for (CardDefinition def : allCards) {
            if (def.getSide() == side && def.getType() == type && def.getBaseCost() == targetCost) {
                candidates.add(def);
            }
        }
        return getRandomCard(candidates);
    }

    /**
     * Flexible Conjure filter. Pass null to ignore a filter parameter.
     */
    public List<CardDefinition> getFilteredCards(Side side, CardType type, HeroClass heroClass, Tribe tribe) {
        List<CardDefinition> candidates = new ArrayList<>();
        for (CardDefinition def : allCards) {
            if (side != null && def.getSide() != side) continue;
            if (type != null && def.getType() != type) continue;
            if (heroClass != null && def.getHeroClass() != heroClass) continue;
            if (tribe != null && (def.getTribes() == null || !def.getTribes().contains(tribe))) continue;

            candidates.add(def);
        }
        return candidates;
    }
}
