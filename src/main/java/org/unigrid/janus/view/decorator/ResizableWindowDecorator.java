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
import java.awt.Rectangle;
import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.unigrid.janus.model.Direction;

public class ResizableWindowDecorator implements Decorator {
	private Optional<Direction> dragDirection = Optional.empty();

	private final Point clickedPoint = new Point();
	private final Rectangle.Double current = new Rectangle.Double();
	private final Rectangle.Double origin = new Rectangle.Double();
	private boolean stillResizing;

	private double x1;
	private double y1;
	private double x2;
	private double y2;

	private void refreshCoordinates(double screenX, double screenY) {
		if (dragDirection.get() == Direction.NORTH || dragDirection.get() == Direction.NORTHEAST
			|| dragDirection.get() == Direction.NORTHWEST) {
			y1 = origin.getMinY() - (clickedPoint.getY() - screenY);
		}

		if (dragDirection.get() == Direction.NORTHEAST || dragDirection.get() == Direction.EAST
			|| dragDirection.get() == Direction.SOUTHEAST) {
			x2 = origin.getMaxX() - (clickedPoint.getX() - screenX);
		}

		if (dragDirection.get() == Direction.SOUTHEAST || dragDirection.get() == Direction.SOUTH
			|| dragDirection.get() == Direction.SOUTHWEST) {
			y2 = origin.getMaxY() - (clickedPoint.getY() - screenY);
		}

		if (dragDirection.get() == Direction.SOUTHWEST || dragDirection.get() == Direction.WEST
			|| dragDirection.get() == Direction.NORTHWEST) {
			x1 = origin.getMinX() - (clickedPoint.getX() - screenX);
		}
	}

	private void refreshFXWindowPosition(Window window) {
		current.setFrameFromDiagonal(x1, y1, x2, y2);

		if (current.getX() != window.getX()) {
			window.setX(current.getX());
		}

		if (current.getY() != window.getY()) {
			window.setY(current.getY());
		}

		if (current.getWidth() != window.getWidth()) {
			if (current.getWidth() >= 600) {
				window.setWidth(current.getWidth());
			}
		}

		if (current.getHeight() != window.getHeight()) {
			if (current.getHeight() >= 400) {
				window.setHeight(current.getHeight());
			}
		}
	}

	@Override
	public void decorate(Decoratable decoratable, Node node) {
		// Slightly speeds up rendering?
		decoratable.getStage().getScene().getWindow().setForceIntegerRenderScale(true);

		node.setOnMousePressed(e -> {
			origin.setRect(
				decoratable.getStage().getX(), decoratable.getStage().getY(),
				decoratable.getStage().getWidth(), decoratable.getStage().getHeight()
			);

			current.setRect(origin);
			clickedPoint.setLocation(e.getScreenX(), e.getScreenY());

			x1 = origin.getMinX();
			y1 = origin.getMinY();
			x2 = origin.getMaxX();
			y2 = origin.getMaxY();
		});

		node.setOnMouseReleased(e -> {
			dragDirection = Optional.empty();
		});

		node.setOnMouseExited(e -> {
			decoratable.getStage().getScene().setCursor(Cursor.DEFAULT);
		});

		node.setOnMouseDragged(e -> {
			if (dragDirection.isPresent() && !stillResizing) {
				stillResizing = true;
				refreshCoordinates(e.getScreenX(), e.getScreenY());

				Platform.runLater(() -> {
					refreshFXWindowPosition(decoratable.getStage().getScene().getWindow());
					stillResizing = false;
				});
			}
		});
	}

	public void resize(Decoratable decoratable, Direction direction) {
		final Scene scene = decoratable.getStage().getScene();

		switch (direction) {
			case NORTH -> scene.setCursor(Cursor.N_RESIZE);
			case NORTHWEST -> scene.setCursor(Cursor.NW_RESIZE);
			case NORTHEAST -> scene.setCursor(Cursor.NE_RESIZE);
			case WEST -> scene.setCursor(Cursor.W_RESIZE);
			case EAST -> scene.setCursor(Cursor.E_RESIZE);
			case SOUTHWEST -> scene.setCursor(Cursor.SW_RESIZE);
			case SOUTH -> scene.setCursor(Cursor.S_RESIZE);
			case SOUTHEAST -> scene.setCursor(Cursor.SE_RESIZE);
		}

		dragDirection = Optional.of(direction);
	}
}
