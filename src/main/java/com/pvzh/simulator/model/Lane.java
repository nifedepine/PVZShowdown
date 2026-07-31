package com.pvzh.simulator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Represents a single lane on the board.
 * Tracks the type of lane (Heights, Ground, Water) and the entities residing in it.
 */
public class Lane {
    private final int id; // 1 to 5
    private final LaneType type;

    // Support Universal Team-Up: Maximum 2 fighters per side.
    private final List<Card> plantFighters = new ArrayList<>(2);
    private final List<Card> zombieFighters = new ArrayList<>(2);

    private Card environment;

    public Lane(int id, LaneType type) {
        this.id = id;
        this.type = type;
    }

    public int getId() { return id; }
    public LaneType getType() { return type; }
    public List<Card> getPlantFighters() { return plantFighters; }
    public List<Card> getZombieFighters() { return zombieFighters; }
    public Card getEnvironment() { return environment; }
    public void setEnvironment(Card environment) { this.environment = environment; }

    public void removeFighter(Card fighter) {
        plantFighters.remove(fighter);
        zombieFighters.remove(fighter);
    }

    /**
     * Adds a fighter to the lane. Handles sacrifice logic if overlapping for Evolution/Fusion.
     * @param fighter The card being played.
     * @param targetOverlap The specific allied card being sacrificed/overlapped. Can be null if standard play.
     */
    public void addFighter(Card fighter, Card targetOverlap) {
        if (!canPlayFighter(fighter, targetOverlap)) {
            throw new IllegalArgumentException("Cannot play fighter in this lane under given rules.");
        }

        List<Card> laneFighters = fighter.getOwner().getSide() == Side.PLANT ? plantFighters : zombieFighters;

        // Consume overlap target if provided
        if (targetOverlap != null && laneFighters.contains(targetOverlap)) {
            // Usually, FUSION/EVOLUTION targets are destroyed or consumed here.
            // For simplicity, we directly remove it from the lane list to make room.
            laneFighters.remove(targetOverlap);
            targetOverlap.markForDestruction(); // To trigger any leaving board effects later
        }

        laneFighters.add(fighter);
    }

    /**
     * Helper overloaded method for standard placements without a specific overlap target.
     */
    public boolean canPlayFighter(Card card) {
        return canPlayFighter(card, null);
    }

    /**
     * Checks if a card can be placed in this lane based on lane type, Team-Up, Evolution, and Fusion rules.
     */
    public boolean canPlayFighter(Card card, Card targetOverlap) {
        if (card.getDefinition().getType() != CardType.FIGHTER) {
            return false;
        }

        List<Card> laneFighters = card.getOwner().getSide() == Side.PLANT ? plantFighters : zombieFighters;

        // Check Amphibious restriction for Water lane
        if (type == LaneType.WATER && !card.hasTrait(Trait.AMPHIBIOUS)) {
            return false;
        }

        // If a specific overlap target is chosen, ensure the mechanic is valid
        if (targetOverlap != null && laneFighters.contains(targetOverlap)) {
            return canPlayViaOverlap(card, targetOverlap);
        }

        // Standard placement limits
        if (laneFighters.size() >= 2) {
            return false; // Lane is physically full and no valid overlap target was provided
        }

        if (laneFighters.size() == 1) {
            // Only valid if using Team-Up
            Card existingFighter = laneFighters.get(0);
            return card.hasTrait(Trait.TEAM_UP) || existingFighter.hasTrait(Trait.TEAM_UP);
        }

        return true;
    }

    /**
     * Checks if the card can be played ON TOP of an existing allied fighter via Evolution or Fusion.
     */
    private boolean canPlayViaOverlap(Card incomingCard, Card existing) {
        // Target has FUSION -> incoming card can be played on it
        if (existing.hasTrait(Trait.FUSION)) {
            return true;
        }

        // Incoming card has EVOLUTION -> can be played on it if tribes match
        if (incomingCard.hasTrait(Trait.EVOLUTION)) {
            Set<Tribe> incomingTribes = incomingCard.getDefinition().getTribes();
            Set<Tribe> existingTribes = existing.getDefinition().getTribes();

            if (incomingTribes != null && existingTribes != null && !Collections.disjoint(incomingTribes, existingTribes)) {
                return true;
            }
        }
        return false;
    }
}
