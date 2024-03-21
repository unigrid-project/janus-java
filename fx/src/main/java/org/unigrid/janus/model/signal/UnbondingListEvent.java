package org.unigrid.janus.model.signal;

import java.util.List;
import org.unigrid.janus.model.gridnode.UnbondingEntry;

public class UnbondingListEvent {
    private final List<UnbondingEntry> unbondingList;

    public UnbondingListEvent(List<UnbondingEntry> unbondingList) {
        this.unbondingList = unbondingList;
    }

    public List<UnbondingEntry> getUnbondingList() {
        return unbondingList;
    }
}
