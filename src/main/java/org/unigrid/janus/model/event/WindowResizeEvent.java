package org.unigrid.janus.model.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.unigrid.janus.model.Direction;

@AllArgsConstructor
public class WindowResizeEvent {
	@Getter private final Direction direction;
}
