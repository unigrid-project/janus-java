package org.unigrid.janus.model.signal;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class CollateralUpdateEvent {
    private final boolean success;
    private final int amount;

    public static CollateralUpdateEvent success(int amount) {
        return new CollateralUpdateEvent(true, amount);
    }

    public static CollateralUpdateEvent failure() {
        return new CollateralUpdateEvent(false, 0);
    }
}
