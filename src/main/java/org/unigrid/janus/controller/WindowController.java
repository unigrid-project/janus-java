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
