package com.pvzh.simulator.modifier;

/**
 * Defines the lifecycle scope of a modifier.
 */
public enum ModifierScope {
    /** Removed when the card leaves the board (e.g. bounce, destruction). */
    TEMPORARY,
    /** Persists across state transitions like bouncing to hand (e.g. Reincarnation). */
    PERSISTENT
}
