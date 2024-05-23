package org.unigrid.janus.model.transaction;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class GridnodeTransaction {
	private Body body;
	private AuthInfo authInfo;
	private List<String> signatures;
}
