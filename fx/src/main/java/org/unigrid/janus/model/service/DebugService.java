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

package org.unigrid.janus.model.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import jakarta.enterprise.context.ApplicationScoped;
import javafx.scene.control.ListView;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.cdi.Eager;

@Eager
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

	public void print(String msg, String className) {
		String path = DataDirectory.get().concat("/wallet.log");

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(path, true))) {
			bw.write(getCurrentDate().concat(": ").concat(className).concat("- ").concat(msg));
			System.out.println(msg);
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void trace(String msg) {
			System.out.println(msg);		
	}

	public String getCurrentDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm");
		Date date = new Date();
		String dateS = dateFormat.format(date);
		return dateS;
	}
}
