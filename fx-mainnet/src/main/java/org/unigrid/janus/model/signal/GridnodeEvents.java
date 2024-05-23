package org.unigrid.janus.model.signal;

import java.util.List;

import org.unigrid.janus.model.gridnode.GridnodeListViewItem;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class GridnodeEvents {
	private EventType eventType;
	private List<GridnodeListViewItem> gridnodeListViewItems; 
	// add any additional types here as needed
	public enum EventType{
		GRIDNODE_STARTED, GRIDNODE_LIST_LOADED
	}
}
