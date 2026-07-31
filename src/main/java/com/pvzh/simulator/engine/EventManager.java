package com.pvzh.simulator.engine;

import com.pvzh.simulator.engine.events.GameEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A central Event Bus (Observer Pattern) for the game engine.
 * Allows cards and abilities to subscribe to specific game events.
 * Implements strict lifecycle tracking via subscriberId to prevent memory leaks.
 */
public class EventManager {

    private static class Subscription<T extends GameEvent> {
        final String subscriberId;
        final Consumer<T> listener;

        Subscription(String subscriberId, Consumer<T> listener) {
            this.subscriberId = subscriberId;
            this.listener = listener;
        }
    }

    private final Map<Class<? extends GameEvent>, List<Subscription<? extends GameEvent>>> listeners = new HashMap<>();

    /**
     * Subscribes a listener to a specific event type tied to a subscriberId (e.g. Card instanceId).
     */
    public <T extends GameEvent> void subscribe(String subscriberId, Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(new Subscription<>(subscriberId, listener));
    }

    /**
     * Unsubscribes a specific listener.
     */
    public <T extends GameEvent> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        List<Subscription<? extends GameEvent>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.removeIf(sub -> sub.listener == listener);
        }
    }

    /**
     * Unsubscribes all listeners associated with a specific subscriber ID.
     * Crucial for preventing memory leaks when cards are destroyed, bounced, or transformed.
     */
    public void unsubscribeAll(String subscriberId) {
        for (List<Subscription<? extends GameEvent>> eventListeners : listeners.values()) {
            Iterator<Subscription<? extends GameEvent>> it = eventListeners.iterator();
            while (it.hasNext()) {
                if (it.next().subscriberId.equals(subscriberId)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Publishes an event to all subscribed listeners.
     */
    @SuppressWarnings("unchecked")
    public <T extends GameEvent> void publish(T event) {
        List<Subscription<? extends GameEvent>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            // Iterate over a copy to prevent concurrent modification issues if listeners unsubscribe during the event
            for (Subscription<? extends GameEvent> sub : new ArrayList<>(eventListeners)) {
                ((Consumer<T>) sub.listener).accept(event);
            }
        }
    }
}
