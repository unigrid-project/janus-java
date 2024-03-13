

package org.unigrid.janus.model.signal;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class DelegationStatusEvent {
	private final double delegatedAmount;
	private final int gridnodeCount;

	public static DelegationStatusEvent of(double delegatedAmount, int gridnodeCount) {
		return new DelegationStatusEvent(delegatedAmount, gridnodeCount);
	}
}
