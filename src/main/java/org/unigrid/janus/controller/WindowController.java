package org.unigrid.janus.controller;

import java.io.Serializable;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.unigrid.janus.model.Direction;
import org.unigrid.janus.model.event.WindowHeaderEvent;
import org.unigrid.janus.model.event.WindowResizeEvent;

@Data
@Named
@RequestScoped
public class WindowController implements Serializable {
	@Autowired
	private ApplicationEventPublisher event;

	private void publish(WindowHeaderEvent.Type eventType) {
		event.publishEvent(new WindowHeaderEvent(eventType));
	}

	public void onClose() {
		publish(WindowHeaderEvent.Type.CLOSE);
	}

	public void onMaximize() {
		publish(WindowHeaderEvent.Type.MAXIMIZE);
	}

	public void onMinimize() {
		publish(WindowHeaderEvent.Type.MINIMIZE);
	}

	public void onMove() {
		publish(WindowHeaderEvent.Type.MOVE);
	}

	public void onResize(Direction direction) {
		event.publishEvent(new WindowResizeEvent(direction));
	}
}
