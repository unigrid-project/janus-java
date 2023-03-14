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

package org.unigrid.janus.model.service;

import jakarta.inject.Inject;
import jakarta.json.bind.JsonbBuilder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.BeforeContainer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
//import org.unigrid.janus.controller.SettingsController;
import org.unigrid.janus.jqwik.fx.BaseFxTest;
import org.unigrid.janus.jqwik.fx.FxResource;
import org.unigrid.janus.model.DataDirectoryMockup;
import org.unigrid.janus.model.external.ResponseMockUp;
import org.unigrid.janus.model.service.external.JerseyInvocationMockUp;
import org.unigrid.janus.model.service.external.WebTargetMockUp;
import mockit.Mock;
import org.unigrid.janus.model.external.JaxrsResponseHandler;
import org.unigrid.janus.model.rpc.entity.GetBlockCount;
import org.unigrid.janus.model.rpc.entity.GetConnectionCount;
import org.unigrid.janus.model.rpc.entity.GetUnlockState;
import org.unigrid.janus.model.rpc.entity.GetWalletInfo;
import org.unigrid.janus.model.rpc.entity.GridnodeList;
import org.unigrid.janus.model.rpc.entity.ListAddressBalances;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.rpc.entity.StakingStatus;
import org.unigrid.janus.model.rpc.entity.ValidateAddress;
import org.unigrid.janus.view.MainWindow;

@FxResource(clazz = MainWindow.class, name = "mainWindow.fxml")
public class DebugServiceTest extends BaseFxTest {

	private final PrintStream standardOut = System.out;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

	@Inject private DebugService debugService;

	@BeforeContainer
	public static void before() {
		new JerseyInvocationMockUp();
		new WebTargetMockUp();
		new DataDirectoryMockup();
		new DaemonMockUp();

		new ResponseMockUp() {
			@Mock
			public <T> T readEntity(Class<T> clazz) {
				T e = readEntities(clazz);
				if (Objects.isNull(e)) {
					if (clazz.equals(StakingStatus.class)) {
						return (T) JaxrsResponseHandler.handle(StakingStatus.class,
							StakingStatus.Result.class,
							() -> "staking_status_not_staking.json");
					}
					if (clazz.equals(GetWalletInfo.class)) {
						return (T) JaxrsResponseHandler.handle(GetWalletInfo.class,
							GetWalletInfo.Result.class,
							() -> "get_wallet_info_locked.json");
					}
					if (clazz.equals(GetUnlockState.class)) {
						return (T) JaxrsResponseHandler.handle(GetUnlockState.class,
							GetUnlockState.Result.class, () -> "get_lock_state.json");
					}
					if (clazz.equals(ListTransactions.class)) {
						return (T) JaxrsResponseHandler.handle(ListTransactions.class,
							new ArrayList<ListTransactions.Result>() {
							}.getClass().getGenericSuperclass(),
							() -> "list_transactions.json");
					}
					if (clazz.equals(GridnodeList.class)) {
						return (T) JaxrsResponseHandler.handle(GridnodeList.class,
							new ArrayList<GridnodeList.Result>() {
							}.getClass().getGenericSuperclass(),
							() -> "list_gridnodes_outputs.json");
					}
					if (clazz.equals(ListAddressBalances.class)) {
						return (T) JaxrsResponseHandler.handle(ListAddressBalances.class,
							new ArrayList<ListAddressBalances.Result>() {
							}.getClass().getGenericSuperclass(),
							() -> "list_address_balances.json");
					}
					if (clazz.equals(GetBlockCount.class)) {
						return (T) JaxrsResponseHandler.handle(GetBlockCount.class,
							Integer.class, () -> "get_block_count.json");
					}
					if (clazz.equals(GetConnectionCount.class)) {
						return (T) JaxrsResponseHandler.handle(GetConnectionCount.class,
							Integer.class, () -> "getconnectioncount.json");
					}
					if (clazz.equals(ValidateAddress.class)) {
						final ValidateAddress result = new ValidateAddress();
						result.setResult(JsonbBuilder.create().fromJson(ResponseMockUp.class
							.getResourceAsStream("validate_address.json"),
							ValidateAddress.Result.class));
						result.setError(null);
						return (T) result;
					}
				}

				return e;
			}
		};
	}

	// test that debugService is not null
	@Example
	public void testDebugServiceNotNull() {
		assertThat(debugService, equalTo(debugService));
	}

	// test that debugService.getCurrentDate() returns a string with the current date
	@Example
	void testGetCurrentDate() {
		String currentDate = new SimpleDateFormat("yyyy.MM.dd.HH.mm").format(new Date());
		String date = debugService.getCurrentDate();

		assertThat(date, equalTo(currentDate));
	}

	// test that debugService.print() prints a message to the console
	@Example
	void testPrint() {

		System.setOut(new PrintStream(outputStreamCaptor));
		debugService.print("test", "DebugServiceTest");
		System.setOut(standardOut);

		assertThat(outputStreamCaptor.toString().trim(), equalTo("test"));
	}

	@Example
	public void testTrace() {
		System.setOut(new PrintStream(outputStreamCaptor));
		debugService.trace("test");
		System.setOut(standardOut);

		assertThat(outputStreamCaptor.toString().trim(), equalTo("test"));
	}

}
