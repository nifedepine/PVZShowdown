package com.pvzh.simulator.modifier;

import com.pvzh.simulator.model.Trait;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a pipeline of modifiers that dynamically compute an attribute's final value.
 */
public class ModifierPipeline {
    private final List<Modifier> modifiers = new ArrayList<>();

    public void addModifier(Modifier modifier) {
        modifiers.add(modifier);
        modifiers.sort(Comparator.comparingInt(Modifier::getPriority));
    }

    public void removeModifiersBySource(String sourceId) {
        modifiers.removeIf(m -> m.getSourceId().equals(sourceId));
    }

    /**
     * Clears all modifiers that are not marked as PERSISTENT.
     * Used during the Bounce mechanic.
     */
    public void clearTemporaryModifiers() {
        modifiers.removeIf(m -> m.getScope() != ModifierScope.PERSISTENT);
    }

    public int compute(int baseValue) {
        int currentValue = baseValue;
        for (Modifier modifier : modifiers) {
            currentValue = modifier.apply(currentValue);
        }
        return currentValue;
    }

    public int getTraitValue(Trait trait) {
        int value = 0;
        for (Modifier modifier : modifiers) {
            value += modifier.getTraitValue(trait);
        }
        return value;
    }
}
