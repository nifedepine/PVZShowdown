package com.pvzh.simulator.engine;

import com.pvzh.simulator.model.Card;
import com.pvzh.simulator.model.GameState;
import com.pvzh.simulator.model.Trait;
import com.pvzh.simulator.modifier.SimpleModifier;

/**
 * Example class demonstrating Aura Lifecycle constraints.
 */
public class AuraManager {

    /**
     * Board-Tied Aura (e.g. Forget-Me-Nuts or Pecanolith)
     * When the card leaves the board, removeFighterFromBoard will clear this modifier using its instanceId.
     */
    public static void applyBoardTiedAura(GameState state, Card source) {
        // E.g., Pecanolith gives ATTACK_WITH_HEALTH globally
        SimpleModifier aura = new SimpleModifier(source.getInstanceId(), 10, 0, Trait.ATTACK_WITH_HEALTH, 1);
        state.getGlobalModifierPipeline().addModifier(aura);
    }

    /**
     * Permanent Buff (e.g. Intergalactic Warlord)
     * Uses a detached string so when the Warlord dies, the buff remains.
     */
    public static void applyPermanentBuff(GameState state, Card source) {
        String detachedId = "PERM_BUFF_" + source.getInstanceId();
        // E.g., Warlord gives +1/+1 to all zombies globally
        SimpleModifier permBuff = new SimpleModifier(detachedId, 50, 1, null, 0);
        state.getGlobalModifierPipeline().addModifier(permBuff);
    }
}
