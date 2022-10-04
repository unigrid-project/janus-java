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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Objects;
import javafx.geometry.VerticalDirection;
import javafx.scene.control.TableView;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import net.jqwik.api.Disabled;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.BeforeContainer;
import static org.awaitility.Awaitility.await;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.api.FxRobot;
import static org.testfx.matcher.control.TableViewMatchers.hasTableCell;
import org.unigrid.janus.jqwik.fx.BaseFxTest;
import org.unigrid.janus.jqwik.fx.FxResource;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.external.JaxrsResponseHandler;
import org.unigrid.janus.model.external.ResponseMockUp;
import org.unigrid.janus.model.rpc.entity.GetBlockCount;
import org.unigrid.janus.model.rpc.entity.GetConnectionCount;
import org.unigrid.janus.model.rpc.entity.GetUnlockState;
import org.unigrid.janus.model.rpc.entity.GetWalletInfo;
import org.unigrid.janus.model.rpc.entity.GridnodeList;
import org.unigrid.janus.model.rpc.entity.ListAddressBalances;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.rpc.entity.StakingStatus;
import org.unigrid.janus.model.service.DaemonMockUp;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.external.JerseyInvocationMockUp;
import org.unigrid.janus.model.service.external.WebTargetMockUp;
import org.unigrid.janus.view.MainWindow;

@FxResource(clazz = MainWindow.class, name = "mainWindow.fxml")
public class TransactionsControllerTest extends BaseFxTest {

	@Inject
	private FxRobot robot;

	@Inject
	private RPCService rpc;

	@Inject
	private Wallet wallet;

	@Mocked
	private DebugService debug;

	@BeforeContainer
	public static void before() {
		new JerseyInvocationMockUp();
		new WebTargetMockUp();
		new DaemonMockUp();

		new ResponseMockUp() {
			@Mock
			public <T> T readEntity(Class<T> clazz) {
				T e = readEntities(clazz);
				if (Objects.isNull(e)) {
					if (clazz.equals(StakingStatus.class)) {
						return (T) JaxrsResponseHandler.handle(StakingStatus.class,
							StakingStatus.Result.class, ()
							-> "staking_status_not_staking.json");
					}
					if (clazz.equals(GetWalletInfo.class)) {
						return (T) JaxrsResponseHandler.handle(GetWalletInfo.class,
							GetWalletInfo.Result.class, ()
							-> "get_wallet_info_unlocked.json");
					}
					if (clazz.equals(GetUnlockState.class)) {
						return (T) JaxrsResponseHandler.handle(GetUnlockState.class,
							GetUnlockState.Result.class, () -> "get_unlock_state.json");
					}
					if (clazz.equals(ListTransactions.class)) {
						return (T) JaxrsResponseHandler.handle(ListTransactions.class,
							new ArrayList<ListTransactions.Result>() {
							}.getClass().getGenericSuperclass(),
							() -> "list_transactions_scroll.json");
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
							String.class, () -> "get_block_count.json");
					}
					if (clazz.equals(GetConnectionCount.class)) {
						return (T) JaxrsResponseHandler.handle(GetConnectionCount.class,
							String.class, () -> "getconnectioncount.json");
					}
				}

				new MockUp<Wallet>() {
					@Mock
					public void setBalance(BigDecimal newValue) {
					}

					@Mock
					public BigDecimal getBalance(BigDecimal newValue) {
						return BigDecimal.ONE;
					}
				};

				new MockUp<DebugService>() {
					@Mock
					public void log(String msg) {
					}
				};

				return e;
			}
		};
	}

	@Example @Disabled
	public boolean shouldLoadTransactionsAndScroll() {
		rpc.pollForInfo(900000);
		await().until(() -> wallet != null && wallet.getStakingStatus() != null
			&& robot.lookup("#tblTransactions").queryAll().iterator().next() != null);

		TableView tb = (TableView) robot.lookup("#tblTransactions").queryAll().iterator().next();
		robot.clickOn("#btnTransactions");
		verifyThat("#tblTransactions", hasTableCell("20.00000000"));
		robot.moveTo("#tblTransactions");
		robot.scroll(20, VerticalDirection.DOWN);
		verifyThat("#tblTransactions", hasTableCell("20.00000000"));
		tb = (TableView) robot.lookup("#tblTransactions").queryAll().iterator().next();
		// await().until(() -> tb.getItems().size() > 20);
		rpc.stopPolling();
		return tb.getItems().size() > 20;
	}
}
