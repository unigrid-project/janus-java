package org.unigrid.janus.model;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;

@Data
@ApplicationScoped
public class UnboundingBalanceModel {
    private double unboundingAmount;
}
