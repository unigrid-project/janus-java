

package org.unigrid.janus.model.signal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data @Builder
@Getter
@AllArgsConstructor
public class DelegationStatusEvent {
    private double delegatedAmount;
    private int gridnodeCount;
}
