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
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.BeforeContainer;
import static org.awaitility.Awaitility.await;
import org.kordamp.ikonli.javafx.FontIcon;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.api.FxRobot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
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
import org.unigrid.janus.model.rpc.entity.LockWallet;
import org.unigrid.janus.model.rpc.entity.StakingStatus;
import org.unigrid.janus.model.rpc.entity.UnlockWallet;
import org.unigrid.janus.model.service.DaemonMockUp;
import org.unigrid.janus.model.FileBasedConfigurationMockup;
import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.external.JerseyInvocationMockUp;
import org.unigrid.janus.model.service.external.WebTargetMockUp;
import org.unigrid.janus.view.MainWindow;

@FxResource(clazz = MainWindow.class, name = "mainWindow.fxml")
public class MainWindowControllerTest extends BaseFxTest {

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
		new FileBasedConfigurationMockup();
		new DaemonMockUp();

		new ResponseMockUp() {
			@Mock
			public <T> T readEntity(Class<T> clazz) {
				T e = readEntities(clazz);
				if (Objects.isNull(e)) {
					if (clazz.equals(StakingStatus.class)) {
						return (T) JaxrsResponseHandler.handle(StakingStatus.class,
							StakingStatus.Result.class, () -> isStaking
								? "staking_status_staking.json"
								: "staking_status_not_staking.json");
					}
					if (clazz.equals(GetWalletInfo.class)) {
						return (T) JaxrsResponseHandler.handle(GetWalletInfo.class,
							GetWalletInfo.Result.class, () -> isLocked
								? "get_wallet_info_locked.json"
								: "get_wallet_info_unlocked.json");
					}
					if (clazz.equals(Info.class)) {
						return (T) JaxrsResponseHandler.handle(Info.class,
							Info.Result.class,
							() -> "get_info.json");
					}
					if (clazz.equals(GetUnlockState.class)) {
						return (T) JaxrsResponseHandler.handle(GetUnlockState.class,
							GetUnlockState.Result.class, () -> isLocked
								? "get_lock_state.json" : "get_unlock_state.json");
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
					if (clazz.equals(LockWallet.class)) {
						return (T) new LockWallet();
					}
					if (clazz.equals(UnlockWallet.class)) {
						return (T) new UnlockWallet();
					}
					if (clazz.equals(GetBlockCount.class)) {
						return (T) JaxrsResponseHandler.handle(GetBlockCount.class,
							Integer.class, () -> "get_block_count.json");
					}
					if (clazz.equals(GetConnectionCount.class)) {
						return (T) JaxrsResponseHandler.handle(GetConnectionCount.class,
							Integer.class, () -> {
								int connections = wallet.getConnections();

								if (connections > 0 && connections < 5) {
									return "getconnectioncount.json";
								} else if (connections >= 5 && connections < 10) {
									return "get_medium_connection_count.json";
								} else if (connections >= 10) {
									return "get_high_connection_count.json";
								}

								return "get_no_connection_count.json";

							});
					}
				}
				return e;
			}
		};

		new MockUp<Wallet>() {
			@Mock
			public void setBalance(BigDecimal newValue) {
			}

			@Mock
			public BigDecimal getBalance(BigDecimal newValue) {
				return BigDecimal.ONE;
			}
		};
	}

	@Example
	public void shouldSwitchTabs() {
		robot.moveTo("#pnlWallet");
		verifyThat("#pnlWallet", isVisible());

		robot.clickOn("#btnDocs");
		verifyThat("#pnlDocs", isVisible());

		robot.clickOn("#btnAddress");
		verifyThat("#pnlAddress", isVisible());

		robot.clickOn("#btnNodes");
		verifyThat("#pnlNodes", isVisible());

		robot.clickOn("#btnTransactions");
		verifyThat("#pnlTransactions", isVisible());

		robot.clickOn("#btnWallet");
		verifyThat("#pnlWallet", isVisible());

		robot.clickOn("#btnSettings");
		verifyThat("#pnlSettings", isVisible());
	}

	@Example
	public void shouldShowIsStaking() {
		isStaking = true;
		rpc.pollForInfo(200000);

		await().until(() -> wallet != null && wallet.getStakingStatus() != null
			&& wallet.getStakingStatus().equals(true));

		rpc.stopPolling();
	}

	@Example
	public void shouldShowIsNotStaking() {
		isStaking = false;
		rpc.pollForInfo(200000);

		await().until(() -> wallet != null && wallet.getStakingStatus() != null
			&& wallet.getStakingStatus().equals(false));

		rpc.stopPolling();
	}

	@Example
	public void shouldUnlock() {
		isLocked = true;
		rpc.pollForInfo(200000);

		await().until(() -> wallet != null && wallet.getLocked() != null && wallet.getLocked());

		verifyThat("#lockBtn", isVisible());
		robot.clickOn("#lockBtn");

		verifyThat("#pnlOverlay", isVisible());
		robot.clickOn("#passphraseInput");

		robot.write("hello");

		robot.clickOn("#submitBtn");
		verifyThat("#unlockedBtn", isVisible());

		rpc.stopPolling();
	}

	@Example
	public void shouldLock() {
		isLocked = false;
		rpc.pollForInfo(200000);
		await().until(() -> wallet != null && wallet.getLocked() != null && !wallet.getLocked());

		verifyThat("#unlockedBtn", n -> n.isVisible());
		robot.clickOn("#unlockedBtn");
		verifyThat("#lockBtn", isVisible());

		rpc.stopPolling();
	}

	@Example
	public void shouldChangeSatelliteIconColor() {
		Paint color;
		FontIcon icon = (FontIcon) robot.lookup("#satelliteIcn").queryAll().iterator().next();

		wallet.setConnections(7);
		color = icon.getIconColor();
		assertThat(color.toString(), equalTo(Color.YELLOWGREEN.toString()));

		wallet.setConnections(99);
		color = icon.getIconColor();
		assertThat(color.toString(), equalTo(Color.GREEN.toString()));

		wallet.setConnections(-1);
		color = icon.getIconColor();
		assertThat(color.toString(), equalTo(Color.RED.toString()));
	}

}
