package org.unigrid.janus.fx.view;

import javafx.application.Platform;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.unigrid.janus.model.event.WindowHeaderEvent;
import org.unigrid.janus.model.event.WindowResizeEvent;

@Component
public class FXEventObserver {
	@EventListener
	public void onWindowHeaderEvent(WindowHeaderEvent windowHeaderEvent) {
		Platform.runLater(() -> {
			switch (windowHeaderEvent.getEventType()) {
				case CLOSE:    MainWindow.getInstance().stop(); break;
				case MAXIMIZE: MainWindow.getInstance().maximize(); break;
				case MINIMIZE: MainWindow.getInstance().minimize(); break;
				case MOVE:     MainWindow.getInstance().move(); break;
			}
		});
	}

	@EventListener
	public void onWindowResizeEvent(WindowResizeEvent windowResizeEvent) {
		Platform.runLater(() -> {
			MainWindow.getInstance().resize(windowResizeEvent.getDirection());
		});
	}
}
