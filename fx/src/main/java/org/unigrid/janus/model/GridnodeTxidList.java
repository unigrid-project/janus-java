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

package org.unigrid.janus.model;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.unigrid.janus.model.rpc.entity.GridnodeList;
import org.unigrid.janus.model.service.DebugService;

public class GridnodeTxidList {
	public static final String GRIDNODE_LIST = "gridnodeList";

	@Inject private DebugService debug;

	@Getter
	private ObservableList<Gridnode> gridnodes = FXCollections.observableArrayList();

	public void setGridnodes(GridnodeList list) throws IOException {
		gridnodes.clear();

		for (Gridnode g : list.getResult()) {
			//System.out.println("gridnode txhash: " + g.getTxhash());
			g.setAvailableTxhash(isTxidInUse(g.getTxhash()));
			gridnodes.add(g);
		}
	}

	private boolean isTxidInUse(String txhash) throws IOException {
		File confFile = DataDirectory.getGridnodeFile();
		String data = FileUtils.readFileToString(confFile, "UTF-8");
		//System.out.println(data);
		int intIndex = data.indexOf(txhash);

		return (intIndex != -1);
	}
}
