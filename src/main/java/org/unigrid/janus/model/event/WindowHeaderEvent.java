package org.unigrid.janus.model.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class WindowHeaderEvent {
	@Getter private final Type eventType;

	public enum Type {
		CLOSE, MAXIMIZE, MINIMIZE, MOVE
	}
}
