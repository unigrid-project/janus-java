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
