package org.unigrid.janus.model.signal;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class GridnodeEvents {
	private EventType eventType;
	// add any additional types here as needed
	public enum EventType{
		GRIDNODE_STARTED
	}
}
