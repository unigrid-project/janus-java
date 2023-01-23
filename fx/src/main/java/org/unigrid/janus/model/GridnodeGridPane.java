package org.unigrid.janus.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public class GridnodeGridPane extends GridPane {
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
