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
import javafx.scene.control.ListView;
import lombok.SneakyThrows;
import org.unigrid.janus.model.service.Daemon;
import org.unigrid.janus.model.rpc.entity.BlockCount;
import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.view.MainWindow;

@ApplicationScoped
public class Janus extends BaseApplication {
	@Inject
	private Daemon daemon;

	@Inject
	private RPCService rpc;

	@Inject
	private DebugService debug;

	@Inject
	private WindowService window;

	@Inject
	private MainWindow mainWindow;

	@PostConstruct @SneakyThrows
	private void init() {
		try {
			daemon.start();
		} catch (Exception e) {
			debug.log(String.format("ERROR: %s", e.getMessage()));
		}
		debug.log("Daemon start done.");
	}

	@PreDestroy @SneakyThrows
	private void destroy() {
		daemon.stop();
	}

	@Override
	public void start(Stage stage, Application.Parameters parameters) throws Exception {
		try {
			mainWindow.show();

			mainWindow.bindDebugListViewWidth(0.98);
			debug.setListView((ListView) window.lookup("lstDebug"));

			final Info info = rpc.call(new Info.Request(), Info.class);
			System.out.println(info.getResult());

			final BlockCount count = rpc.call(new BlockCount.Request(), BlockCount.class);
			System.out.println(count.getResult());
		} catch (Exception e) {
			debug.log(String.format("ERROR: %s", e.getMessage()));
		}
	}
}
