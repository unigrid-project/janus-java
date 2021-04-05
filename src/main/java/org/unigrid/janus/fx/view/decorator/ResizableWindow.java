package org.unigrid.janus.fx.view.decorator;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import org.unigrid.janus.model.Direction;

public class ResizableWindow {
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
			window.setWidth(current.getWidth());
		}

		if (current.getHeight() != window.getHeight()) {
			window.setHeight(current.getHeight());
		}
	}

	public void decorate(DecoratableWindow window) {
		/* Slightly speeds up rendering? */
		window.getStage().getScene().getWindow().setForceIntegerRenderScale(true);

		window.getStage().getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, (e) -> {
			origin.setRect(
				window.getStage().getX(), window.getStage().getY(),
				window.getStage().getWidth(), window.getStage().getHeight()
			);

			current.setRect(origin);
			clickedPoint.setLocation(e.getScreenX(), e.getScreenY());

			x1 = origin.getMinX();
			y1 = origin.getMinY();
			x2 = origin.getMaxX();
			y2 = origin.getMaxY();
		});

		window.getStage().getScene().addEventFilter(MouseEvent.MOUSE_RELEASED, (e) -> {
			dragDirection = Optional.empty();
		});

		window.getStage().getScene().addEventFilter(MouseEvent.MOUSE_DRAGGED, (e) -> {
			if (dragDirection.isPresent() && !stillResizing) {
				stillResizing = true;
				refreshCoordinates(e.getScreenX(), e.getScreenY());

				Platform.runLater(() -> {
					refreshFXWindowPosition(window.getStage().getScene().getWindow());
					stillResizing = false;
				});
			}
		});
	}

	public void resize(Direction direction) {
		dragDirection = Optional.of(direction);
	}
}
