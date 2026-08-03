package com.pvzh.simulator.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Blueprint for a card, intended to be loaded from external JSON files.
 * This ensures strict Data Decoupling.
 */
public class CardDefinition {
    private String id;
    private String name;

    // Strict Card Categorization
    private Side side; // PLANT or ZOMBIE
    private CardType type; // FIGHTER, TRICK, ENVIRONMENT
    private Set<Tribe> tribes; // E.g., PEA, GARGANTUAR
    private HeroClass heroClass; // The class this card belongs to

    private int baseCost;
    private int baseAttack; // 0 for tricks/environments
    private int baseHealth; // 0 for tricks/environments

    // Parameterized traits (e.g., ARMORED -> 2)
    private Map<Trait, Integer> traits;
    private List<String> abilities; // IDs referencing ability logic scripts

    // Moddable weighted RNG for "Conjure" selection.
    // Defaults to 100 for perfectly uniform vanilla probability.
    private int rngWeight = 100;

    public CardDefinition() {
    }

    public CardDefinition(String id, String name, Side side, CardType type, Set<Tribe> tribes, HeroClass heroClass,
                          int baseCost, int baseAttack, int baseHealth, Map<Trait, Integer> traits, List<String> abilities) {
        this.id = id;
        this.name = name;
        this.side = side;
        this.type = type;
        this.tribes = tribes;
        this.heroClass = heroClass;
        this.baseCost = baseCost;
        this.baseAttack = baseAttack;
        this.baseHealth = baseHealth;
        this.traits = traits != null ? traits : new HashMap<>();
        this.abilities = abilities;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Side getSide() { return side; }
    public CardType getType() { return type; }
    public Set<Tribe> getTribes() { return tribes; }
    public HeroClass getHeroClass() { return heroClass; }

    public int getBaseCost() { return baseCost; }
    public int getBaseAttack() { return baseAttack; }
    public int getBaseHealth() { return baseHealth; }
    public Map<Trait, Integer> getTraits() { return traits; }
    public List<String> getAbilities() { return abilities; }

    public int getRngWeight() { return rngWeight; }
    public void setRngWeight(int rngWeight) { this.rngWeight = rngWeight; }
}
