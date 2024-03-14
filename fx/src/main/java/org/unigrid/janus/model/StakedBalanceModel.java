package org.unigrid.janus.model;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;

@Data
@ApplicationScoped
public class StakedBalanceModel {
    private long stakedBalance;
}
