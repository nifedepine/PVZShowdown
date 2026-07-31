package com.pvzh.simulator.engine;

import com.pvzh.simulator.engine.events.BonusAttackEvent;
import com.pvzh.simulator.model.Card;
import com.pvzh.simulator.model.CardSnapshot;
import com.pvzh.simulator.model.GameState;
import com.pvzh.simulator.model.Lane;
import com.pvzh.simulator.model.Side;
import com.pvzh.simulator.model.Trait;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the generalized combat loop, simultaneous strike snapshotting, and bonus attacks.
 */
public class CombatResolver {
    private final GameState gameState;
    private final EventManager eventManager;
    private final EventDispatcher eventDispatcher;
    private final GameLogger logger;

    public CombatResolver(GameState gameState, EventManager eventManager, EventDispatcher eventDispatcher, GameLogger logger) {
        this.gameState = gameState;
        this.eventManager = eventManager;
        this.eventDispatcher = eventDispatcher;
        this.logger = logger;
    }

    /**
     * Executes the standard Fight Phase for a specific lane.
     * Fully supports Team-Up by looping over all fighters in the lane.
     */
    public void resolveLaneCombat(Lane lane) {
        logger.logDebug("Resolving combat for Lane " + lane.getId());

        // Step 1: Pre-Combat Environments
        resolvePreCombatEnvironments(lane);
        eventDispatcher.resolveDestructions();

        List<Card> zombies = new ArrayList<>(lane.getZombieFighters());
        List<Card> plants = new ArrayList<>(lane.getPlantFighters());

        // Step 2: Pre-Combat Overshoot (Zombies First)
        for (Card zombie : zombies) {
            if (zombie.hasTrait(Trait.OVERSHOOT) && !zombie.isMarkedForDestruction()) {
                int overshootDmg = zombie.getTraitValue(Trait.OVERSHOOT);
                gameState.getPlantPlayer().takeDamage(overshootDmg, zombie);
            }
        }

        // Step 3: Pre-Combat Overshoot (Plants)
        for (Card plant : plants) {
            if (plant.hasTrait(Trait.OVERSHOOT) && !plant.isMarkedForDestruction()) {
                int overshootDmg = plant.getTraitValue(Trait.OVERSHOOT);
                gameState.getZombiePlayer().takeDamage(overshootDmg, plant);
            }
        }

        // We execute combat sequentially front-to-back if there are multiple team-up cards.
        // For PvP simulator purposes, we match Zombie[0] vs Plant[0], and Zombie[1] vs Plant[1], etc.
        int maxFights = Math.max(zombies.size(), plants.size());

        for (int i = 0; i < maxFights; i++) {
            Card zombie = i < zombies.size() ? zombies.get(i) : null;
            Card plant = i < plants.size() ? plants.get(i) : null;

            // Step 4: Snapshot Generation (Only for survivors)
            CardSnapshot zombieSnap = (zombie != null && !zombie.isMarkedForDestruction()) ? new CardSnapshot(zombie) : null;
            CardSnapshot plantSnap = (plant != null && !plant.isMarkedForDestruction()) ? new CardSnapshot(plant) : null;

            List<Card> queuedBonusAttacks = new ArrayList<>();

            // Step 5: Simultaneous Combat Strikes
            if (zombieSnap != null) {
                executeStrike(zombieSnap, plantSnap, lane.getId(), false, queuedBonusAttacks);
            }
            if (plantSnap != null) {
                executeStrike(plantSnap, zombieSnap, lane.getId(), false, queuedBonusAttacks);
            }

            // Step 6: Lane Cleanup (Resolve Destructions per pairing before moving to backline)
            eventDispatcher.resolveDestructions();

            for (Card attacker : queuedBonusAttacks) {
                performBonusAttack(attacker);
            }
        }
    }

    private void resolvePreCombatEnvironments(Lane lane) {
        // Placeholder for environment logic
    }

