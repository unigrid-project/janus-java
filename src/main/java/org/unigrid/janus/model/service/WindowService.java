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
import javafx.stage.Stage;
import javafx.scene.Node;
import org.unigrid.janus.controller.component.WindowBarController;

@ApplicationScoped
public class WindowService {
	private static Stage stage;

	public Stage getStage() {
		return this.stage;
	}

	public void setStage(Stage value) {
		this.stage = value;
	}

	public Node lookup(String id) {
		if (stage != null) {
			return stage.getScene().lookup("#" + id);
		} else {
			return null;
		}
	}

	private static WindowBarController wbController;

	public WindowBarController getWindowBarController() {
		return this.wbController;
	}

	public void setWindowBarController(WindowBarController controller) {
		this.wbController = controller;
	}
}
