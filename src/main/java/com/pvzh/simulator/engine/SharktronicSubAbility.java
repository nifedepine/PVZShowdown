package com.pvzh.simulator.engine;

import com.pvzh.simulator.engine.events.DamageTakenEvent;
import com.pvzh.simulator.model.Card;
import com.pvzh.simulator.model.Side;

/**
 * Demonstrates a reactive ability using the EventManager Observer Pattern.
 * Sharktronic Sub: When a Plant takes damage, destroy it.
 */
public class SharktronicSubAbility {
    private final Card self;
    private final EventManager eventManager;

    public SharktronicSubAbility(Card self, EventManager eventManager) {
        this.self = self;
        this.eventManager = eventManager;
    }

    /**
     * Should be called when the card enters the board.
     */
    public void onEnterBoard() {
        eventManager.subscribe(self.getInstanceId(), DamageTakenEvent.class, this::onDamageTaken);
    }

    /**
     * Note: With the new EventManager.unsubscribeAll(subscriberId),
     * this explicit unsubscription is technically optional if handled globally on destruction,
     * but remains good practice.
     */
    public void onLeaveBoard() {
        eventManager.unsubscribe(DamageTakenEvent.class, this::onDamageTaken);
    }

    private void onDamageTaken(DamageTakenEvent event) {
        if (self.isMarkedForDestruction()) {
            return;
        }

        Card victim = event.getVictim();
        if (victim != null && victim.getOwner().getSide() == Side.PLANT) {
            victim.markForDestruction();
        }
    }
}
