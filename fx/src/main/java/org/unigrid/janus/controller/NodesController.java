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

import com.github.javafaker.Faker;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.File;
import static java.lang.Integer.parseInt;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.Gridnode;
import org.unigrid.janus.model.NodeStatus;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.GetNewAddress;
import org.unigrid.janus.model.rpc.entity.GridnodeEntity;
import org.unigrid.janus.model.rpc.entity.GridnodeList;
import org.unigrid.janus.model.rpc.entity.SendMany;
import org.unigrid.janus.model.rpc.entity.ValidateAddress;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.BrowserService;
import org.unigrid.janus.model.signal.NodeRequest;
import org.unigrid.janus.model.signal.OverlayRequest;
import org.unigrid.janus.model.signal.State;
import org.unigrid.janus.model.signal.UnlockRequest;
import org.unigrid.janus.view.backing.GridnodeListModel;
import org.unigrid.janus.view.backing.GridnodeTxidList;
import org.unigrid.janus.view.backing.NodeStatusList;
import org.unigrid.janus.view.backing.OsxUtils;

@ApplicationScoped
public class NodesController implements Initializable, PropertyChangeListener {
	@Inject private BrowserService browser;
	@Inject private DebugService debug;
	@Inject private RPCService rpc;
	@Inject private Wallet wallet;
	@Inject private HostServices hostServices;
	@Inject private Event<State> stateEvent;
	@Inject private Event<UnlockRequest> unlockRequestEvent;
	@Inject private Event<OverlayRequest> overlayRequest;

	private static GridnodeListModel nodes = new GridnodeListModel();
	private static GridnodeTxidList txList = new GridnodeTxidList();
	private NodeStatusList nodeStatusList = new NodeStatusList();
	private final Clipboard clipboard = Clipboard.getSystemClipboard();
	private final ClipboardContent content = new ClipboardContent();

	private OsxUtils osxUtils = new OsxUtils();

	@FXML private Button btnNodeAdd;
	@FXML private Button btnNodeAddProgress;
	@FXML private Slider sldNodeAdd;
	@FXML private TextField vpsPassword;
	@FXML private TextField vpsAddress;
	@FXML private TextField tfNodeAdd;
	@FXML private TextField tfNodeAddAddress;
	@FXML private TextField tfNodeAddUsername;
	@FXML private TextField tfNodeAddPassword;
	@FXML private TextArea vpsOutput;
	@FXML private VBox vpsConect;
	@FXML private VBox genereateKeyPnl;
	@FXML private VBox pnlNodeAdd;
	@FXML private VBox pnlNodeAddProgress;
	@FXML private GridPane pnlNodeStatus;
	@FXML private TableView tblGridnodes;
	@FXML private TableView tblGridnodeKeys;
	@FXML private TableColumn tblTxInUse;
	@FXML private TableColumn colNodeStatus;
	@FXML private TableColumn colNodeAlias;
	@FXML private TableColumn colNodeAddress;
	@FXML private TableColumn colNodeTxhash;
	@FXML private HBox newGridnodeDisplay;
	@FXML private Text gridnodeDisplay;
	@FXML private Text txtNodeAddWarnMsg;

	private String serverResponse;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		setupNodeList();
		wallet.addPropertyChangeListener(this);

