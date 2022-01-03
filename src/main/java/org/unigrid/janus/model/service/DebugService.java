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

package org.unigrid.janus.model.service;

import jakarta.enterprise.context.ApplicationScoped;
import javafx.scene.control.ListView;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

@ApplicationScoped
public class DebugService {
	private static ListView output;
	private static ObservableList<String> items;

	public void setListView(ListView lv) {
		output = lv;
		if (lv != null && items != null) {
			output.setItems(items);
		}
	}

	public void log(String msg) {
		if (output != null) {
			output.getItems().add(msg);
		} else {
			if (items == null) {
				items = FXCollections.observableArrayList();
			}
			items.add(msg);
		}
	}
}
