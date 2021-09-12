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

package org.unigrid.janus;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.unigrid.janus.model.Daemon;
import org.unigrid.janus.view.MainWindow;

@ApplicationScoped
public class Janus extends BaseApplication {
	@Inject
	private Daemon daemon;

	@Inject
	private MainWindow mainWindow;

	@PostConstruct @SneakyThrows
	private void init() {
		daemon.start();
	}

	@PreDestroy @SneakyThrows
	private void destroy() {
		daemon.stop();
	}

	@Override
	public void start(Stage stage, Application.Parameters parameters) throws Exception {
		mainWindow.show();
	}
}
