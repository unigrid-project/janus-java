/*
    The Janus Wallet
    Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model.signal;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.unigrid.janus.model.Gridnode;
import org.unigrid.janus.model.GridnodeDeployment.State;

@Data @Builder
public class NodeUpdate {
	private List<Gridnode> confList;
	private Pair<Gridnode, State> gridnode;
	private String stepName;
	private int  step;
	private double progress;

	public static class NodeUpdateBuilder {
		public NodeUpdateBuilder pair(Gridnode gridnode, State state) {
			this.gridnode = Pair.of(gridnode, state);
			return this;
		}
	}
}
