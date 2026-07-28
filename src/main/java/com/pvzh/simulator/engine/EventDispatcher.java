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

import java.util.ArrayList;
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

    public void sweepBoard(Consumer<Card> zombieAction, Consumer<Card> plantAction) {
        for (Lane lane : gameState.getLanes()) {
            if (zombieAction != null) {
                for (Card zombie : new ArrayList<>(lane.getZombieFighters())) {
                    zombieAction.accept(zombie);
                }
            }
            if (plantAction != null) {
                for (Card plant : new ArrayList<>(lane.getPlantFighters())) {
                    plantAction.accept(plant);
                }
            }
        }
    }

    public void sweepBoardAndHands(Consumer<Card> zombieAction, Consumer<Card> plantAction) {
        if (zombieAction != null) {
            for (Card zombieHandCard : new ArrayList<>(gameState.getZombiePlayer().getHand())) {
                zombieAction.accept(zombieHandCard);
            }
        }
        if (plantAction != null) {
            for (Card plantHandCard : new ArrayList<>(gameState.getPlantPlayer().getHand())) {
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

    private void removeFighterFromBoard(Card card) {
        for (Lane lane : gameState.getLanes()) {
            lane.removeFighter(card);
        }
        // CRITICAL AURA LIFECYCLE: Remove any global board auras tied to this specific card instance.
        gameState.getGlobalModifierPipeline().removeModifiersBySource(card.getInstanceId());

        // Also remove any modifiers it injected into the Player directly (e.g. Brainy/Suns modifiers)
        // This is handled by ensuring they share the same pipeline or calling remove on the player's pipeline.
    }

    public void triggerPhaseStart(Phase phase, int turnNumber) {
        if (phase == Phase.ZOMBIE_PLAY) {
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
