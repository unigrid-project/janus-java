package org.unigrid.janus.model.signal;

import org.unigrid.janus.model.rest.entity.RedelegationsRequest.Response;

public class RedelegationsEvent {
    private final Response redelegationsResponse;

    public RedelegationsEvent(Response redelegationsResponse) {
        this.redelegationsResponse = redelegationsResponse;
    }

    public Response getRedelegationsResponse() {
        return redelegationsResponse;
    }
}
