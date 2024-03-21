package org.unigrid.janus.model.gridnode;

import java.util.List;
import lombok.Data;

@Data
public class QueryUnbondingEntriesResponse {
    private List<UnbondingEntry> unbondingEntries;
}