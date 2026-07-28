package com.pvzh.simulator.engine;

import com.pvzh.simulator.model.Card;
import com.pvzh.simulator.model.GameState;
import com.pvzh.simulator.model.TargetInfo;

/**
 * Functional interface for card effects (Tricks, Abilities).
 */
public interface CardEffect {
    /**
     * Executes the effect logic.
     * @param state The current game state.
     * @param source The card generating the effect.
     * @param target The target selected (can be null/NONE).
     */
    void apply(GameState state, Card source, TargetInfo target);
}
