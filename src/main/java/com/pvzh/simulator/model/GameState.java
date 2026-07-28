package com.pvzh.simulator.model;

import com.pvzh.simulator.engine.PhaseManager;
import com.pvzh.simulator.modifier.ModifierPipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * The root state holder for the entire game.
 * Everything here represents the Server-Side Authority.
 */
public class GameState {
    private final Player plantPlayer;
    private final Player zombiePlayer;
    private final List<Lane> lanes;
    private final PhaseManager phaseManager;

    // Global modifiers affecting all entities or specific rules
    private final ModifierPipeline globalModifierPipeline = new ModifierPipeline();

    public GameState(Player plantPlayer, Player zombiePlayer) {
        this.plantPlayer = plantPlayer;
        this.zombiePlayer = zombiePlayer;
        this.phaseManager = new PhaseManager();
        this.lanes = new ArrayList<>(5);

        // Initialize default PvZH Board layout
        lanes.add(new Lane(1, LaneType.HEIGHTS));
        lanes.add(new Lane(2, LaneType.GROUND));
        lanes.add(new Lane(3, LaneType.GROUND));
        lanes.add(new Lane(4, LaneType.GROUND));
        lanes.add(new Lane(5, LaneType.WATER));
    }

    public Player getPlantPlayer() { return plantPlayer; }
    public Player getZombiePlayer() { return zombiePlayer; }
    public List<Lane> getLanes() { return lanes; }
    public PhaseManager getPhaseManager() { return phaseManager; }
    public ModifierPipeline getGlobalModifierPipeline() { return globalModifierPipeline; }

    public Lane getLane(int laneId) {
        if (laneId >= 1 && laneId <= 5) {
            return lanes.get(laneId - 1);
        }
        return null;
    }
}
