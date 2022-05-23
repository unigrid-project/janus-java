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

package org.unigrid.janus.view.decorator;

import java.awt.Point;
import javafx.scene.Cursor;
import javafx.scene.Node;

public class MovableWindowDecorator implements Decorator {
	private final Point clickedPoint = new Point(0, 0);

	@Override
	public void decorate(Decoratable decoratable, Node node) {
		node.setOnMousePressed(e -> {
			clickedPoint.setLocation(
				decoratable.getStage().getX() - e.getScreenX(),
				decoratable.getStage().getY() - e.getScreenY()
			);
			decoratable.getStage().getScene().setCursor(Cursor.MOVE);
		});

		node.setOnMouseReleased(e -> {
			decoratable.getStage().getScene().setCursor(Cursor.DEFAULT);
		});

		node.setOnMouseDragged(e -> {
			decoratable.getStage().setX(e.getScreenX() + clickedPoint.getX());
			decoratable.getStage().setY(e.getScreenY() + clickedPoint.getY());
		});
	}
}
