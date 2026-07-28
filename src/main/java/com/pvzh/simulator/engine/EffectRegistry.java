package com.pvzh.simulator.engine;

import com.pvzh.simulator.model.CardSource;
import com.pvzh.simulator.model.TargetType;
import java.util.HashMap;
import java.util.Map;

/**
 * Decouples ability/trick logic from CardDefinitions.
 */
public class EffectRegistry {
    private final Map<String, CardEffect> registry = new HashMap<>();
    private final EventManager eventManager;

    public EffectRegistry(EventManager eventManager) {
        this.eventManager = eventManager;
        registerStandardEffects();
    }

    public void register(String effectId, CardEffect effect) {
        registry.put(effectId, effect);
    }

    public CardEffect getEffect(String effectId) {
        return registry.get(effectId);
    }

    private void registerStandardEffects() {
        // Deal 3 Damage to a fighter
        register("BERRY_BLAST_DAMAGE", (state, source, target) -> {
            if (target != null && target.getType() == TargetType.FIGHTER) {
                target.getTargetCard().takeDamage(3, source);
            } else if (target != null && target.getType() == TargetType.HERO) {
                target.getTargetHero().takeDamage(3, source);
            }
        });

        // Heal 4
        register("HEAL_4", (state, source, target) -> {
            if (target != null && target.getType() == TargetType.FIGHTER) {
                target.getTargetCard().heal(4);
            } else if (target != null && target.getType() == TargetType.HERO) {
                target.getTargetHero().heal(4);
            }
        });

        // Draw a Card
        register("DRAW_CARD", (state, source, target) -> {
            source.getOwner().drawCard(eventManager);
        });

        // Shuffle into deck (E.g. Clique Peas or Going Viral)
        register("SHUFFLE_INTO_DECK", (state, source, target) -> {
            // Re-inserts the definition into the deck and shuffles
            source.getOwner().getDeck().getCards().add(source.getDefinition());
            source.getOwner().getDeck().shuffle();
        });
    }
}
