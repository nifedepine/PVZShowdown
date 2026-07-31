package com.pvzh.simulator.engine;

import com.pvzh.simulator.engine.events.EntityTurnEndEvent;
import com.pvzh.simulator.engine.events.EntityTurnStartEvent;
import com.pvzh.simulator.engine.events.TurnEndEvent;
import com.pvzh.simulator.engine.events.TurnStartEvent;
import com.pvzh.simulator.engine.events.UnveilEvent;
import com.pvzh.simulator.model.Card;
import com.pvzh.simulator.model.CardState;
import com.pvzh.simulator.model.GameState;
import com.pvzh.simulator.model.Lane;
import com.pvzh.simulator.model.Phase;

import java.util.List;
import java.util.function.Consumer;

/**
 * Handles universal Left-to-Right, Zombie-First resolution for board events,
 * and integrates with the EventManager for pub/sub mechanics.
 */
public class EventDispatcher {
    private final GameState gameState;
    private final EventManager eventManager;

    public EventDispatcher(GameState gameState, EventManager eventManager) {
        this.gameState = gameState;
        this.eventManager = eventManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Optimized board sweep using zero-allocation arrays from the lists, preventing ConcurrentModificationExceptions
     * without creating full ArrayList copies.
     */
    public void sweepBoard(Consumer<Card> zombieAction, Consumer<Card> plantAction) {
        for (Lane lane : gameState.getLanes()) {
            if (zombieAction != null) {
                // Optimization: toArray avoids full ArrayList instantiation while still preventing CME.
                Card[] zombies = lane.getZombieFighters().toArray(new Card[0]);
                for (Card zombie : zombies) {
                    zombieAction.accept(zombie);
                }
            }
            if (plantAction != null) {
                Card[] plants = lane.getPlantFighters().toArray(new Card[0]);
                for (Card plant : plants) {
                    plantAction.accept(plant);
                }
            }
        }
    }

    public void sweepBoardAndHands(Consumer<Card> zombieAction, Consumer<Card> plantAction) {
        if (zombieAction != null) {
            Card[] zombieHand = gameState.getZombiePlayer().getHand().toArray(new Card[0]);
            for (Card zombieHandCard : zombieHand) {
                zombieAction.accept(zombieHandCard);
            }
        }
        if (plantAction != null) {
            Card[] plantHand = gameState.getPlantPlayer().getHand().toArray(new Card[0]);
            for (Card plantHandCard : plantHand) {
                plantAction.accept(plantHandCard);
            }
        }
        sweepBoard(zombieAction, plantAction);
    }

    public void unveilGravestones() {
        sweepBoard(
            zombie -> {
                if (zombie.getState() == CardState.GRAVESTONE) {
                    zombie.setState(CardState.REVEALED);
                    eventManager.publish(new UnveilEvent(zombie));
                }
            },
            null
        );
    }

    public void resolveDestructions() {
        sweepBoard(
            zombie -> {
                if (zombie.isMarkedForDestruction()) {
                    triggerWhenDestroyed(zombie);
                    removeFighterFromBoard(zombie);
                }
            },
            plant -> {
                if (plant.isMarkedForDestruction()) {
                    triggerWhenDestroyed(plant);
                    removeFighterFromBoard(plant);
                }
            }
        );
    }

    private void triggerWhenDestroyed(Card card) {
        // Here we would look up abilities triggered on destruction.
    }

    public void removeFighterFromBoard(Card card) {
        for (Lane lane : gameState.getLanes()) {
            lane.removeFighter(card);
        }
        // LEAK PREVENTION: Remove global board auras tied to this specific card instance.
        gameState.getGlobalModifierPipeline().removeModifiersBySource(card.getInstanceId());

        // LEAK PREVENTION: Unsubscribe any reactive abilities attached to this entity
        eventManager.unsubscribeAll(card.getInstanceId());
    }

    public void triggerPhaseStart(Phase phase, int turnNumber) {
        if (phase == Phase.ZOMBIE_PLAY) {
            gameState.getZombiePlayer().startTurnRamp();
            gameState.getPlantPlayer().startTurnRamp();

            eventManager.publish(new TurnStartEvent(turnNumber));
            sweepBoardAndHands(
                zombie -> eventManager.publish(new EntityTurnStartEvent(zombie, turnNumber)),
                plant -> eventManager.publish(new EntityTurnStartEvent(plant, turnNumber))
            );
        }
    }

    public void triggerPhaseEnd(Phase phase, int turnNumber) {
        if (phase == Phase.FIGHT) {
            sweepBoardAndHands(
                zombie -> eventManager.publish(new EntityTurnEndEvent(zombie, turnNumber)),
                plant -> eventManager.publish(new EntityTurnEndEvent(plant, turnNumber))
            );
            eventManager.publish(new TurnEndEvent(turnNumber));
        }
    }
}
