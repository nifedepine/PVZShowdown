package com.pvzh.simulator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Represents a Player's Hero, their health, block meter, and superpower pool.
 */
public class Hero {
    private final Side side;
    private final List<HeroClass> classes;
    private int maxHealth;
    private int currentHealth;
    private int blockMeter;

    // Moddable Superblock Limit
    private int maxBlocks = 3;
    private int blocksRemaining = 3;

    // The pool of superpowers available to be blocked/drawn (usually 4 unique cards)
    private final List<CardDefinition> superpowerPool;

    private final Random random = new Random();

    public Hero(Side side, List<HeroClass> classes, List<CardDefinition> superpowerPool) {
        this.side = side;
        this.classes = new ArrayList<>(classes);
        this.maxHealth = 20;
        this.currentHealth = 20;
        this.blockMeter = 0;
        this.superpowerPool = new ArrayList<>(superpowerPool);
    }

    public Side getSide() { return side; }
    public List<HeroClass> getClasses() { return classes; }
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    public int getCurrentHealth() { return currentHealth; }
    public int getBlockMeter() { return blockMeter; }

    public int getMaxBlocks() { return maxBlocks; }
    public void setMaxBlocks(int maxBlocks) {
        this.maxBlocks = maxBlocks;
        this.blocksRemaining = maxBlocks;
    }
    public int getBlocksRemaining() { return blocksRemaining; }

    public void heal(int amount) {
        if (amount > 0) {
            this.currentHealth = Math.min(this.maxHealth, this.currentHealth + amount);
        }
    }

    /**
     * Applies damage to the hero, processing Block Meter RNG, block limits, and Superpower rewards.
     * @param amount The amount of incoming damage.
     * @param attacker The card dealing the damage (determines Bullseye). Nullable.
     * @param blockRewardCallback A callback invoked if the hero successfully blocks.
     */
    public void takeDamage(int amount, Card attacker, Consumer<CardDefinition> blockRewardCallback) {
        if (amount <= 0) {
            return;
        }

        boolean hasBullseye = attacker != null && attacker.hasTrait(Trait.BULLSEYE);

        // Bullseye or 0 blocks remaining -> Damage bypasses block meter completely.
        if (hasBullseye || blocksRemaining <= 0) {
            this.currentHealth -= amount;
            return;
        }

        // Generate RNG for the Block Meter (1, 2, or 3 charges)
        int charges = random.nextInt(3) + 1;
        this.blockMeter += charges;

        // Check for Block
        if (this.blockMeter >= 8) {
            // Damage is immediately cancelled
            this.blockMeter = 0; // Reset meter
            this.blocksRemaining--; // Decrement available blocks

            // Give a random superpower from the pool, if any remain
            if (!superpowerPool.isEmpty()) {
                int powerIndex = random.nextInt(superpowerPool.size());
                CardDefinition powerReward = superpowerPool.remove(powerIndex);

                if (blockRewardCallback != null) {
                    blockRewardCallback.accept(powerReward);
                }
            }
        } else {
            // If no Block occurred, apply the damage
            this.currentHealth -= amount;
        }
    }
}
