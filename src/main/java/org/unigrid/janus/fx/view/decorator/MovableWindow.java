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
