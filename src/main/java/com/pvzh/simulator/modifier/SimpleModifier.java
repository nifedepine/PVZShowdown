package com.pvzh.simulator.modifier;

import com.pvzh.simulator.model.Trait;

/**
 * A basic implementation of Modifier for stat buffs or persistent traits.
 */
public class SimpleModifier implements Modifier {
    private final String sourceId;
    private final int priority;
    private final int valueChange;
    private final Trait grantedTrait;
    private final int traitValue;
    private final ModifierScope scope;

    public SimpleModifier(String sourceId, int priority, int valueChange, Trait grantedTrait, int traitValue) {
        this(sourceId, priority, valueChange, grantedTrait, traitValue, ModifierScope.TEMPORARY);
    }

    public SimpleModifier(String sourceId, int priority, int valueChange, Trait grantedTrait, int traitValue, ModifierScope scope) {
        this.sourceId = sourceId;
        this.priority = priority;
        this.valueChange = valueChange;
        this.grantedTrait = grantedTrait;
        this.traitValue = traitValue;
        this.scope = scope;
    }

    @Override
    public int apply(int currentValue) {
        return currentValue + valueChange;
    }

    @Override
    public int getTraitValue(Trait trait) {
        if (this.grantedTrait == trait) {
            return traitValue;
        }
        return 0;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public ModifierScope getScope() {
        return scope;
    }
}
