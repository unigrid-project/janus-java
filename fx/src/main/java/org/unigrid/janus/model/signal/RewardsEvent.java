package org.unigrid.janus.model.signal;

import org.unigrid.janus.model.rest.entity.RewardsRequest.Response;

public class RewardsEvent {
    private final Response rewardsResponse;

    public RewardsEvent(Response rewardsResponse) {
        this.rewardsResponse = rewardsResponse;
    }

    public Response getRewardsResponse() {
        return rewardsResponse;
    }
}
