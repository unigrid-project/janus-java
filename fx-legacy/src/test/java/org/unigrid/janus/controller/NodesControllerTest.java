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
import mockit.Mock;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.BeforeContainer;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.api.FxRobot;
import static org.testfx.matcher.control.TableViewMatchers.hasTableCell;
import static org.testfx.matcher.control.TextMatchers.hasText;
import org.unigrid.janus.jqwik.fx.BaseFxTest;
import org.unigrid.janus.jqwik.fx.FxResource;
import org.unigrid.janus.model.external.JaxrsResponseHandler;
import org.unigrid.janus.model.external.ResponseMockUp;
import org.unigrid.janus.model.rpc.entity.GridnodeEntity;
import org.unigrid.janus.model.rpc.entity.GridnodeList;
import org.unigrid.janus.model.service.DaemonMockUp;
import org.unigrid.janus.model.FileBasedConfigurationMockup;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.external.JerseyInvocationMockUp;
import org.unigrid.janus.model.service.external.WebTargetMockUp;
import org.unigrid.janus.view.MainWindow;

@FxResource(clazz = MainWindow.class, name = "mainWindow.fxml")
public class NodesControllerTest extends BaseFxTest {
	@Inject
	private FxRobot robot;

	@Inject
	private RPCService rpc;

	@BeforeContainer
	public static void before() {
		new JerseyInvocationMockUp();
		new WebTargetMockUp();
		new FileBasedConfigurationMockup();

		new DaemonMockUp();
		System.out.println("Before container");
		new ResponseMockUp() {
			@Mock
			public <T> T readEntity(Class<T> clazz) {
				T e = readEntities(clazz);
				if (Objects.isNull(e)) {
					if (clazz.equals(GridnodeList.class)) {
						return (T) JaxrsResponseHandler.handle(GridnodeList.class,
							new ArrayList<GridnodeList.Result>() {
							}.getClass().getGenericSuperclass(),
							() -> "list_gridnodes_outputs.json");
					}
					if (clazz.equals(GridnodeEntity.class)) {
						return (T) JaxrsResponseHandler.handle(GridnodeEntity.class,
							String.class, () -> "get_new_gridnode.json");
					}
				}
				return e;
			}
		};

	}

	@Example
	public void shouldShowNodesOnRefreshClicked() {
		String alias = "alias-test";
		String address = "ABtm9BuS3t9eZaf4G7HitmzXEMPL3P7McZ";
		String status = "online";

		robot.clickOn("#btnNodes");
		robot.clickOn("#btnNodeRefresh");

		await().until(() -> robot.lookup("#tblGridnodes")
			.queryAll().iterator().next() != null);

		TableView tb = (TableView) robot.lookup("#tblGridnodes").queryAll().iterator().next();
		GridnodeList.Result nodes = (GridnodeList.Result) tb.getItems().get(0);

		await().until(() -> nodes != null);

		verifyThat("#tblGridnodes", hasTableCell(alias));
		verifyThat("#tblGridnodes", hasTableCell(address));
		verifyThat("#tblGridnodes", hasTableCell(status));
		assertThat(alias, equalTo(nodes.getAlias()));
		assertThat(address, equalTo(nodes.getAddress()));
		assertThat(status, equalTo(nodes.getStatus()));
	}

	@Example
	public void shouldGenerateNodeAndCopyKey() {
		final String node = "2xvxSQDH3Jdg2QHikqtbvCbtXqvk8PZisnTt3UHNv7H869zcdx9";
		Clipboard clipboard = Clipboard.getSystemClipboard();

		robot.clickOn("#btnNodes");
		robot.clickOn("#btnNodeSetup");
		robot.clickOn("#btnNodeSetupGenerate");

		await().until(() -> robot.lookup("#gridnodeDisplay")
			.queryAll().iterator().next() != null);

		verifyThat("#gridnodeDisplay", hasText(node));
		Text nodeText = (Text) robot.lookup("#gridnodeDisplay").queryAll().iterator().next();
		assertThat(nodeText.getText(), equalTo(node));

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				assertThat(clipboard.getString(), clipboard.getString().contains(node));
			}
		});

		robot.clickOn("#btnNodeSetupCopy");
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				assertThat(clipboard.getString(), clipboard.getString().contains(node));
			}
		});

		robot.clickOn("#btnNodeSetupClear");
		verifyThat("#gridnodeDisplay", hasText(""));

		robot.clickOn("#btnNodeSetupGenerate");
		robot.clickOn("#btnNodeSetupClose");
		verifyThat("#gridnodeDisplay", hasText(""));
	}
}
