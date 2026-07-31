package com.pvzh.simulator.model;

/**
 * A perfectly frozen snapshot of a Card's state at a specific millisecond.
 * Crucial for simultaneous combat resolution logic.
 * OPTIMIZATION: Uses a primitive array mapping Trait ordinals instead of a HashMap to minimize Garbage Collection overhead.
 */
public final class CardSnapshot {
    private final Card cardRef;
    private final Side side;
    private final int currentHealth;
    private final int attack;
    private final int cost;

    // Array size equal to number of enum constants in Trait. Highly performant.
    private final int[] traitValues = new int[Trait.values().length];

    public CardSnapshot(Card card) {
        this.cardRef = card;
        this.side = card.getOwner().getSide();
        this.currentHealth = card.getCurrentHealth();
        this.attack = card.getAttack();
        this.cost = card.getCost();

        // Capture all active traits dynamically with zero boxing overhead
        for (Trait trait : Trait.values()) {
            int val = card.getTraitValue(trait);
            if (val > 0) {
                this.traitValues[trait.ordinal()] = val;
            }
        }
    }

    public Card getCardRef() { return cardRef; }
    public Side getSide() { return side; }
    public int getCurrentHealth() { return currentHealth; }
    public int getAttack() { return attack; }
    public int getCost() { return cost; }

    public int getTraitValue(Trait trait) {
        return traitValues[trait.ordinal()];
    }

    public boolean hasTrait(Trait trait) {
        return traitValues[trait.ordinal()] > 0;
    }
}
