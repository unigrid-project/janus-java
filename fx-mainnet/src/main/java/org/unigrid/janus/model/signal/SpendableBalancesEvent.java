package org.unigrid.janus.model.signal;

import org.unigrid.janus.model.rest.entity.SpendableBalancesRequest.Response;

public class SpendableBalancesEvent {
    private final Response spendableBalances;

    public SpendableBalancesEvent(Response rewardsResponse) {
        this.spendableBalances = rewardsResponse;
    }

    public Response getSpendableBalancesResponse() {
        return spendableBalances;
    }
}
