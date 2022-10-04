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
import java.util.ArrayList;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import mockit.Mock;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.BeforeContainer;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.api.FxRobot;
import static org.testfx.matcher.control.TableViewMatchers.hasNumRows;
import static org.testfx.matcher.control.TableViewMatchers.hasTableCell;
import static org.testfx.matcher.control.TextMatchers.hasText;
import org.unigrid.janus.jqwik.fx.BaseFxTest;
import org.unigrid.janus.jqwik.fx.FxResource;
import org.unigrid.janus.jqwik.fx.FxStart;
import org.unigrid.janus.jqwik.fx.FxStop;
import org.unigrid.janus.model.external.JaxrsResponseHandler;
import org.unigrid.janus.model.external.ResponseMockUp;
import org.unigrid.janus.model.rpc.entity.GetNewAddress;
import org.unigrid.janus.model.rpc.entity.ListAddressBalances;
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
		new JerseyInvocationMockUp();
		new WebTargetMockUp();
		new DaemonMockUp();

		new ResponseMockUp() {
			@Mock
			public <T> T readEntity(Class<T> clazz) {
				T e = readEntities(clazz);
				if (Objects.isNull(e)) {
					if (clazz.equals(ListAddressBalances.class)) {
						return (T) JaxrsResponseHandler.handle(ListAddressBalances.class,
							new ArrayList<ListAddressBalances.Result>() {
							}.getClass().getGenericSuperclass(),
							() -> "list_address_balances.json");
					}
					if (clazz.equals(GetNewAddress.class)) {
						return (T) JaxrsResponseHandler.handle(GetNewAddress.class,
							String.class, () -> "get_new_address.json");
					}
				}
				return e;
			}
		};
	}

	@Example
	public void shouldGenerateAddressAndCopyAddress() {
		final String addr = "A7HitmzXEMPL3P7McZABtm9BuS3t9eZaf4";
		Clipboard clipboard = Clipboard.getSystemClipboard();

		robot.clickOn("#btnAddress");
		robot.clickOn("#btnGenerateAddress");

		await().until(() -> robot.lookup("#addressDisplay").queryAll().iterator().next() != null);

		verifyThat("#addressDisplay", hasText(addr));
		Text addrText = (Text) robot.lookup("#addressDisplay").queryAll().iterator().next();
		assertThat(addrText.getText(), equalTo(addr));

		robot.clickOn("#btnNewAddressCopy");
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				assertThat(clipboard.getString(), clipboard.getString().contains(addr));
			}
		});

		robot.clickOn("#btnNewAddressClear");
		verifyThat("#addressDisplay", hasText(""));
	}

	@Example
	public void shouldHideZeroBalances() {
		robot.clickOn("#btnAddress");
		robot.clickOn("#chkAddress");

		verifyThat("#tblAddresses", hasNumRows(1));
		verifyThat("#tblAddresses", hasTableCell(2000.0));

		robot.clickOn("#chkAddress");
		verifyThat("#tblAddresses", hasNumRows(2));
		verifyThat("#tblAddresses", hasTableCell(0.0));
		verifyThat("#tblAddresses", hasTableCell(2000.0));
	}

	@Example
	public void shouldSortAddresses() {
		robot.clickOn("#btnAddress");
		robot.clickOn("#chkAmountSort");

		await().until(() -> robot.lookup("#tblAddresses").queryAll().iterator().next() != null);

		TableView tb = (TableView) robot.lookup("#tblAddresses").queryAll().iterator().next();
		ListAddressBalances.Result addrFirst = (ListAddressBalances.Result) tb.getItems().get(0);
		ListAddressBalances.Result addrLast = (ListAddressBalances.Result) tb.getItems().get(1);

		assertThat("A7HitmzXEMPL3P7McZABtm9BuS3t9eZaf4", equalTo(addrFirst.getAddress()));
		assertThat("B7rq85k4BCaUC6Ksiw27EyPV4Vw5ehpahy", equalTo(addrLast.getAddress()));

		robot.clickOn("#chkAmountSort");

		tb = (TableView) robot.lookup("#tblAddresses").queryAll().iterator().next();
		addrFirst = (ListAddressBalances.Result) tb.getItems().get(0);
		addrLast = (ListAddressBalances.Result) tb.getItems().get(1);

		assertThat("B7rq85k4BCaUC6Ksiw27EyPV4Vw5ehpahy", equalTo(addrFirst.getAddress()));
		assertThat("A7HitmzXEMPL3P7McZABtm9BuS3t9eZaf4", equalTo(addrLast.getAddress()));
	}
}
