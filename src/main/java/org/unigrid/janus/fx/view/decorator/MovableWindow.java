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

package org.unigrid.janus.fx.view.decorator;

import java.awt.Point;
import javafx.scene.input.MouseEvent;

public class MovableWindow {
	private final Point clickedPoint = new Point(0, 0);
	private boolean moving;

	public void move() {
		moving = true;
	}

	public void decorate(DecoratableWindow window) {
		window.getStage().getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, (e) -> {
			clickedPoint.setLocation(
				window.getStage().getX() - e.getScreenX(),
				window.getStage().getY() - e.getScreenY()
			);
		});

		window.getStage().getScene().addEventFilter(MouseEvent.MOUSE_RELEASED, (e) -> {
			moving = false;
		});

		window.getStage().getScene().addEventFilter(MouseEvent.MOUSE_DRAGGED, (e) -> {
			if (moving) {
				window.getStage().setX(e.getScreenX() + clickedPoint.getX());
				window.getStage().setY(e.getScreenY() + clickedPoint.getY());
			}
		});
	}
}
