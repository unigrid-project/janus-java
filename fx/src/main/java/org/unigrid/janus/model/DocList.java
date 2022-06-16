package org.unigrid.janus.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Data;
import org.unigrid.janus.model.service.DebugService;

@Data
public class DocList {
	private static DebugService debug = new DebugService();
	public static final String DOCUMENTATION_LIST = "docList";
	private static PropertyChangeSupport pcs;
	private static ObservableList<Documentation> doclist = FXCollections.observableArrayList();

	public DocList() {
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

	public ObservableList<Documentation> getDoclist() {
		return this.doclist;
	}

	public void setDoclist(List<Documentation> list) {
		int oldCount = 0;
		doclist.clear();
		int newCount = 0;
		for(int i = 0; i < list.size(); ++i) {
			//debug.log(String.format("doclist title: %s", list.get(i).getTitle()));
			doclist.add(list.get(i));
			newCount++;
		}
		this.pcs.firePropertyChange(this.DOCUMENTATION_LIST, oldCount, newCount);
	}

}
