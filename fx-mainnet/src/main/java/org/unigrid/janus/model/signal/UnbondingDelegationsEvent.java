package org.unigrid.janus.model.signal;

import org.unigrid.janus.model.rest.entity.UnbondingDelegationsRequest.Response;

public class UnbondingDelegationsEvent {
    private final Response unbondingDelegationsResponse;

    public UnbondingDelegationsEvent(Response unbondingDelegationsResponse) {
        this.unbondingDelegationsResponse = unbondingDelegationsResponse;
    }

    public Response getUnbondingDelegationsResponse() {
        return unbondingDelegationsResponse;
    }
}
