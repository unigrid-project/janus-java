package org.unigrid.janus.model.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GridnodeMessage {
	private String type; // renamed from '@type' for valid Java naming
	@JsonProperty("delegator_address")
	private String delegatorAddress;
	@JsonProperty("unique_id")
	private String uniqueId;
	private String amount;
	private String timestamp;

}
