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
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.File;
import static java.lang.Integer.parseInt;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.FXMLInjectable;
import org.unigrid.janus.model.FXMLName;
import org.unigrid.janus.model.Gridnode;
import org.unigrid.janus.model.GridnodeDeployment;
import org.unigrid.janus.model.GridnodeDeployment.Authentication;
import static org.unigrid.janus.model.GridnodeDeployment.State.FIVE_POSTDEPLOYMENT;
import static org.unigrid.janus.model.GridnodeDeployment.State.FOUR_DEPLOYED;
import static org.unigrid.janus.model.GridnodeDeployment.State.ONE_PREDEPLOYING;
import static org.unigrid.janus.model.GridnodeDeployment.State.THREE_DEPLOYMENT;
import static org.unigrid.janus.model.GridnodeDeployment.State.TWO_PENDING;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.cdi.CDIUtil;
import org.unigrid.janus.model.rpc.entity.GridnodeEntity;
import org.unigrid.janus.model.rpc.entity.GridnodeList;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.BrowserService;
import org.unigrid.janus.model.service.GridnodeService;
import org.unigrid.janus.model.signal.NodeRequest;
import org.unigrid.janus.model.signal.NodeUpdate;
import org.unigrid.janus.model.signal.OverlayRequest;
import org.unigrid.janus.model.signal.State;
import org.unigrid.janus.model.signal.UnlockRequest;
import org.unigrid.janus.view.backing.GridnodeListModel;
import org.unigrid.janus.view.backing.GridnodeTxidList;
import org.unigrid.janus.view.backing.NodeStatusList;
import org.unigrid.janus.view.backing.OsxUtils;
import org.unigrid.janus.view.component.CustomGridPane;

@ApplicationScoped
public class NodesController implements Initializable, PropertyChangeListener {
	@Inject private BrowserService browser;
	@Inject private DebugService debug;
	@Inject private GridnodeService gridnodeService;
	@Inject private RPCService rpc;
	@Inject private Wallet wallet;
	@Inject private HostServices hostServices;
	@Inject @FXMLName("nodeEntry") private Instance<FXMLInjectable<GridPane>> nodeEntry;
	@Inject private Event<State> stateEvent;
	@Inject private Event<UnlockRequest> unlockRequestEvent;
	@Inject private Event<OverlayRequest> overlayRequest;
	@Inject private Event<GridnodeDeployment> gridnodeEvent;

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
	@FXML private CustomGridPane pnlNodeStatus;
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

