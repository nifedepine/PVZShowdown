package com.pvzh.simulator.engine;

import com.pvzh.simulator.model.CardDefinition;
import com.pvzh.simulator.model.Deck;
import com.pvzh.simulator.model.Hero;

import java.util.HashMap;
import java.util.Map;

/**
 * Validates a Deck against the strict PvZH deckbuilding rules.
 */
public class DeckValidator {

    private boolean isStrictValidationEnabled = true;

    public DeckValidator(boolean isStrictValidationEnabled) {
        this.isStrictValidationEnabled = isStrictValidationEnabled;
    }

    public boolean isStrictValidationEnabled() {
        return isStrictValidationEnabled;
    }

    public void setStrictValidationEnabled(boolean strictValidationEnabled) {
        isStrictValidationEnabled = strictValidationEnabled;
    }

    /**
     * Validates the provided deck for the given hero.
     * Throws a DeckValidationException if any rule is violated and strict mode is enabled.
     *
     * @param deck The deck to validate.
     * @param hero The hero who owns the deck.
     * @throws DeckValidationException if validation fails.
     */
    public void validate(Deck deck, Hero hero) throws DeckValidationException {
        if (!isStrictValidationEnabled) {
            return; // Sandbox mode bypasses all rules
        }

        // Rule 1: Size Rule
        if (deck.size() != 40) {
            throw new DeckValidationException("Deck must contain exactly 40 cards. Found: " + deck.size());
        }

        Map<String, Integer> cardCounts = new HashMap<>();

        for (CardDefinition card : deck.getCards()) {
            // Rule 3: Side Rule
            if (card.getSide() != hero.getSide()) {
                throw new DeckValidationException("Card side (" + card.getSide() +
                        ") does not match Hero side (" + hero.getSide() + ") for card: " + card.getName());
            }

            // Rule 4: Class Rule
            if (!hero.getClasses().contains(card.getHeroClass())) {
                throw new DeckValidationException("Card class (" + card.getHeroClass() +
                        ") does not match any of the Hero's classes for card: " + card.getName());
            }

            // Rule 2: Duplicate Rule
            String cardId = card.getId();
            int count = cardCounts.getOrDefault(cardId, 0) + 1;
            if (count > 4) {
                throw new DeckValidationException("Deck cannot contain more than 4 copies of a card. Violator: " + card.getName());
            }
            cardCounts.put(cardId, count);
        }
    }
}
