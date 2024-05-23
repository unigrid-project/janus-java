
package org.unigrid.janus.controller;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendInfo {
    CosmosCredentials credentials;
    String toAddress;
    long amountInAtom;
}
