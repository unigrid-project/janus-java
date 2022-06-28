package org.unigrid.janus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Data;

@Data
public class Documentation {
	private String link;
	private String title;
	private static ObservableList<Documentation> docs = FXCollections.observableArrayList();
}
