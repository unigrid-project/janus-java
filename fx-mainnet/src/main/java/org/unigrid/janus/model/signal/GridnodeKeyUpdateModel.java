package org.unigrid.janus.model.signal;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;

@Data
@ApplicationScoped
public class GridnodeKeyUpdateModel {
    private boolean additionalKeysPossible;
    private String message;
}

