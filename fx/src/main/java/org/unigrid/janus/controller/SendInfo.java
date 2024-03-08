
package org.unigrid.janus.controller;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SendInfo {
    CosmosCredentials credentials;
    String toAddress;
    BigDecimal amountInAtom;
}