		Platform.runLater(() -> {
			try {
				vpsConect.setVisible(false);
				vpsOutput.textProperty().addListener(new ChangeListener() {
					public void changed(ObservableValue ov, Object oldValue, Object newValue) {
						vpsOutput.setScrollTop(Double.MAX_VALUE);    //top
						//vpsOutput.setScrollTop(Double.MIN_VALUE);   //down
					}
				});
				sldNodeAdd.valueProperty().addListener(new ChangeListener<Number>() {
					@Override
					public void changed(
						ObservableValue<? extends Number> ov,
						Number oldValue,
						Number newValue) {
						tfNodeAdd.textProperty().setValue(String.valueOf(newValue.intValue()));
					}
				});
				tfNodeAdd.textProperty().addListener(new ChangeListener() {
					public void changed(
						ObservableValue ov,
						Object oldValue,
						Object newValue) {
						boolean isNumber = false;
						try {
							Integer.parseInt((String) newValue);
							isNumber = true;
						} catch (NumberFormatException e) {
							isNumber = false;
							if ((String) newValue == "") {
								tfNodeAdd.textProperty().setValue("0");
							} else {
								tfNodeAdd.textProperty().setValue((String) oldValue);
							}
						} catch (NullPointerException e) {
							isNumber = false;
							tfNodeAdd.textProperty().setValue("0");
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (isNumber) {
							sldNodeAdd.setValue(Integer.parseInt((String) newValue));
							BigDecimal maxNodes = wallet.getBalance().divide(
								new BigDecimal(3000), 0, RoundingMode.DOWN);

							if (maxNodes.intValue() < Integer.parseInt((String) newValue)) {
								tfNodeAdd.textProperty().setValue(maxNodes.toString());
							} else if (Integer.parseInt((String) newValue) > 100) {
								tfNodeAdd.textProperty().setValue("100");
							}
							if (Integer.parseInt((String) newValue) < 0) {
								tfNodeAdd.textProperty().setValue("0");
							}
						}
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
			tblGridnodes.setItems(nodes.getGridnodes());
			colNodeStatus.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("status"));
			colNodeAlias.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("alias"));
			colNodeAddress.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("address"));

			colNodeTxhash.setCellValueFactory(cell -> {
				Gridnode gridnode = ((TableColumn.CellDataFeatures<Gridnode, Hyperlink>) cell).getValue();
				String text = gridnode.getTxhash() + " " + gridnode.getOutputidx();
				Hyperlink link = new Hyperlink();
				link.setText(text);

				link.setOnAction(e -> {
					if (e.getTarget().equals(link)) {
						browser.navigateTransaction(gridnode.getTxhash());
					}
				});

				Button btn = new Button();
				FontIcon fontIcon = new FontIcon("fas-clipboard");
				fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
				btn.setGraphic(fontIcon);

				btn.setOnAction(e -> {
					final Clipboard cb = Clipboard.getSystemClipboard();
					final ClipboardContent content1 = new ClipboardContent();

					content1.putString(text);
					cb.setContent(content1);

					if (SystemUtils.IS_OS_MAC_OSX) {
						Notifications
							.create()
							.title("Key copied to clipboard")
							.text(gridnode.getTxhash())
							.position(Pos.TOP_RIGHT)
							.showInformation();
					} else {
						Notifications
							.create()
							.title("Key copied to clipboard")
							.text(gridnode.getTxhash())
							.showInformation();
					}
				});

				link.setGraphic(btn);
				link.setAlignment(Pos.CENTER_RIGHT);

				return new ReadOnlyObjectWrapper(link);
			});

			tblTxInUse.setCellValueFactory(cell -> {
				final Gridnode txList = ((CellDataFeatures<Gridnode, Hyperlink>) cell).getValue();
				Button btn = new Button();
				boolean used = txList.isAvailableTxhash();
				btn.setStyle("-fx-cursor: hand");
				FontIcon fontIcon = new FontIcon("far-check-circle");

				if (used) {
					fontIcon = new FontIcon("fas-ban");
					fontIcon.setIconColor(setColor(255, 0, 0, 10));
					btn.setTooltip(new Tooltip("txhash is already in use"));
				} else {
					fontIcon.setIconColor(setColor(48, 186, 69, 10));
					btn.setTooltip(new Tooltip("txhash is available for use"));
				}
				btn.setGraphic(fontIcon);
				return new ReadOnlyObjectWrapper(btn);
			});
		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup node table) %s", e.getMessage()));
		}
	}

	private Color setColor(int r, int g, int b, int number) {
		return Color.rgb(r, g, b, Math.min(1.0f, Math.max(0.8f, 0.8f))); //number * 0.1f)));
	}

	private void updateTerminal(String str) {
		serverResponse = serverResponse + str;
		vpsOutput.setText(serverResponse);
		vpsOutput.setScrollTop(Double.MAX_VALUE);
	}

	private void getNodeList() {
		debug.log("Loading gridnode list");

		stateEvent.fire(State.builder().working(true).build());
		GridnodeList result = rpc.call(new GridnodeList.Request(new Object[]{"list-conf"}), GridnodeList.class);
		nodes.setGridnodes(result);
		nodes.getGridnodes(); //TODO: Why this call?
		stateEvent.fire(State.builder().working(false).build());
	}

	@FXML
	private void onSetupClicked(MouseEvent event) {
		genereateKeyPnl.setVisible(true);
		loadAvailableInputsForGridnode();
	}

	@FXML
	private void onNodeAddClicked(MouseEvent event) {
		pnlNodeAdd.setVisible(true);
	}

	@FXML
	private void onNodeProgressClicked(MouseEvent event) {
		pnlNodeAddProgress.setVisible(true);
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
		loadAvailableInputsForGridnode();
	}

	@FXML
	private void onOpenGridnodeConfigClicked(MouseEvent e) throws NullPointerException {
		File gridnode = DataDirectory.getGridnodeFile();
		try {
			if (SystemUtils.IS_OS_MAC_OSX) {
				osxUtils.openFileOsx(DataDirectory.GRIDNODE_FILE);
			} else {
				hostServices.showDocument(gridnode.getAbsolutePath());
			}
		} catch (NullPointerException error) {
			debug.print(error.getMessage(), SettingsController.class.getSimpleName());
		}
	}

	public void loadAvailableInputsForGridnode() {
		try {
			GridnodeList result = rpc.call(new GridnodeList.Request(new Object[]{"outputs"}),
				GridnodeList.class
			);

			txList.setGridnodes(result);

			tblGridnodeKeys.setItems(txList.getGridnodes());
		} catch (Exception e) {
			debug.print("loadAvailableInputsForGridnode " + e.getMessage(),
				NodesController.class.getSimpleName());
		}
	}

	@FXML
	private void onCloseGridnodeClicked(MouseEvent event) {
		getNodeList();
		genereateKeyPnl.setVisible(false);
		newGridnodeDisplay.setVisible(false);
		gridnodeDisplay.setText("");
	}

	@FXML
	private void onCloseGridnodeAddClicked(MouseEvent event) {
		pnlNodeAdd.setVisible(false);
		tfNodeAdd.setText("");
		tfNodeAddAddress.setText("");
		tfNodeAddUsername.setText("");
		tfNodeAddPassword.setText("");
	}

	@FXML
	private void onCloseProgressClicked(MouseEvent event) {
		pnlNodeAddProgress.setVisible(false);
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

	@FXML
	private void onCopyScriptClicked(MouseEvent event) {
		content.putString("bash -ic \"$(wget -4qO- -o- raw.githubusercontent.com/unigrid-project/unigrid-installer/"
			+ "main/node_installer.sh)\"; source ~/.bashrc");
		clipboard.setContent(content);
		Notifications.create().title("Gridnode script copied to clipboard").showInformation();
	}

	private void copyToClipboard(String gridnode) {
		content.putString(gridnode);
		clipboard.setContent(content);
		Notifications.create().title("Gridnode copied to clipboard").text(gridnode).showInformation();
	}

	@FXML
	private void onRefreshNodeListPressed(MouseEvent e) {
		debug.log("Calling node list refresh");
		getNodeList();
	}

	@FXML
	private void onStartAllNodesPressed(MouseEvent e) {
		if (wallet.getLocked()) {
			System.out.println(wallet.getLocked());
			unlockRequestEvent.fire(UnlockRequest.builder().type(UnlockRequest.Type.FOR_GRIDNODE).build());
			overlayRequest.fire(OverlayRequest.OPEN);
		} else {
			eventNodeRequest(NodeRequest.START_MISSING);
		}
	}

	@FXML
	private void onNodeAddNodeClicked() {
		debug.log("Add nodes");
		if (wallet.getLocked()) {
			System.out.println(wallet.getLocked());
			unlockRequestEvent.fire(UnlockRequest.builder()
				.type(UnlockRequest.Type.FOR_GRIDNODE).build());
			overlayRequest.fire(OverlayRequest.OPEN);

			return;
		}

		String host = tfNodeAddAddress.getText();
		String username = tfNodeAddUsername.getText();
		String password = tfNodeAddPassword.getText();
		int node = parseInt(tfNodeAdd.getText());

		if (tfNodeAddAddress.getText().equals("") && tfNodeAddAddress.getText() != null) {
			onErrorMessage("Please enter an address.");
			return;
		}
		if (tfNodeAddUsername.getText().equals("") && tfNodeAddUsername.getText() != null) {
			onErrorMessage("Please enter a username.");
			return;
		}
		if (tfNodeAddPassword.getText().equals("") && tfNodeAddPassword.getText() != null) {
			onErrorMessage("Please enter a password.");
			return;
		}
		if (node == 0) {
			onErrorMessage("Please enter an amount of nodes to setup.");
			return;
		}

		pnlNodeAddProgress.setVisible(true);
		tfNodeAddAddress.setText("");
		tfNodeAddUsername.setText("");
		tfNodeAddPassword.setText("");
		tfNodeAdd.setText("0");
		String name = new Faker().superhero().name();
		String txHash = "";
		Map<String, BigDecimal> mapAmount = new HashMap();

		for (int i = 0; i < node; i++) {
			GetNewAddress newAddress = rpc.call(new GetNewAddress.Request(""), GetNewAddress.class);
			final ValidateAddress call = rpc.call(new ValidateAddress.Request(newAddress
				.getResult().toString()), ValidateAddress.class);
			if (call.getError() != null) {
				System.out.println(String.format("ERROR: %s", call.getError()));
				debug.log(String.format("ERROR: %s", call.getError()));
			} else {
				mapAmount.put(newAddress.getResult(), new BigDecimal(3000));
			}
		}

		SendMany sendMany = rpc.call(new SendMany.Request("", mapAmount), SendMany.class);
		txHash = sendMany.getResult();

		ArrayList<Integer> ouputIndexList = new ArrayList<>();
		final String hash = txHash;

		Thread prepareNodeSetup = new Thread(() -> {
			while (ouputIndexList.size() < node) {
				GridnodeList result = rpc.call(new GridnodeList.Request(new Object[]{"outputs"}),
					GridnodeList.class
				);

				for (Gridnode hashNode : result.getResult()) {
					if (!hashNode.isAvailableTxhash()) {
						if (hashNode.getTxhash().equals(hash)) {
							ouputIndexList.add(hashNode.getOutputidx());
						}
					}
				}

				try {
					Thread.sleep(2000);
				} catch (InterruptedException ex) {
					Logger.getLogger(NodesController.class.getName())
						.log(Level.SEVERE, null, ex);
				}
			}
		});
		prepareNodeSetup.start();

		for (int j = 0; j < node; j++) {
			GridnodeEntity newGridnode = rpc.call(
				new GridnodeEntity.Request(new Object[]{"genkey"}),
				GridnodeEntity.class
			);

			NodeStatus nodeStatus = new NodeStatus();
			nodeStatus.setNodeNumber(j + 1);
			nodeStatus.setName(name);
			nodeStatus.setIpAddress(host);
			nodeStatus.setProgress(NodeStatus.INPROGRESS);
			nodeStatus.responseAddLine("Waiting block confirmation...\n");
			nodeStatusList.getSource().add(nodeStatus);
			getProgressList();

			final int pos = nodeStatusList.getSource().indexOf(nodeStatus);
			final int index = j;

			Thread newThread = new Thread(() -> {
				while (ouputIndexList.size() != node) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException ex) {
						Logger.getLogger(NodesController.class.getName())
							.log(Level.SEVERE, null, ex);
					}
				}

				//int port = 22;
				//int port = 444;
				int port = 22004;

				String wget = "wget -O- raw.githubusercontent.com/TimNhanTa/installer/master/node_installer.sh";
				//String wget = "wget -O- raw.githubusercontent.com/unigrid-project/unigrid-installer/main/node_installer.sh";
				String command = "echo " + password + " | sudo -S sudo echo >/dev/null && sudo "
					+ wget + " | sudo bash -s --"
					+ " -t \"" + hash + " " + ouputIndexList.get(index) + "\""
					+ " -k \"" + newGridnode.getResult() + "\""
					+ " -i \"" + (index + 1) + "\""
					+ " -n \"" + name.replaceAll(" ", "_").toLowerCase() + "\"";
				String commandType = "exec";

				sendCommandToVps(pos, host, port, username, password, command, commandType);

				nodeStatusList.getSource().remove(nodeStatus);
				Platform.runLater(
					() -> {
						getProgressList();
					});
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					Logger.getLogger(NodesController.class.getName())
						.log(Level.SEVERE, null, ex);
				}
			});
			newThread.start();
		}
	}

	private void getProgressList() {
		debug.log("Loading progress list");

		pnlNodeStatus.setVisible(nodeStatusList.getSource().size() != 0);
		pnlNodeStatus.getChildren().clear();
		int row = 0;
		pnlNodeStatus.add(new Label("Name"), 0, row, 1, 1);
		pnlNodeStatus.add(new Label("Address"), 1, row, 1, 1);
		pnlNodeStatus.add(new Label("Progress"), 2, row, 1, 1);
		pnlNodeStatus.add(new Label("Output"), 3, row, 1, 1);
		row++;

		for (NodeStatus progress : nodeStatusList.getSource()) {
			pnlNodeStatus.add(new Label(progress.getFullDescription()), 0, row, 1, 1);
			pnlNodeStatus.add(new Label(progress.getIpAddress()), 1, row, 1, 1);
			pnlNodeStatus.add(new ProgressBar(progress.getProgress()), 2, row, 1, 1);
			Button btn = new Button("");
			String iconCode = progress.isShowOutput() ? "fas-arrow-down" : "fas-arrow-right";
			FontIcon fontIcon = new FontIcon(iconCode);
			fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
			btn.setGraphic(fontIcon);
			btn.setOnAction(e -> {
				progress.setShowOutput(!progress.isShowOutput());
				getProgressList();
			});
			pnlNodeStatus.add(btn, 3, row, 1, 1);
			row++;

			TextArea ta = new TextArea();
			ta.setText(progress.getOutputAsString());
			ta.positionCaret(progress.getOutputAsString().length());
			ta.setEditable(false);
			ta.setWrapText(true);
			ta.setMinHeight(NodeStatus.OUTPUT_HEIGHT);
			ta.setMinWidth(NodeStatus.OUTPUT_WIDTH);
			ta.setVisible(progress.isShowOutput());
			ta.setManaged(progress.isShowOutput());
			pnlNodeStatus.add(ta, 0, row, NodeStatus.GRIDPANE_MAX_COLUMN, 1);
			row++;
		}
	}

	@RequiredArgsConstructor
	public static class InterceptingOutputStream extends ByteArrayOutputStream {
		private final Consumer<String> consumer;

		@Override
		public synchronized void write(byte[] b, int off, int len) {
			for (String line : new String(b, off, len).split("\n")) {
				consumer.accept(line);
			}
		}
	}

	public void sendCommandToVps(int i, String host, int port, String username, String password, String command,
		String commandType) {
		Session session = null;
		ChannelExec channel = null;

		try {
			session = new JSch().getSession(username, host, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			channel = (ChannelExec) session.openChannel(commandType);
			channel.setCommand(command);

			channel.setOutputStream(new InterceptingOutputStream(s -> {
				System.out.println(i + ": " + s);
				final String output = s.replaceAll("\u001B\\[[;\\d]*m", "").trim();

				nodeStatusList.getSource().get(i).responseAddLine(output + "\n");
				checkStepAndProgressFromOutput(nodeStatusList.getSource().get(i), output);
				if (checkStringIsGridnodeOutput(output)) {
					String[] split = output.split(" ");
					rpc.call(new GridnodeEntity.Request(new Object[]{"add-conf", split[0], split[1], split[2], split[3], split[4]}), GridnodeEntity.class);
				}
				Platform.runLater(() -> {
					getProgressList();
				});
			}));

			channel.connect();

			Thread.sleep(2700000);

			session.disconnect();
		} catch (InterruptedException ex) {
			Logger.getLogger(NodesController.class.getName()).log(Level.SEVERE, null, ex);
		} catch (JSchException ex) {
			pnlNodeAddProgress.setVisible(false);

			if (ex.getCause() != null && (ex.getCause().getMessage().contains("Auth fail")
				|| ex.getCause().getMessage().equals("Connection refused"))) {
				onErrorMessage(ex.getCause().getMessage());
			} else {
				onErrorMessage("Authentication failed");
			}

			Logger.getLogger(NodesController.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if (session != null) {
				session.disconnect();
			}
			if (channel != null) {
				channel.disconnect();
			}
		}
	}

	public void checkStepAndProgressFromOutput(NodeStatus status, String output) {
		String stepOutputs[] = new String[]{"", "Unigrid sync status", "Loading the Unigrid backend"};
		String stepDescription[] = new String[]{"Copy Volume", "Sync Unigrid", "Loading Backend"};
		double progress = NodeStatus.INPROGRESS;

		String normalSpacedString = StringUtils.normalizeSpace(output);
		String stringRegex = "[0-9]+\\.[0-9]+\\w+ ([0-9]+)% [0-9]+\\.[0-9]+\\w+\\/\\w+ [0-9]+:[0-9]+:[0-9]+";

		Pattern patternVolume = Pattern.compile(stringRegex);
		Matcher matcher = patternVolume.matcher(normalSpacedString);

		if (matcher.find()) {
			status.setStep(1);
			status.setStepDescription(stepDescription[0]);

			try {
				if (!matcher.group(1).equals("")) {
					progress = Integer.parseInt(matcher.group(1)) / 100.0;
					status.setProgress(progress);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for (int i = 1; i < stepOutputs.length; i++) {
			if (output.contains(stepOutputs[i])) {
				status.setStep(i + 1);
				status.setStepDescription(stepDescription[i]);
				if (i == 1) {
					String[] split = output.split("\\.");
					String clean = split[split.length - 2].replaceAll("\\D+", "");

					try {
						if (!clean.equals("")) {
							progress = Integer.parseInt(clean) / 100.0;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (i == 2) {
					String clean = output.replaceAll("\\D+", "");

					try {
						if (!clean.equals("")) {
							progress = ((Integer.parseInt(clean) + 1) * 3.34) / 100;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				status.setProgress(progress);
			}
		}
	}

	public boolean checkStringIsGridnodeOutput(String str) {
		String[] split = str.split(" ");
		String pattern[] = new String[]{"ugd_docker_[0-9]+", "[0-9]+(\\.[0-9]+){3}:[0-9]+", "[A-Za-z0-9]{51}", "[A-Za-z0-9]{64}", "[0-9]+"};
		boolean exactListSize = split.length == 5;

		if (!exactListSize) {
			return false;
		}

		boolean match = exactListSize;

		for (int i = 0; i < split.length; i++) {
			match &= Pattern.matches(pattern[i], split[i]);
		}

		return match;
	}

	private void onErrorMessage(String message) {
		txtNodeAddWarnMsg.setFill(Color.RED);
		txtNodeAddWarnMsg.setText(message);
		txtNodeAddWarnMsg.setVisible(true);
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

	private void eventNodeRequest(@Observes NodeRequest nodeRequest) {
		rpc.callToJson(new GridnodeEntity.Request(new Object[]{"start-missing", "0"}));
		getNodeList();
		debug.log("Attempting to start nodes");
	}
}
