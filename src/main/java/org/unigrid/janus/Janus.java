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
import org.unigrid.janus.model.service.Daemon;
import org.unigrid.janus.model.rpc.entity.Balance;
import org.unigrid.janus.model.rpc.entity.BlockCount;
import org.unigrid.janus.model.rpc.entity.ConnectionCount;
import org.unigrid.janus.model.rpc.entity.DataDirectory;
import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.rpc.entity.ListAddressGroupings;
import org.unigrid.janus.model.rpc.entity.StakingStatus;
import org.unigrid.janus.model.rpc.entity.WalletInfo;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.view.MainWindow;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

@ApplicationScoped
public class Janus extends BaseApplication {
	@Inject
	private Daemon daemon;

	@Inject
	private RPCService rpc;

	@Inject
	private MainWindow mainWindow;

	@PostConstruct @SneakyThrows
	private void init() {
		try {
			daemon.start();
		} catch (Exception e) {
			Alert a = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
			a.showAndWait();
		}
	}

	@PreDestroy @SneakyThrows
	private void destroy() {
		daemon.stop();
	}

	@Override
	public void start(Stage stage, Application.Parameters parameters) throws Exception {
		try {
			mainWindow.show();

			final Info info = rpc.call(new Info.Request(), Info.class);
			Jsonb jsonb = JsonbBuilder.create();
			String result = String.format("Info result: %s", jsonb.toJson(info.getResult()));
			// System.out.println(info.getResult());
			Alert a = new Alert(AlertType.INFORMATION, result, ButtonType.OK);
			a.showAndWait();

			rpc.alert(new BlockCount.Request());

			rpc.alert(new Balance.Request());

			rpc.alert(new ConnectionCount.Request());

			rpc.alert(new DataDirectory.Request());

			rpc.alert(new ListTransactions.Request());

			rpc.alert(new ListAddressGroupings.Request());

			rpc.alert(new StakingStatus.Request());

			rpc.alert(new WalletInfo.Request());
		} catch (Exception e) {
			Alert a = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
			a.showAndWait();
		}
	}
}
