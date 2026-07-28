package com.pvzh.simulator.engine;

import com.pvzh.simulator.model.Card;
import com.pvzh.simulator.model.CardType;
import com.pvzh.simulator.model.Phase;
import com.pvzh.simulator.model.Side;
import com.pvzh.simulator.model.TargetInfo;
import com.pvzh.simulator.model.TargetType;
import com.pvzh.simulator.model.Trait;

/**
 * Validates phase enforcement for playing cards and targeting constraints.
 */
public class TargetValidator {

    public static boolean canPlayInPhase(Card card, Phase currentPhase) {
        Side side = card.getOwner().getSide();
        CardType type = card.getDefinition().getType();

        if (side == Side.ZOMBIE) {
            if (type == CardType.TRICK) {
                return currentPhase == Phase.ZOMBIE_TRICKS;
            } else {
                return currentPhase == Phase.ZOMBIE_PLAY;
            }
        } else { // PLANT
            return currentPhase == Phase.PLANT_PLAY;
        }
    }

    public static boolean isValidTarget(Card source, TargetInfo target) {
        if (target.getType() != TargetType.FIGHTER || target.getTargetCard() == null) {
            return true;
        }

        Card targetCard = target.getTargetCard();
        boolean isOpponent = source.getOwner().getSide() != targetCard.getOwner().getSide();

        if (targetCard.hasTrait(Trait.UNTRICKABLE) && isOpponent) {
            if (source.getDefinition().getType() == CardType.TRICK) {
                return false;
            }
        }
        return true;
    }
}
