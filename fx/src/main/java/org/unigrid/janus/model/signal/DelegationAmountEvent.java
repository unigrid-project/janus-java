package org.unigrid.janus.model.signal;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class DelegationAmountEvent {
    private final BigDecimal amount;
}