    public void performBonusAttack(Card attacker) {
        if (attacker.isMarkedForDestruction()) return;

        Side opponentSide = attacker.getOwner().getSide() == Side.PLANT ? Side.ZOMBIE : Side.PLANT;
        if (hasAntiBonusAttackAura(opponentSide)) {
            logger.logDebug("Bonus attack by " + attacker.getDefinition().getName() + " was blocked by Anti-Bonus Attack.");
            return;
        }

        logger.logDebug(attacker.getDefinition().getName() + " performs a Bonus Attack!");
        eventManager.publish(new BonusAttackEvent(attacker));

        Lane attackerLane = findLane(attacker);
        if (attackerLane == null) return;

        CardSnapshot attackerSnap = new CardSnapshot(attacker);

        List<Card> defenders = opponentSide == Side.PLANT ? attackerLane.getPlantFighters() : attackerLane.getZombieFighters();
        Card defender = defenders.isEmpty() ? null : defenders.get(0);
        CardSnapshot defenderSnap = defender != null ? new CardSnapshot(defender) : null;

        List<Card> nestedBonusAttacks = new ArrayList<>();

        executeStrike(attackerSnap, defenderSnap, attackerLane.getId(), true, nestedBonusAttacks);

        eventDispatcher.resolveDestructions();

        for (Card nestedAttacker : nestedBonusAttacks) {
            performBonusAttack(nestedAttacker);
        }
    }

    private void executeStrike(CardSnapshot attackerSnap, CardSnapshot defenderSnap, int laneId, boolean isBonusAttack, List<Card> queuedBonusAttacks) {
        Card attackerRef = attackerSnap.getCardRef();

        if (attackerRef.isFrozen()) {
            attackerRef.setFrozen(false);
            logger.logDebug(attackerSnap.getCardRef().getDefinition().getName() + " is frozen and cannot attack!");
            return;
        }

        int attackDamage = attackerSnap.hasTrait(Trait.ATTACK_WITH_HEALTH) ? attackerSnap.getCurrentHealth() : attackerSnap.getAttack();

        if (attackDamage <= 0) return;

        boolean isStrikethrough = attackerSnap.hasTrait(Trait.STRIKETHROUGH);
        boolean defenderDestroyed = false;

        // Strike Defender
        if (defenderSnap != null) {
            Card defenderRef = defenderSnap.getCardRef();
            defenderRef.takeDamage(attackDamage, attackerRef);

            if (defenderRef.isMarkedForDestruction()) {
                defenderDestroyed = true;
            }
        }

        // Strike Hero
        if (defenderSnap == null || isStrikethrough) {
            if (attackerSnap.getSide() == Side.ZOMBIE) {
                gameState.getPlantPlayer().takeDamage(attackDamage, attackerRef);
            } else {
                gameState.getZombiePlayer().takeDamage(attackDamage, attackerRef);
            }
        }

        // Splash Damage
        int splash = attackerSnap.getTraitValue(Trait.SPLASH_DAMAGE);
        if (splash > 0) {
            applySplashDamage(attackerSnap.getSide(), laneId, splash, attackerRef);
        }

        // Queue Post-Strike Traits (Frenzy, Double Strike)
        if (attackerSnap.hasTrait(Trait.DOUBLE_STRIKE) && !isBonusAttack && !attackerRef.isMarkedForDestruction()) {
             queuedBonusAttacks.add(attackerRef);
        }

        if (attackerSnap.hasTrait(Trait.FRENZY) && defenderDestroyed && !attackerRef.isMarkedForDestruction()) {
             queuedBonusAttacks.add(attackerRef);
        }
    }

    private void applySplashDamage(Side attackerSide, int centerLaneId, int damage, Card attacker) {
        Side opponentSide = attackerSide == Side.PLANT ? Side.ZOMBIE : Side.PLANT;

        int leftLane = centerLaneId - 1;
        int rightLane = centerLaneId + 1;

        if (leftLane >= 1) {
            Lane lane = gameState.getLane(leftLane);
            List<Card> defenders = opponentSide == Side.PLANT ? lane.getPlantFighters() : lane.getZombieFighters();
            for (Card def : defenders) {
                def.takeDamage(damage, attacker);
            }
        }

        if (rightLane <= 5) {
            Lane lane = gameState.getLane(rightLane);
            List<Card> defenders = opponentSide == Side.PLANT ? lane.getPlantFighters() : lane.getZombieFighters();
            for (Card def : defenders) {
                def.takeDamage(damage, attacker);
            }
        }
    }

    private boolean hasAntiBonusAttackAura(Side side) {
        for (Lane lane : gameState.getLanes()) {
            List<Card> fighters = side == Side.PLANT ? lane.getPlantFighters() : lane.getZombieFighters();
            for (Card card : fighters) {
                if (card.hasTrait(Trait.ANTI_BONUS_ATTACK)) return true;
            }
        }
        return false;
    }

    private Lane findLane(Card card) {
        for (Lane lane : gameState.getLanes()) {
            if (lane.getPlantFighters().contains(card) || lane.getZombieFighters().contains(card)) {
                return lane;
            }
        }
        return null;
    }
}
