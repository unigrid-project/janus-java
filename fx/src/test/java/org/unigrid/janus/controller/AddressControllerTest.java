/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

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

import jakarta.inject.Inject;
import javafx.stage.Stage;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.Disabled;
import net.jqwik.api.Example;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.api.FxRobot;
import static org.testfx.matcher.control.TableViewMatchers.hasTableCell;
import org.unigrid.janus.jqwik.fx.BaseFxTest;
import org.unigrid.janus.jqwik.fx.FxResource;
import org.unigrid.janus.jqwik.fx.FxStart;
import org.unigrid.janus.jqwik.fx.FxStop;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.external.ResponseMockUp;
import org.unigrid.janus.model.service.DaemonMockUp;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.external.JerseyInvocationMockUp;
import org.unigrid.janus.model.service.external.WebTargetMockUp;
import org.unigrid.janus.view.MainWindow;

@FxResource(clazz = MainWindow.class, name = "mainWindow.fxml")
public class AddressControllerTest extends BaseFxTest {
	@Inject
	private FxRobot robot;

	@Inject
	private RPCService rpc;

	@FxStart // TODO: Remove this method
	public void startWithStage(Stage stage) {
		System.out.println("I take a stage!");
		System.out.println("I'm starting a test run and I am only here for demo purposes!");
	}

	@FxStart // TODO: Remove this method
	public void start() {
		System.out.println("I do not take a stage...");
		System.out.println("I'm starting a test run and I am only here for demo purposes!");
	}

	@FxStop // TODO: Remove this method
	public void stop() {
		System.out.println("I'm stopping a test run and I am only here for demo purposes!");
	}

	@BeforeContainer
	private static void before() {
		new ResponseMockUp();
		new JerseyInvocationMockUp();
		new WebTargetMockUp();
		new DaemonMockUp();
	}

	@Example @Disabled
	public void shouldHideZeroBalances() {
		Eager.instantiate(rpc);
		waitForScene();

		robot.clickOn("#btnAddress");
		robot.clickOn("#chkAddress");
		verifyThat("#tblAddresses", hasTableCell(2000.0));

		robot.clickOn("#chkAddress");
		verifyThat("#tblAddresses", hasTableCell(0.0));
		verifyThat("#tblAddresses", hasTableCell(2000.0));
	}
}
