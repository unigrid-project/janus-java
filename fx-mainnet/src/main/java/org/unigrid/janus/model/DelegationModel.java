

package org.unigrid.janus.model;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@ApplicationScoped
public class DelegationModel {
    private double delegatedAmount;
    private int gridnodeCount;

    private List<DelegationModelListener> listeners = new ArrayList<>();

    public void addListener(DelegationModelListener listener) {
        listeners.add(listener);
    }

    public void notifyListeners() {
        for (DelegationModelListener listener : listeners) {
            listener.modelUpdated();
        }
    }

    // Setters created by @Data will now notify listeners
    public void setDelegatedAmount(double delegatedAmount) {
        this.delegatedAmount = delegatedAmount;
        notifyListeners();
    }

    public void setGridnodeCount(int gridnodeCount) {
        this.gridnodeCount = gridnodeCount;
        notifyListeners();
    }
}

interface DelegationModelListener {
    void modelUpdated();
}

