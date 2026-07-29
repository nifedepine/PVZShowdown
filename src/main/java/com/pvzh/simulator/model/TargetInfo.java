package com.pvzh.simulator.model;

/**
 * Encapsulates the target selected by the player or engine.
 */
public class TargetInfo {
    private final TargetType type;
    private final Card targetCard;
    private final Player targetHero;
    private final Lane targetLane;

    public TargetInfo(Card targetCard) {
        this.type = targetCard.getDefinition().getType() == CardType.FIGHTER ? TargetType.FIGHTER : TargetType.ENVIRONMENT;
        this.targetCard = targetCard;
        this.targetHero = null;
        this.targetLane = null;
    }

    public TargetInfo(Player targetHero) {
        this.type = TargetType.HERO;
        this.targetCard = null;
        this.targetHero = targetHero;
        this.targetLane = null;
    }

    public TargetInfo(Lane targetLane) {
        this.type = TargetType.LANE;
        this.targetCard = null;
        this.targetHero = null;
        this.targetLane = targetLane;
    }

    public TargetType getType() { return type; }
    public Card getTargetCard() { return targetCard; }
    public Player getTargetHero() { return targetHero; }
    public Lane getTargetLane() { return targetLane; }
}
