package com.pvzh.simulator.model;

import com.pvzh.simulator.engine.EventManager;
import com.pvzh.simulator.engine.events.GameOverEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Player in the game.
 * Manages the Hero, their Hand, resources, and Deck interactions.
 */
public class Player {
    private final Side side;
    private final Hero hero;
    private Deck deck;

    private final List<Card> hand = new ArrayList<>();

    private int maxResources; // Suns for plants, Brains for zombies
    private int currentResources;

    public Player(Side side, Hero hero) {
        this.side = side;
        this.hero = hero;
        this.maxResources = 0;
        this.currentResources = 0;
    }

    public Side getSide() { return side; }
    public Hero getHero() { return hero; }
    public void setDeck(Deck deck) { this.deck = deck; }
    public Deck getDeck() { return deck; }
    public int getMaxResources() { return maxResources; }
    public void setMaxResources(int maxResources) { this.maxResources = maxResources; }
    public int getCurrentResources() { return currentResources; }
    public void setCurrentResources(int currentResources) { this.currentResources = currentResources; }
    public void spendResources(int amount) { this.currentResources -= amount; }
    public List<Card> getHand() { return hand; }

    public boolean addCardToHand(Card card, CardSource source) {
        if (source == CardSource.BOUNCE) {
            hand.add(card);
            return true;
        } else {
            if (hand.size() >= 10) {
                return false;
            } else {
                hand.add(card);
                return true;
            }
        }
    }

    public void drawCard(EventManager eventManager) {
        if (deck == null) return;
        CardDefinition def = deck.draw();
        if (def == null) {
            eventManager.publish(new GameOverEvent(this, "Deckout"));
            return;
        }
        Card card = new Card(def, this);
        addCardToHand(card, CardSource.DRAW);
    }

    public void takeDamage(int amount, Card attacker) {
        hero.takeDamage(amount, attacker, powerRewardDefinition -> {
            Card powerCard = new Card(powerRewardDefinition, this);
            addCardToHand(powerCard, CardSource.BLOCK_REWARD);
        });
    }

    public void heal(int amount) {
        hero.heal(amount);
    }
}
