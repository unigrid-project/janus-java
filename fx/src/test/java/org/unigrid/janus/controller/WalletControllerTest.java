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
import jakarta.json.bind.JsonbBuilder;
import java.util.ArrayList;
import java.util.Objects;
import mockit.Mock;
import mockit.Mocked;
import net.jqwik.api.Disabled;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.BeforeContainer;
import static org.awaitility.Awaitility.await;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.api.FxRobot;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import org.testfx.matcher.control.TextInputControlMatchers;
import org.testfx.matcher.control.TextMatchers;
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
import org.unigrid.janus.model.rpc.entity.SendTransaction;
import org.unigrid.janus.model.rpc.entity.StakingStatus;
import org.unigrid.janus.model.rpc.entity.UnlockWallet;
import org.unigrid.janus.model.rpc.entity.ValidateAddress;
import org.unigrid.janus.model.service.DaemonMockUp;
import org.unigrid.janus.model.DataDirectoryMockup;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.external.JerseyInvocationMockUp;
import org.unigrid.janus.model.service.external.WebTargetMockUp;
import org.unigrid.janus.view.MainWindow;

@FxResource(clazz = MainWindow.class, name = "mainWindow.fxml")
public class WalletControllerTest extends BaseFxTest {
	@Inject
	private FxRobot robot;

	@Inject
	private static Wallet wallet;

	@Inject
	private static RPCService rpc;

	@Mocked
	private DebugService debug;

	private static boolean isStaking = false;
	private static boolean isLocked = false;

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
							GetWalletInfo.Result.class, () -> isLocked
								? "get_wallet_info_locked.json"
								: "get_wallet_info_unlocked.json");
					}
					if (clazz.equals(GetUnlockState.class)) {
						return (T) JaxrsResponseHandler.handle(GetUnlockState.class,
							GetUnlockState.Result.class, () -> isLocked
								? "get_lock_state.json" : "get_unlock_state.json");
					}
					if (clazz.equals(UnlockWallet.class)) {
						return (T) new UnlockWallet();
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
					if (clazz.equals(SendTransaction.class)) {
						return (T) new SendTransaction();
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

	@Example
	public void shouldSendTransaction() {
		rpc.pollForInfo(900000);
		await().until(() -> wallet != null && wallet.getStakingStatus() != null);

		robot.clickOn("#btnWalletTransaction");
		robot.clickOn("#amountToSend");
		robot.write("1");
		robot.clickOn("#ugdAddressTxt");
		robot.write("2xvx");
		robot.clickOn("#btnWalletTransactionSend");

		verifyThat("#sendWarnMsg", TextMatchers.hasText("TRANSACTION SENT!"));
		verifyThat("#ugdAddressTxt", TextInputControlMatchers.hasText(""));
		verifyThat("#amountToSend", TextInputControlMatchers.hasText(""));
		rpc.stopPolling();
	}

	@Example
	public void shouldUnlockAndSendTransaction() {
		isLocked = true;
		rpc.pollForInfo(900000);
		await().until(() -> wallet != null && wallet.getLocked() != null && wallet.getLocked().equals(true));

		robot.clickOn("#btnWalletTransaction");
		robot.clickOn("#amountToSend");
		robot.write("1");
		robot.clickOn("#ugdAddressTxt");
		robot.write("2xvx");
		robot.clickOn("#btnWalletTransactionSend");

		robot.clickOn("#passphraseInput");
		robot.write("hello");
		robot.clickOn("#submitBtn");

		verifyThat("#sendWarnMsg", TextMatchers.hasText("TRANSACTION SENT!"));
		verifyThat("#ugdAddressTxt", TextInputControlMatchers.hasText(""));
		verifyThat("#amountToSend", TextInputControlMatchers.hasText(""));
		rpc.stopPolling();
	}

	// TODO this test fails everytime
	@Disabled
	@Example
	public void shouldShowErrorMessageOnSendEmptyInputs() {
		robot.clickOn("#btnWalletTransaction");
		robot.clickOn("#btnWalletTransactionSend");

		verifyThat("#ugdAddressTxt", TextInputControlMatchers.hasText(""));
		verifyThat("#amountToSend", TextInputControlMatchers.hasText(""));
		await().until(() -> robot.lookup("#sendWarnMsg").queryText().getText()
			.equals("Please enter an amount of Unigrid to send."));

		robot.clickOn("#amountToSend");
		robot.write("1");
		robot.clickOn("#btnWalletTransactionSend");

		verifyThat("#sendWarnMsg", TextMatchers.hasText("Please enter a valid Unigrid address."));
	}

	@Example
	public void shouldClearInputsOnTransactionCancel() {
		robot.clickOn("#btnWalletTransaction");
		verifyThat("#sendTransactionPnl", isVisible());

		robot.clickOn("#amountToSend");
		robot.write("1");
		robot.clickOn("#ugdAddressTxt");
		robot.write("s");
		robot.clickOn("#btnWalletTransactionClose");

		verifyThat("#ugdAddressTxt", TextInputControlMatchers.hasText(""));
		verifyThat("#amountToSend", TextInputControlMatchers.hasText(""));
	}

	@Example
	public void shouldChangeTabOnReceiveClicked() {
		robot.clickOn("#btnWalletRecieve");
		verifyThat("#pnlAddress", isVisible());
	}
}
