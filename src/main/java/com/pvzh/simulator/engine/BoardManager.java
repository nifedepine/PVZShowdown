package com.pvzh.simulator.engine;

import com.pvzh.simulator.model.Card;
import com.pvzh.simulator.model.CardDefinition;
import com.pvzh.simulator.model.CardType;
import com.pvzh.simulator.model.GameState;
import com.pvzh.simulator.model.Lane;
import com.pvzh.simulator.model.Side;
import com.pvzh.simulator.model.Trait;
import com.pvzh.simulator.modifier.SimpleModifier;

import java.util.List;
import java.util.UUID;

/**
 * Manages complex board interactions like transformations, movement, and leaps.
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
     * Moves a fighter from its current lane to a target lane.
     */
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
        // During movement, if it's just a move, we assume no overlap sacrifice is occurring.
        if (targetLane != null && targetLane.canPlayFighter(card)) {
            currentLane.removeFighter(card);
            targetLane.addFighter(card, null);
        }
    }

    /**
     * Silently replaces a card in its exact current location (Hand or Lane).
     * Cleans up all old subscriptions to prevent memory leaks and retains persistent traits.
     */
    public Card transformCard(Card oldCard, CardDefinition newDef) {
        Card newCard = new Card(newDef, oldCard.getOwner());

        // Handle persistent transformation traits (e.g., Reincarnation, Fig)
        if (oldCard.hasTrait(Trait.REINCARNATION)) {
            String modId = UUID.randomUUID().toString();
            newCard.getAttackPipeline().addModifier(new SimpleModifier(modId, 50, 1, null, 0));
            newCard.getHealthPipeline().addModifier(new SimpleModifier(modId, 50, 1, null, 0));
            newCard.getTraitPipeline().addModifier(new SimpleModifier(modId, 50, 0, Trait.REINCARNATION, 1));
        } else if (oldCard.hasTrait(Trait.FIG_LEAP)) {
            String modId = UUID.randomUUID().toString();
            newCard.getTraitPipeline().addModifier(new SimpleModifier(modId, 50, 0, Trait.FIG_LEAP, 1));
        }

        // LEAK PREVENTION: Remove global auras and unsubscribe old card
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
                // Also wire newCard to gameState
                newCard.setGameStateContext(gameState);
                return;
            }
        }
    }
}
