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

package org.unigrid.janus.view;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import lombok.SneakyThrows;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import org.unigrid.janus.model.JanusModel;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.event.CloseJanusEvent;
import org.unigrid.janus.model.service.WindowService;

@Eager
@ApplicationScoped
public class MainWindow implements Window {
	private WindowService window = WindowService.getInstance();

	@Inject private Stage stage;
	@Inject private JanusModel janusModel;
	@Inject private Event<CloseJanusEvent> closeJanusEvent;

	@PostConstruct
	private void init() {
		stage.centerOnScreen();
		stage.initStyle(StageStyle.UNDECORATED);
		stage.setResizable(true);
	}

	@SneakyThrows
	public void show() {
		try {
			window.setStage(stage);
			window.getSettingsController().setVersion(janusModel.getVersion());
			stage.show();
		} catch (Exception e) {
			Alert a = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
			a.showAndWait();
		}
	}

	public void hide() {
		stage.hide();
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

	private void onClose(@Observes Event<CloseJanusEvent> event) {
		this.stage.setWidth(900);
		this.stage.hide();
	}
}
