package com.pvzh.simulator.modifier;

import com.pvzh.simulator.model.Card;
import com.pvzh.simulator.model.GameState;
import com.pvzh.simulator.model.Lane;
import com.pvzh.simulator.model.Side;

/**
 * A positional dynamic modifier that grants bonus attack if the opposing lane slot is empty.
 */
public class AntiHeroModifier implements Modifier {
    private final String sourceId;
    private final Card cardRef;
    private final GameState gameState;
    private final int bonusValue;

    public AntiHeroModifier(String sourceId, Card cardRef, GameState gameState, int bonusValue) {
        this.sourceId = sourceId;
        this.cardRef = cardRef;
        this.gameState = gameState;
        this.bonusValue = bonusValue;
    }

    @Override
    public int apply(int currentValue) {
        if (isOpposingSlotEmpty()) {
            return currentValue + bonusValue;
        }
        return currentValue;
    }

    private boolean isOpposingSlotEmpty() {
        for (Lane lane : gameState.getLanes()) {
            Side mySide = cardRef.getOwner().getSide();
            if (mySide == Side.PLANT && lane.getPlantFighters().contains(cardRef)) {
                return lane.getZombieFighters().isEmpty();
            } else if (mySide == Side.ZOMBIE && lane.getZombieFighters().contains(cardRef)) {
                return lane.getPlantFighters().isEmpty();
            }
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }
}
