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

package org.unigrid.janus.controller.component;

import jakarta.enterprise.context.Dependent;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import lombok.Getter;
import org.unigrid.janus.model.Direction;
import org.unigrid.janus.view.decorator.Decoratable;
import org.unigrid.janus.view.decorator.ResizableWindowDecorator;

@Dependent
public class WindowBorderController implements Decoratable, Initializable {

	private Set<Node> decoratedNodes = new HashSet();
	private ResizableWindowDecorator resizableWindowDecorator = new ResizableWindowDecorator();
	@Getter
	private Stage stage;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
	}

	@FXML
	private void onDecorate(MouseEvent event) {
		if (!decoratedNodes.contains((Node) event.getSource())) {
			stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			resizableWindowDecorator.decorate(this, (Node) event.getSource());
			decoratedNodes.add((Node) event.getSource());
		}

		final Direction direction = Direction.valueOf((String) ((Node) event.getSource()).getUserData());
		resizableWindowDecorator.resize(this, direction);
	}
}
