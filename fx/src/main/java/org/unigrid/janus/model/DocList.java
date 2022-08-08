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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.unigrid.janus.model.service.DebugService;

public class DocList {
	public static final String DOCUMENTATION_LIST = "docList";

	private static DebugService debug = new DebugService();
	private static ObservableList<Documentation> doclist = FXCollections.observableArrayList();

	private PropertyChangeSupport pcs;

	public DocList() {
		if (pcs != null) {
			return;
		}
		pcs = new PropertyChangeSupport(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public ObservableList<Documentation> getDoclist() {
		return doclist;
	}

	public void setDoclist(List<Documentation> list) {
		int oldCount = 0;
		doclist.clear();
		int newCount = 0;

		for (int i = 0; i < list.size(); ++i) {
			//debug.log(String.format("doclist title: %s", list.get(i).getTitle()));
			doclist.add(list.get(i));
			newCount++;
		}

		pcs.firePropertyChange(DOCUMENTATION_LIST, oldCount, newCount);
	}
}