	private void initializeGridnodeList() {
		final List<Pair<Gridnode, GridnodeDeployment.State>> nodes = gridnodeService.getGridnodeDatabase().getIndividualGridnodesWithState();
		for (Pair<Gridnode, GridnodeDeployment.State> p : nodes) {
			gridPaneAddNode(p.getKey());
		}
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/*for (int i = 0; i < 5; i++) {
			FXMLInjectable<GridPane> g = nodeEntry.get();
			CDIUtil.instantiate(g.get());
			System.out.println("g: " + g);
			System.out.println("g.get: " + g.get());
		}*/
		CDIUtil.instantiate(nodeEntry.get());
		//System.out.println("userdata:" + nodeEntry.get().get().getUserData());
		initializeGridnodeList();
		gridnodeService.start();
		getNodeList();
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
			} catch (Exception e) {
				debug.log(String.format("ERROR: (gridnode init) %s", e.getMessage()));
			}
		});
	}

	private void setupNodeList() {
		try {
			setItems(nodes.getGridnodes());
			/*colNodeStatus.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("status"));
			colNodeAlias.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("alias"));
			colNodeAddress.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("address"));*/

			colNodeTxhash.setCellValueFactory(cell -> {
				Gridnode gridnode = ((TableColumn.CellDataFeatures<Gridnode, Hyperlink>) cell).getValue();
				String text = gridnode.getTxHash() + " " + gridnode.getOutputIndex();
				Hyperlink link = new Hyperlink();
				link.setText(text);

				link.setOnAction(e -> {
					if (e.getTarget().equals(link)) {
						browser.navigateTransaction(gridnode.getTxHash());
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
							.text(gridnode.getTxHash())
							.position(Pos.TOP_RIGHT)
							.showInformation();
					} else {
						Notifications
							.create()
							.title("Key copied to clipboard")
							.text(gridnode.getTxHash())
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
		/*
		GridnodeList result = rpc.call(new GridnodeList.Request(new Object[]{"list-conf"}), GridnodeList.class);
		GridnodeList resultNew = rpc.call(new GridnodeList.Request(new Object[]{"list-conf"}), GridnodeList.class);
		List<Gridnode> grr = new ArrayList<>();
		
		for (Gridnode g : result.getResult()) {
			boolean exist = true;
			for (Gridnode n : resultNew.getResult()) {
				if (g.getAlias().equals(n.getAlias())) {
					exist &= false;
				}
			}if (exist) {
				grr.add(g);
			}
		}
		System.out.println("");System.out.println("");
		System.out.println("ggr: " + grr);
		System.out.println("");System.out.println("");
		resultNew.getResult().removeAll(result.getResult());
		System.out.println("res: " + resultNew.getResult());
		GridnodeList resulttt = rpc.call(new GridnodeList.Request(new Object[]{"list-conf"}), GridnodeList.class);
		GridnodeList resultttNew = rpc.call(new GridnodeList.Request(new Object[]{"list-conf"}), GridnodeList.class);
		Gridnode g = new Gridnode();
		g.setAlias("test");
		resultttNew.getResult().add(g);
		resultttNew.getResult().removeAll(resulttt.getResult());
		System.out.println("res: " + resultttNew.getResult());
		/*
		for (Node n : nodeEntry.get().get().getChildren()) {
			System.out.println("n.class: " + n.getClass());
			System.out.println("n.id: " + n.getId());
		}

		StackPane statusStackPane = (StackPane) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryStackPaneStatus")).findFirst().get();
		FlowPane flowPane = (FlowPane) statusStackPane.getChildren().stream().filter((n)-> n.getId().equals("entryFlowPane")).findFirst().get();
		TextArea ta = (TextArea) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryOutputTa")).findFirst().get();
		Button btn = (Button) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryOutputBtn")).findFirst().get();


		final FontIcon fontIconRight = new FontIcon("fas-arrow-right");
		fontIconRight.setIconColor(Paint.valueOf("#FFFFFF"));
		btn.setGraphic(fontIconRight);
		ProgressBar progress = (ProgressBar) flowPane.getChildren().stream().filter((n)-> n.getId().equals("entryProgressBar")).findFirst().get();
		Label status = (Label) statusStackPane.getChildren().stream().filter((n)-> n.getId().equals("entryStatus")).findFirst().get();
		Label alias = (Label) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryAlias")).findFirst().get();
		Label address = (Label) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryAddress")).findFirst().get();
		Label privateKey = (Label) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryPrivateKey")).findFirst().get();

		// TEST
		
		System.out.println("::::::::::::TEST:::::::::::::");
		System.out.println(":::::::::::::::::::::::::");
		flowPane.setVisible(true);
		progress.setVisible(true);
		progress.setProgress(0.22);
		status.setVisible(false);
		alias.setText("myUsername");

		address.setText("123.0213.023.22:333");
	
		privateKey.setText("TestOfPrivateKeysss");
		pnlNodeStatus.addChirldren(statusStackPane, alias, address, btn, privateKey, ta);
		//System.out.println("p88 children As Row: "+pnlNodeStatus.getChildrenAsRow("TestOfPrivateKey"));
		System.out.println("p99 children: " + pnlNodeStatus.getChildren("TestOfPrivateKeysss","entryOutputTa"));

		TextArea textArea = (TextArea) pnlNodeStatus.getChildren("TestOfPrivateKeysss", "entryOutputTa");
		if (textArea != null) {
			System.out.println("TextArea:" + textArea);
			textArea.setVisible(true);
			textArea.setText("text area output here!");

		}*/
		//pnlNodeStatus.removeChildren("TestOfPrivateKey");
		/* Test 
		FlowPane flowPane = (FlowPane) statusStackPane.getChildren().stream().filter((n)-> n.getId().equals("entryFlowPane")).findFirst().get();
		ProgressBar progress2 = (ProgressBar) flowPane.getChildren().stream().filter((n)-> n.getId().equals("entryProgressBar")).findFirst().get();
		progress2.setProgress(0.22);

		Label alias2 = (Label) nodeEntry.get().get().getChildren().get(GridnodeDeployment.Authentication.ALIAS_NODE);
		alias2.setText("22myUsername");

		Label address2 = (Label) nodeEntry.get().get().getChildren().get(GridnodeDeployment.Authentication.ADDRESS_NODE
		);
		address2.setText("222.0213.023.22:333");

		TextArea ta2 = (TextArea) nodeEntry.get().get().getChildren().get(
			GridnodeDeployment.Authentication.OUTPUT_TA_NODE);

		Button btn2 = (Button) nodeEntry.get().get().getChildren().get(
			GridnodeDeployment.Authentication.OUTPUT_BTN_NODE);

		Label privateKey2 = (Label) nodeEntry.get().get().getChildren().get(
			GridnodeDeployment.Authentication.PRIVATEKEY_NODE);
		privateKey2.setText("22TestOfPrivateKey");
		pnlNodeStatus.addChirldren(progress2, alias2, address2, btn2, privateKey2, ta2);
		
		System.out.println("Verify is true / same");
		System.out.println("TEST: " + pnlNodeStatus.getChildrenAsRows().get(GridnodeDeployment.Authentication.STATUS_NODE));		
//System.out.println(((ProgressBar)pnlNodeStatus.getChildrenAsRows().get(GridnodeDeployment.Authentication.STATUS_NODE)).getProgress() == progress.getProgress());

		System.out.println("size:" + pnlNodeStatus.getChildrenAsRows().size());
		System.out.println("list: " + pnlNodeStatus.getChildrenAsRows());
		Gridnode gridnode = new Gridnode();
		InetSocketAddress address3 = new InetSocketAddress("101.21.321.31",222);
		
		final GridnodeEntity gridnodeAddConf = rpc.call(GridnodeEntity.addConf("mYaliased_2",
			address3, "privatEKey","segewgewgewg32g32gg32", 2), GridnodeEntity.class);
		
		*/

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
		tfNodeAdd.setText("1");
		tfNodeAddAddress.setText("brizo.unigrid.org:22004");
		tfNodeAddUsername.setText("tim");
		tfNodeAddPassword.setText("Chexa5Eiseixeo");

		final int node = parseInt(tfNodeAdd.getText());

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

		String[] addressSplit = tfNodeAddAddress.getText().split(":");

		InetSocketAddress address = new InetSocketAddress(
			addressSplit[0],
			Integer.valueOf(addressSplit[1])
		);

		final Authentication auth = Authentication.builder().auth(
			tfNodeAddUsername.getText(),
			tfNodeAddPassword.getText(),
			address
		).build();

		gridnodeService.deploy(auth, node);

		tfNodeAddAddress.setText("");
		tfNodeAddUsername.setText("");
		tfNodeAddPassword.setText("");
		tfNodeAdd.setText("0");
		pnlNodeAdd.setVisible(false);
	}

	private void eventNodeUpdate(@Observes NodeUpdate nodeUpdate) {
		if (nodeUpdate.getGridnode() != null) {
			switch (nodeUpdate.getGridnode().getValue()) {
				case ONE_PREDEPLOYING:
					gridPaneAddNode(nodeUpdate.getGridnode().getKey());
					break;
				case TWO_PENDING:
					//gridPaneUpdateNodes(nodeUpdate.getGridnode().getKey(), nodeUpdate.getGridnode().getValue());
					break;
				case THREE_DEPLOYMENT:
					gridPaneUpdateNodes(
						nodeUpdate.getGridnode().getKey(),
						nodeUpdate.getStepName(),
						nodeUpdate.getStep(),
						nodeUpdate.getProgress()
					);
					break;
				case FOUR_DEPLOYED:
					gridPaneRemoveNode(nodeUpdate.getGridnode().getKey());
					break;
				default:
					break;
			}
		} else if (nodeUpdate.getConfList() != null) {
			gridPaneUpdateConfList(nodeUpdate.getConfList());
		}
	}

	private void gridPaneAddNode(Gridnode gridnode) {
		System.out.println("---gridPaneAddNode---");
		BigDecimal maxNodes = wallet.getBalance();
		System.out.println("----balances: " + maxNodes);
		Platform.runLater(() -> {
			StackPane statusStackPane = (StackPane) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryStackPaneStatus")).findFirst().get();
			FlowPane flowPane = (FlowPane) statusStackPane.getChildren().stream().filter((n)-> n.getId().equals("entryFlowPane")).findFirst().get();
			TextArea ta = (TextArea) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryOutputTa")).findFirst().get();
			Button btn = (Button) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryOutputBtn")).findFirst().get();

			//ProgressBar progress = (ProgressBar) flowPane.getChildren().stream().filter((n)-> n.getId().equals("entryProgressBar")).findFirst().get();
			//FontIcon fontIcon = (FontIcon) flowPane.getChildren().stream().filter((n)-> n.getId().equals("entryStatusFontIcon")).findFirst().get();

			Label status = (Label) statusStackPane.getChildren().stream().filter((n)-> n.getId().equals("entryStatus")).findFirst().get();
			Label alias = (Label) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryAlias")).findFirst().get();
			Label address = (Label) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryAddress")).findFirst().get();
			Label privateKey = (Label) nodeEntry.get().get().getChildren().stream().filter((n)-> n.getId().equals("entryPrivateKey")).findFirst().get();
			alias.setText("");

			privateKey.setText(gridnode.getPrivateKey());

			if (gridnode.isGridnodeDeployed()) {
				flowPane.setVisible(false);
				status.setVisible(true);
				status.setText(gridnode.getStatus().name());
				alias.setText(gridnode.getAlias());
				address.setText(gridnode.getAddress());
				privateKey.setText(gridnode.getPrivateKey());
				pnlNodeStatus.addChirldren(statusStackPane, alias, address, privateKey);
			} else {
				final GridnodeDeployment deployment = gridnodeService.getGridnodeDatabase().getParent(
					gridnode
				).get();
				flowPane.setVisible(true);
				status.setVisible(false);
				address.setText(String.format("%s:%s", 
					deployment.getAuthentication().get().getAddress().getHostString(),
					deployment.getAuthentication().get().getAddress().getPort()
				));
				final FontIcon fontIconRight = new FontIcon("fas-arrow-right");
				fontIconRight.setIconColor(Paint.valueOf("#FFFFFF"));
				btn.setGraphic(fontIconRight);
				final FontIcon fontIconDown = new FontIcon("fas-arrow-down");
				fontIconDown.setIconColor(Paint.valueOf("#FFFFFF"));
				btn.setOnAction(e -> {
					ta.setMinHeight(ta.isVisible() == true ? 0 : 100);
					ta.setMaxHeight(ta.isVisible() == true ? 0 : Double.MAX_VALUE);
					ta.setVisible(ta.isVisible() == true ? false : true);
					btn.setGraphic(ta.isVisible() == true ? fontIconDown : fontIconRight);
				});
				pnlNodeStatus.addChirldren(statusStackPane, alias, address, btn, privateKey, ta);
			}
		});
	}

	private void gridPaneUpdateNodes(Gridnode gridnode, String description, int step, double progressValue) {
		Platform.runLater(() -> {
			TextArea ta = (TextArea) pnlNodeStatus.getChildren(gridnode.getPrivateKey(), "entryOutputTa");
			if (ta != null) {
				ta.setText(gridnode.getResponseAsString());
				ta.setScrollTop(Double.MAX_VALUE);

				Label alias = (Label) pnlNodeStatus.getChildren(gridnode.getPrivateKey(), "entryAlias");
				if (alias.getText().isEmpty() && gridnode.getAlias() != null) {
					alias.setText(gridnode.getAlias());
				}
				if (!description.isEmpty()) {
					StackPane statusStackPane = (StackPane) pnlNodeStatus.getChildren(gridnode.getPrivateKey(),"entryStackPaneStatus");
					FlowPane flowPane = (FlowPane) statusStackPane.getChildren().stream().filter((n) -> n.getId().equals("entryFlowPane")).findFirst().get();
					Button btn = (Button) flowPane.getChildren().stream().filter((n) -> n.getId().equals("entryStatusBtn")).findFirst().get();
					FontIcon fontIcon = (FontIcon) btn.getGraphic();
					Tooltip tltp = btn.getTooltip();
					ProgressBar progress = (ProgressBar) flowPane.getChildren().stream().filter((n) -> n.getId().equals("entryProgressBar")).findFirst().get();
					progress.setProgress(progressValue);
					if (description.equalsIgnoreCase("Copy volume")
						|| description.equalsIgnoreCase("Sync Unigrid")) {

						fontIcon.setIconLiteral("fas-clone");
						fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
						tltp.setText("Step " + step + "/2: " + description);
						btn.setGraphic(fontIcon);
						btn.setTooltip(tltp);
					} else if (description.equalsIgnoreCase("Loading Backend")) {
						fontIcon.setIconLiteral("fas-spinner");
						fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
						btn.setGraphic(fontIcon);
						tltp.setText("Step " + step + "/2: " + description);
						btn.setTooltip(tltp);
					} else if (description.equalsIgnoreCase("Node Is Deployed!")
						|| (description.equalsIgnoreCase("Loading Backend")
						&& progress.getProgress() == 1.0)) {
						fontIcon.setIconLiteral("fas-check");
						fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
						btn.setGraphic(fontIcon);
						tltp.setText("Step " + step + "/2: " + description);
						btn.setTooltip(tltp);
					}
				}
			}
		});
	}

	private void gridPaneRemoveNode(Gridnode gridnode) {
		Platform.runLater(() -> {
			pnlNodeStatus.removeChildren(gridnode.getPrivateKey());
			//nodes.getGridnodes().add(gridnode);
		});
	}

	private void gridPaneUpdateConfList(List<Gridnode> gridnodes) {
		System.out.println("  Gridnodes: ");
		System.out.println("  nodes.getGridnodes(): " + nodes.getGridnodes());
		System.out.println("  Gridnodes: " + nodes.getGridnodes());
		System.out.println("  Gridnodes: ");

		/*if (nodes.getGridnodes().indexOf(gridnode) == -1) {
			nodes.getGridnodes().add(gridnode);
		}*/
		ObservableList<Gridnode> newConfList = FXCollections.observableArrayList();
		ObservableList<Gridnode> removedConfList = FXCollections.observableArrayList();
		for (Gridnode g : gridnodes) {
			if (g.isGridnodeDeployed()) {
				boolean exist = true;
				for (ObservableList<Node> nodeList : pnlNodeStatus.getChildrenAsRows()) {
					Label privateKeyLabel = (Label) nodeList.stream().filter((n)-> n.getId().equals("entryPrivateKey")).findFirst().get();

					if (privateKeyLabel.getText().equals(g.getPrivateKey())) {
						exist &= false;
					}
				}
				if (exist) {
					removedConfList.add(g);
				}
			}
		}
		for (Gridnode n : removedConfList) {
			gridPaneRemoveNode(n);
		}
		for (Gridnode n : gridnodes) {
			if (n.isGridnodeDeployed()) {
				boolean exist = true;
				for (Gridnode g : nodes.getGridnodes()) {
					if (n.getPrivateKey().equals(g.getPrivateKey()) && n.
						getOutputIndex() == g.getOutputIndex()) {
						exist &= false;
					}
				}
				if (exist) {
					newConfList.add(n);
				}
			}
		}
		for (Gridnode n : newConfList) {
			gridPaneAddNode(n);
		}
		
		System.out.println("  Gridnodes: ");
		System.out.println("  nodes.getGridnodes(): " + nodes.getGridnodes());
		System.out.println("  Gridnodes: " + gridnodes);
		System.out.println("  Gridnodes: ");
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
			//pnlNodeStatus.setItems(nodes.getGridnodes());
		}

		// after wallet is done loading, load the gridnodes.
		if (event.getPropertyName().equals(wallet.STATUS_PROPERTY)) {
			debug.log("loading gridnode list");
			getNodeList();
		}
	}
	public void setItems(ObservableList<Gridnode> gridnodes) {
		for (Gridnode gridnode : gridnodes) {
			gridPaneAddNode(gridnode);
		}
	}

	private void eventNodeRequest(@Observes NodeRequest nodeRequest) {
		rpc.callToJson(new GridnodeEntity.Request(new Object[]{"start-missing", "0"}));
		getNodeList();
		debug.log("Attempting to start nodes");
	}
}
