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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jakarta.enterprise.context.ApplicationScoped;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.controlsfx.control.Notifications;
import org.unigrid.janus.model.Gridnode;
import org.unigrid.janus.model.GridnodeListModel;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.GridnodeEntity;
import org.unigrid.janus.model.rpc.entity.GridnodeList;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;

@ApplicationScoped
public class NodesController implements Initializable, PropertyChangeListener {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static WindowService window = WindowService.getInstance();
	private static GridnodeListModel nodes = new GridnodeListModel();
	private final Clipboard clipboard = Clipboard.getSystemClipboard();
	private final ClipboardContent content = new ClipboardContent();

	private Wallet wallet;

	@FXML
	private TextField vpsPassword;
	@FXML
	private TextField vpsAddress;
	@FXML
	private TextArea vpsOutput;
	@FXML
	private VBox vpsConect;
	@FXML
	private VBox genereateKeyPnl;
	@FXML
	private TableView tblGridnodes;
	@FXML
	private TableView tblGridnodeKeys;
	@FXML
	private TableColumn colNodeStatus;
	@FXML
	private TableColumn colNodeAlias;
	@FXML
	private TableColumn colNodeAddress;
	@FXML
	private TableColumn colNodeStart;
	@FXML
	private TableColumn colNodeKey;
	@FXML
	private HBox newGridnodeDisplay;
	@FXML
	private Text gridnodeDisplay;

	private String serverResponse;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		wallet = window.getWallet();
		setupNodeList();
		nodes.addPropertyChangeListener(this);
		wallet.addPropertyChangeListener(this);
		window.setNodeController(this);
		Platform.runLater(() -> {
			try {
				vpsConect.setVisible(false);
				vpsOutput.textProperty().addListener(new ChangeListener() {
					public void changed(ObservableValue ov, Object oldValue, Object newValue) {
						vpsOutput.setScrollTop(Double.MAX_VALUE);    //top
						//vpsOutput.setScrollTop(Double.MIN_VALUE);   //down
					}
				});
				// this.getNodeList();
			} catch (Exception e) {
				debug.log(String.format("ERROR: (gridnode init) %s", e.getMessage()));
			}
		});
	}

	private void setupNodeList() {
		try {
			colNodeStatus.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("status"));
			colNodeAlias.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("alias"));
			colNodeAddress.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("address"));

		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup node table) %s", e.getMessage()));
		}
	}

	private void updateTerminal(String str) {
		serverResponse = serverResponse + str;
		vpsOutput.setText(serverResponse);
		vpsOutput.setScrollTop(Double.MAX_VALUE);
	}

	private void getNodeList() {
		debug.log("Loading gridnode list");
		window.getWindowBarController().startSpinner();
		GridnodeList result = rpc.call(new GridnodeList.Request(new Object[]{"list-conf"}), GridnodeList.class);
		nodes.setGridnodes(result);
		window.getWindowBarController().stopSpinner();
		//debug.log(String.format("gridnode result: %s", nodes.getGridnodes()));
	}

	@FXML
	private void onGenerateKeyClicked(MouseEvent event) {
		genereateKeyPnl.setVisible(true);
	}

	@FXML
	private void onGenerateNewKeyClicked(MouseEvent e) {
		GridnodeEntity newGridnode = rpc.call(
			new GridnodeEntity.Request(new Object[]{"genkey"}),
			GridnodeEntity.class
		);
		gridnodeDisplay.setText(newGridnode.getResult().toString());
		newGridnodeDisplay.setVisible(true);
		copyToClipboard(gridnodeDisplay.getText());
	}

	@FXML
	private void onCloseGridnodeClicked(MouseEvent event) {
		genereateKeyPnl.setVisible(false);
		newGridnodeDisplay.setVisible(false);
		gridnodeDisplay.setText("");
	}

	@FXML
	private void onClearGridnodeClicked(MouseEvent event) {
		newGridnodeDisplay.setVisible(false);
		gridnodeDisplay.setText("");
	}

	@FXML
	private void onCopyToClipboardClicked(MouseEvent event) {
		copyToClipboard(gridnodeDisplay.getText());
	}

	private void copyToClipboard(String gridnode) {
		content.putString(gridnode);
		clipboard.setContent(content);
		Notifications
			.create()
			.title("Gridnode copied to clipboard")
			.text(gridnode)
			.showInformation();
	}

	@FXML
	private void onRefreshNodeListPressed(MouseEvent e) {
		debug.log("Calling node list refresh");
		getNodeList();
	}

	@FXML
	private void onStartAllNodesPressed(MouseEvent e) {
		if (wallet.getLocked()) {
			window.getMainWindowController().unlockForGridnode();
		} else {
			startMissingNodes();
		}
	}

	public void startMissingNodes() {
		rpc.callToJson(new GridnodeEntity.Request(new Object[]{"start-missing", "0"}));
		getNodeList();
		debug.log("ATTEMPTING TO START NODES");
	}

	@FXML
	public void connectToVps() throws Exception {
		int port = 22;
		Session session = null;
		Channel channel = null;
		ChannelShell shell = null;
		try {
			session = new JSch().getSession("root", vpsAddress.getText(), port);
			session.setPassword(vpsPassword.getText());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();
			channel = session.openChannel("shell");

			OutputStream inputStream = channel.getOutputStream();
			PrintStream commander = new PrintStream(inputStream, true);

			channel.setOutputStream(System.out, true);

			channel.connect();

			commander.println("ls -la");
			commander.close();
			readChannelOutput(channel);
			commander.close();

			do {
				Thread.sleep(1000);
			} while (!channel.isEOF());

			session.disconnect();

		} finally {
			if (session != null) {
				session.disconnect();
			}
			if (channel != null) {
				channel.disconnect();
			}
		}
	}

	private void readChannelOutput(Channel channel) {

		byte[] buffer = new byte[1024];

		try {
			InputStream in = channel.getInputStream();
			String line = "";
			while (true) {
				while (in.available() > 0) {
					int i = in.read(buffer, 0, 1024);
					if (i < 0) {
						break;
					}
					line = new String(buffer, 0, i);
					if (line.contains("txhash:")) {
						debug.log("ENTER COMMAND HERE");
					}
					updateTerminal(line);
				}

				if (line.contains("logout")) {
					break;
				}

				if (channel.isClosed()) {
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
		} catch (Exception e) {
			System.out.println("Error while reading channel output: " + e);
		}

	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(nodes.GRIDNODE_LIST)) {
			tblGridnodes.setItems(nodes.getGridnodes());
		}
		// after wallet is done loading, load the gridnodes.
		if (event.getPropertyName().equals(wallet.STATUS_PROPERTY)) {
			debug.log("loading gridnode list");
			getNodeList();
		}
	}
}
