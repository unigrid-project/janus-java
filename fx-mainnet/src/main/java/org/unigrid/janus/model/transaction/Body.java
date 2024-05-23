package org.unigrid.janus.model.transaction;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class Body {
	private List<GridnodeMessage> messages;
	private String memo;
	private String timeoutHeight;
	// ... other fields
}
