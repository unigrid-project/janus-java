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

package org.unigrid.janus.view;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import lombok.SneakyThrows;
import org.unigrid.janus.model.service.WindowService;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

@Dependent
public class MainWindow implements Window {
	@Inject
	private Stage stage;
	private WindowService window = new WindowService();

	@SneakyThrows
	public void show() {
		try {
			window.setStage(stage);
			stage.centerOnScreen();
			stage.initStyle(StageStyle.UNDECORATED);
			stage.setResizable(true);
			stage.show();
		} catch (Exception e) {
			Alert a = new Alert(AlertType.ERROR,
				  				e.getMessage(),
				  				ButtonType.OK);
			a.showAndWait();
		}
	}

	public void bindDebugListViewWidth(double multiplier) {
		ListView list = (ListView) window.lookup("lstDebug");
		list.setCellFactory(param -> new ListCell<String>() {
			{
				prefWidthProperty().bind(list.widthProperty().multiply(multiplier));
				setMaxWidth(Control.USE_PREF_SIZE);
				setWrapText(true);
			}

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);

				if (item != null && !empty) {
					setText(item);
				} else {
					setText(null);
				}
			}
		});
	}
}
