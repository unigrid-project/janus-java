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

package org.unigrid.janus.model.service;

import jakarta.enterprise.context.ApplicationScoped;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;
import org.unigrid.janus.model.cdi.Eager;

@Slf4j
@Eager @ApplicationScoped
public class GridPaneService {
	public static ObservableList<ObservableList<Node>> getChildrenAsRows(GridPane gridPane) {
		ObservableList<ObservableList<Node>> rowList = FXCollections.observableArrayList();
		ObservableList<Node> row = FXCollections.observableArrayList();
		for (Node n : gridPane.getChildren()) {
			int count = 0;
			if (GridPane.getRowIndex(n).equals(count)) {
				row.add(gridPane.getChildren().get(count));
			} else {
				rowList.add(row);
				row = FXCollections.observableArrayList();
				count++;
			}
		}
		return rowList;
	}
}
