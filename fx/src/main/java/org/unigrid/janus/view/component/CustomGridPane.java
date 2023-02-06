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

import java.util.Comparator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.unigrid.janus.model.Gridnode;

public class CustomGridPane extends GridPane {
	public static int NUMBER_OF_HEADERS = 4;

	public CustomGridPane() {
		super();
	}

	public ObservableList<ObservableList<Node>> getChildrenAsRows() {
		return getChildrenAsRows(true);
	}

	public ObservableList<ObservableList<Node>> getChildrenAsRows(Boolean skipHeaders) {
		ObservableList<ObservableList<Node>> rowList = FXCollections.observableArrayList();
		ObservableList<Node> row = FXCollections.observableArrayList();
		ObservableList<Node> chirldrens = super.getChildren();

		int skipCount = 0;
		//System.out.println();
		//System.out.println();
		FlowPane flowPane  = null;
		for (Node n : chirldrens) {
			if (n instanceof StackPane) {
				StackPane statusStackPane = (StackPane) n;
				flowPane = (FlowPane) statusStackPane.getChildren().stream().filter((c)-> c.getId().equals("entryFlowPane")).findFirst().get();
			}
			//System.out.println("Node n: " + n);
			if (skipHeaders == true && skipCount < NUMBER_OF_HEADERS) {
				skipCount++;
				continue;
			}

			if (!flowPane.isVisible() && n.getId().equals("entryPrivateKey")) {
				row.add(n);
				rowList.add(row);
				row = FXCollections.observableArrayList();

			} else if (flowPane.isVisible() && n.getId().equals("entryOutputTa")) {
				row.add(n);
				rowList.add(row);
				row = FXCollections.observableArrayList();
			} else {
				row.add(n);
			}
		}
		//System.out.println("rowList: " + rowList);
		//System.out.println("rowList size: " + rowList.size());
		rowList.sort(new Comparator<ObservableList<Node>>() {
			@Override
			public int compare(ObservableList<Node> list1, ObservableList<Node> list2) {
				return Integer.compare(list1.size(), list2.size());
			}
		});
		return rowList;
	}

	public void addChirldren(StackPane statusStackPane, Label alias, Label address, Button btn, Label privateKey,
		TextArea ta) {
		addChirldren(statusStackPane, alias, address, btn, privateKey, ta, super.getRowCount());
	}

	public void addChirldren(StackPane statusStackPane, Label alias, Label address, Button btn, Label privateKey,
		TextArea ta, int row) {
		super.addRow(row, statusStackPane, alias, address, btn, privateKey);
		super.addRow(row + 1, ta);
	}

	public void addChirldren(StackPane statusStackPane, Label alias, Label address, Label privateKey) {
		int row = super.getRowCount();
		super.addRow(row, statusStackPane, alias, address, privateKey);
	}

	public ObservableList<Node> getChildrenAsRow(String privateKey) {
		ObservableList<ObservableList<Node>> childrensList = getChildrenAsRows();
		for (ObservableList<Node> nodeList : childrensList) {
			Label privateKeyLabel = (Label) nodeList.stream().filter((n)-> n.getId().equals("entryPrivateKey")).findFirst().get();

			if (privateKeyLabel.getText().equals(privateKey)) {
				return nodeList;
			}
		}

		return FXCollections.observableArrayList();
	}

	public Node getChildren(String privateKey, String id) {
		ObservableList<Node> list = getChildrenAsRow(privateKey);
		//System.out.println("list: " + list);
		for (Node n : list) {
			if (/*n.getId() != null &&*/ n.getId().equals(id)) {
				return n;
			}
		}

		return null;
	}

	public void removeChildren(String privateKey) {
		for (Node n : getChildrenAsRow(privateKey)) {
			super.getChildren().remove(n);
		}
	}

	/*public void updateChildren(String privateKey, String id, Node node) {
		ObservableList<Node> list = getChildrenAsRow(privateKey);
		Node resultNode = list.stream().filter((n) -> n.getId().equals(id)).findFirst().get();
		int colIndex = GridPane.getColumnIndex(resultNode);
		int rowIndex = GridPane.getRowIndex(resultNode);
		int colSpan = GridPane.getColumnSpan(resultNode);
		int rowSpan = GridPane.getRowSpan(resultNode);

		super.getChildren().remove(resultNode);
		super.add(node, colIndex, rowIndex, colSpan, rowSpan);
	}*/

	/*public void updateChildrens(StackPane statusStackPane, Label alias, Label address, Button btn, Label privateKey,
		TextArea ta) {

		ObservableList<Node> list = getChildrenAsRow(privateKey.getText());

		for (int i = 0; i < list.size(); i++) {
			int colIndex = GridPane.getColumnIndex(list.get(i));
			int rowIndex = GridPane.getRowIndex(list.get(i));
			int colSpan = GridPane.getColumnSpan(list.get(i));
			int rowSpan = GridPane.getRowSpan(list.get(i));

			super.getChildren().remove(list.get(i));

			if (list.get(i).getId().equals("entryStackPaneStatus")) {
				super.add(statusStackPane, colIndex, rowIndex, colSpan, rowSpan);
			} else if (list.get(i).getId().equals("entryAlias")) {
				super.add(alias, colIndex, rowIndex, colSpan, rowSpan);
			} else if (list.get(i).getId().equals("entryAddress")) {
				super.add(address, colIndex, rowIndex, colSpan, rowSpan);
			} else if (list.get(i).getId().equals("entryOutputBtn")) {
				super.add(btn, colIndex, rowIndex, colSpan, rowSpan);
			} else if (list.get(i).getId().equals("entryPrivateKey")) {
				super.add(privateKey, colIndex, rowIndex, colSpan, rowSpan);
			} else if (list.get(i).getId().equals("entryOutputTa")) {
				super.add(ta, colIndex, rowIndex, colSpan, rowSpan);
			}
		}
	}*/

	/*public void setItems(ObservableList<Gridnode> gridnodes) {
		for (Gridnode g : gridnodes) {
			Label status = new Label(g.getStatus().name());
			Label alias = new Label(g.getAlias());
			Label address = new Label(g.getAddress());

			addChirldren(status, alias, address);
		}
	}*/
}
