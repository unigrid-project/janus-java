/*
	The Janus Wallet
	Copyright © 2021 The Unigrid Foundation

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.unigrid.janus.model.rpc.entity.GridnodeList;
import org.unigrid.janus.model.service.DebugService;

public class GridnodeListModel {
	private static DebugService debug = new DebugService();
	public static final String GRIDNODE_LIST = "gridnodeList";
	private static PropertyChangeSupport pcs;
	private static ObservableList<Gridnode> gridnodes = FXCollections.observableArrayList();

	public GridnodeListModel() {
		if (this.pcs != null) {
			return;
		}
		this.pcs = new PropertyChangeSupport(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}

	public ObservableList<Gridnode> getGridnodes() {
		return this.gridnodes;
	}

	public void setGridnodes(GridnodeList list) {
		int oldCount = 0;
		gridnodes.clear();
		int newCount = 0;
		for (Gridnode g : list.getResult()) {
			debug.log(String.format("gridnode name: %s", g.getAlias()));
			gridnodes.add(g);
			newCount++;
		}
		this.pcs.firePropertyChange(this.GRIDNODE_LIST, oldCount, newCount);
	}
}
