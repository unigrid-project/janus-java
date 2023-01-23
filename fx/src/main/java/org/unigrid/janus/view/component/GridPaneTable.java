/*
	The Janus Wallet
	Copyright © 2021-2023 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.view.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public class GridPaneTable extends GridPane {
	public ObservableList<ObservableList<Node>> getChildrenAsRows(int columnLimit) {
		ObservableList<ObservableList<Node>> rowList = FXCollections.observableArrayList();
		for (Node n : super.getChildren()) {
			ObservableList<Node> row = FXCollections.observableArrayList();
			for (int i = 0; i < columnLimit; i++) {
				row.add(super.getChildren().get(i));
			}
			rowList.add(row);
		}
		return rowList;
	}
}
