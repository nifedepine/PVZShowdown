package com.pvzh.simulator.engine;

import com.pvzh.simulator.model.Card;
import com.pvzh.simulator.model.CardDefinition;
import com.pvzh.simulator.model.CardSource;
import com.pvzh.simulator.model.CardType;
import com.pvzh.simulator.model.GameState;
import com.pvzh.simulator.model.Lane;
import com.pvzh.simulator.model.Player;
import com.pvzh.simulator.model.Side;
import com.pvzh.simulator.model.Trait;
import com.pvzh.simulator.modifier.ModifierScope;
import com.pvzh.simulator.modifier.SimpleModifier;

import java.util.List;
import java.util.UUID;

/**
 * Manages complex board interactions like transformations, movement, bouncing, and leaps.
 */
public class BoardManager {
    private final GameState gameState;
    private final CardRegistry registry;
    private final EventManager eventManager;

    public BoardManager(GameState gameState, CardRegistry registry, EventManager eventManager) {
        this.gameState = gameState;
        this.registry = registry;
        this.eventManager = eventManager;
    }

    /**
     * Executes the Bounce mechanic. Removes the card from the board and returns it to the owner's hand.
     * Crucially resets states and clears non-persistent pipelines.
     */
    public void bounceCardToHand(Card card, Player targetOwner) {
        // Find and remove from lane
        boolean found = false;
        for (Lane lane : gameState.getLanes()) {
            if (lane.getPlantFighters().contains(card) || lane.getZombieFighters().contains(card)) {
                lane.removeFighter(card);
                found = true;
                break;
            }
        }

        if (!found) return;

        // Cleanup EventBus and Global Auras tied to this specific instance
        gameState.getGlobalModifierPipeline().removeModifiersBySource(card.getInstanceId());
        eventManager.unsubscribeAll(card.getInstanceId());

        // Reset Card State
        card.resetDamageTaken();
        card.setFrozen(false);
        // Note: the card object is NOT destroyed.

        // Clear all temporary (board-tied) modifiers, retaining only Persistent ones (like Reincarnation's internal buffs).
        card.getAttackPipeline().clearTemporaryModifiers();
        card.getHealthPipeline().clearTemporaryModifiers();
        card.getCostPipeline().clearTemporaryModifiers();
        card.getTraitPipeline().clearTemporaryModifiers();

        // Add to hand (BOUNCE source ignores standard hand size limits)
        targetOwner.addCardToHand(card, CardSource.BOUNCE);
    }

    public void moveFighter(Card card, int targetLaneId) {
        Lane currentLane = null;
        for (Lane lane : gameState.getLanes()) {
            if (lane.getPlantFighters().contains(card) || lane.getZombieFighters().contains(card)) {
                currentLane = lane;
                break;
            }
        }

        if (currentLane == null) {
            return;
        }

        Lane targetLane = gameState.getLane(targetLaneId);
        if (targetLane != null && targetLane.canPlayFighter(card)) {
            currentLane.removeFighter(card);
            targetLane.addFighter(card, null);
        }
    }

    public Card transformCard(Card oldCard, CardDefinition newDef) {
        Card newCard = new Card(newDef, oldCard.getOwner());

        if (oldCard.hasTrait(Trait.REINCARNATION)) {
            String modId = UUID.randomUUID().toString();
            newCard.getAttackPipeline().addModifier(new SimpleModifier(modId, 50, 1, null, 0, ModifierScope.PERSISTENT));
            newCard.getHealthPipeline().addModifier(new SimpleModifier(modId, 50, 1, null, 0, ModifierScope.PERSISTENT));
            newCard.getTraitPipeline().addModifier(new SimpleModifier(modId, 50, 0, Trait.REINCARNATION, 1, ModifierScope.PERSISTENT));
        } else if (oldCard.hasTrait(Trait.FIG_LEAP)) {
            String modId = UUID.randomUUID().toString();
            newCard.getTraitPipeline().addModifier(new SimpleModifier(modId, 50, 0, Trait.FIG_LEAP, 1, ModifierScope.PERSISTENT));
        }

        gameState.getGlobalModifierPipeline().removeModifiersBySource(oldCard.getInstanceId());
        eventManager.unsubscribeAll(oldCard.getInstanceId());

        boolean swappedInHand = swapInHand(oldCard, newCard);
        if (!swappedInHand) {
            swapInLane(oldCard, newCard);
        }

        return newCard;
    }

    public void leap(Card card) {
        int targetCost = card.getCost() + 1;
        CardDefinition newDef = registry.getRandomCardWithCost(card.getDefinition().getSide(), CardType.FIGHTER, targetCost);

        if (newDef != null) {
            transformCard(card, newDef);
        }
    }

    private boolean swapInHand(Card oldCard, Card newCard) {
        List<Card> hand = oldCard.getOwner().getHand();
        int index = hand.indexOf(oldCard);
        if (index != -1) {
            hand.set(index, newCard);
            return true;
        }
        return false;
    }

    private void swapInLane(Card oldCard, Card newCard) {
        for (Lane lane : gameState.getLanes()) {
            List<Card> fighters = newCard.getOwner().getSide() == Side.PLANT
                    ? lane.getPlantFighters()
                    : lane.getZombieFighters();

            int index = fighters.indexOf(oldCard);
            if (index != -1) {
                fighters.set(index, newCard);
                newCard.setGameStateContext(gameState);
                return;
            }
        }
    }
}
