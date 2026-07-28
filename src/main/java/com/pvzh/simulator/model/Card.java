package com.pvzh.simulator.model;

import com.pvzh.simulator.modifier.ModifierPipeline;

import java.util.UUID;

/**
 * An active instance of a card on the board or in a hand.
 * Computes its attributes dynamically via Modifier Pipelines.
 */
public class Card {
    private final String instanceId; // Unique ID for this instance
    private final CardDefinition definition; // Static blueprint
    private final Player owner;

    // State management
    private CardState state = CardState.REVEALED;
    private boolean isFrozen = false;

    // Optional link to GameState for resolving global auras on this card
    private GameState gameStateContext;

    // We maintain a damage counter instead of mutating base health.
    // Current health = (BaseHealth modified) - damageTaken
    private int damageTaken = 0;

    // Safety flag for destruction logic queueing
    private boolean isMarkedForDestruction = false;

    // Modifier pipelines for dynamic attributes
    private final ModifierPipeline attackPipeline = new ModifierPipeline();
    private final ModifierPipeline healthPipeline = new ModifierPipeline();
    private final ModifierPipeline costPipeline = new ModifierPipeline();
    private final ModifierPipeline traitPipeline = new ModifierPipeline();

    public Card(CardDefinition definition, Player owner) {
        this.instanceId = UUID.randomUUID().toString();
        this.definition = definition;
        this.owner = owner;
    }

    public void setGameStateContext(GameState state) {
        this.gameStateContext = state;
    }

    public String getInstanceId() { return instanceId; }
    public CardDefinition getDefinition() { return definition; }
    public Player getOwner() { return owner; }
    public CardState getState() { return state; }
    public void setState(CardState state) { this.state = state; }
    public boolean isFrozen() { return isFrozen; }
    public void setFrozen(boolean frozen) { this.isFrozen = frozen; }

    public int getCost() { return Math.max(0, costPipeline.compute(definition.getBaseCost())); }
    public int getAttack() { return Math.max(0, attackPipeline.compute(definition.getBaseAttack())); }
    public int getMaxHealth() { return Math.max(1, healthPipeline.compute(definition.getBaseHealth())); }
    public int getCurrentHealth() { return Math.max(0, getMaxHealth() - damageTaken); }

    public void takeDamage(int amount, Card attacker) {
        if (amount <= 0 || isMarkedForDestruction) return;
        if (this.state == CardState.GRAVESTONE) return;
        if (hasTrait(Trait.INVULNERABLE)) return;

        int armor = getTraitValue(Trait.ARMORED);
        int finalDamage = Math.max(0, amount - armor);

        if (finalDamage > 0) {
            this.damageTaken += finalDamage;

            // NOTE: Disptaching DamageTakenEvent should be handled by the combat/event engine in real play,
            // but we register the state change here.

            if (attacker != null && attacker.hasTrait(Trait.DEADLY)) {
                this.isMarkedForDestruction = true;
            }
            if (getCurrentHealth() <= 0) {
                this.isMarkedForDestruction = true;
            }
        }
    }

    public void heal(int amount) {
        if (amount > 0) {
            this.damageTaken = Math.max(0, this.damageTaken - amount);
        }
    }

    public boolean isMarkedForDestruction() { return isMarkedForDestruction; }
    public void markForDestruction() { this.isMarkedForDestruction = true; }

    /**
     * Dynamically determines the active value of a specific trait.
     * Combines the static blueprint value, dynamic card modifiers, and global auras.
     */
    public int getTraitValue(Trait trait) {
        int baseValue = 0;
        if (definition.getTraits() != null && definition.getTraits().containsKey(trait)) {
            baseValue = definition.getTraits().get(trait);
        }

        int cardValue = baseValue + traitPipeline.getTraitValue(trait);

        int globalValue = 0;
        if (gameStateContext != null) {
            globalValue = gameStateContext.getGlobalModifierPipeline().getTraitValue(trait);
        }

        return cardValue + globalValue;
    }

    public boolean hasTrait(Trait trait) {
        return getTraitValue(trait) > 0;
    }

    public ModifierPipeline getAttackPipeline() { return attackPipeline; }
    public ModifierPipeline getHealthPipeline() { return healthPipeline; }
    public ModifierPipeline getCostPipeline() { return costPipeline; }
    public ModifierPipeline getTraitPipeline() { return traitPipeline; }
}
