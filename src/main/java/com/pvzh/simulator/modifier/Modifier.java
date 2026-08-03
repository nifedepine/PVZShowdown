package com.pvzh.simulator.modifier;

import com.pvzh.simulator.model.Trait;

/**
 * A modifier that can alter an attribute (like attack or health) dynamically,
 * or grant specific Traits (like FRENZY) with a numeric value.
 */
public interface Modifier {
    /**
     * Applies the modification to the current numeric value.
     */
    default int apply(int currentValue) {
        return currentValue;
    }

    /**
     * @return the value granted for this trait.
     */
    default int getTraitValue(Trait trait) {
        return 0;
    }

    /**
     * @return The priority of this modifier. Lower values execute earlier.
     */
    int getPriority();

    /**
     * @return A unique identifier or source for this modifier.
     */
    String getSourceId();

    /**
     * @return The scope defining if this modifier survives a board-to-hand transition.
     */
    default ModifierScope getScope() {
        return ModifierScope.TEMPORARY;
    }
}
