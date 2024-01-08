/*
	The Janus Wallet
	Copyright © 2021-2023 The Unigrid Foundation, UGD Software AB

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cosmos.bank.v1beta1.QueryGrpc;
import cosmos.bank.v1beta1.QueryOuterClass.QueryBalanceRequest;
import cosmos.bank.v1beta1.QueryOuterClass.QueryBalanceResponse;
import java.math.BigDecimal;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import org.unigrid.janus.model.CryptoUtils;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.GridnodeDelegationService;
import org.unigrid.janus.model.service.Hedgehog;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.text.DecimalFormat;
import java.util.BitSet;
import java.util.Map;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.unigrid.janus.model.AccountModel;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.AccountsData.Account;
import org.unigrid.janus.model.AddressCosmos;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.MnemonicModel;
import org.unigrid.janus.model.rest.entity.CollateralRequired;
import org.unigrid.janus.model.rest.entity.DelegationsRequest;
import org.unigrid.janus.model.rest.entity.RewardsRequest.Balance;
import org.unigrid.janus.model.rpc.entity.TransactionResponse;
import org.unigrid.janus.model.rpc.entity.TransactionResponse.TxResponse;
import org.unigrid.janus.model.service.AccountsService;
import org.unigrid.janus.model.service.AddressCosmosService;
import org.unigrid.janus.model.service.CosmosRestClient;
import org.unigrid.janus.model.service.GrpcService;
import org.unigrid.janus.model.service.MnemonicService;
import org.unigrid.janus.model.signal.MnemonicState;
import org.unigrid.janus.model.signal.ResetTextFieldsSignal;
import org.unigrid.janus.model.signal.TabRequestSignal;
import org.unigrid.janus.utils.AddressUtil;
import org.unigrid.janus.utils.CosmosCredentials;
import org.unigrid.janus.view.backing.CosmosTxList;

@ApplicationScoped
public class CosmosController implements Initializable {

	@Inject
	private DebugService debug;
	@Inject
	private AccountModel accountModel;
	@Inject
	private MnemonicState mnemonicState;
	@Inject
	private AccountsService accountsService;
	@Inject
	private AccountsData accountsData;
	@Inject
	private CosmosRestClient cosmosClient;
	@Inject
	private MnemonicModel mnemonicModel;
	@Inject
	private CosmosTxList cosmosTxList;
	@Inject
	private Event<ResetTextFieldsSignal> resetTextFieldsEvent;
	@Inject
	private Hedgehog hedgehog;
	@Inject
	private CollateralRequired collateral;
	@Inject
	private GridnodeDelegationService gridnodeDelegationService;
	@Inject
	private MnemonicService mnemonicService;
	@Inject
	private GrpcService grpcService;

	private Account currentSelectedAccount;
	@FXML
	private Label addressLabel;
	@FXML
	private Label balanceLabel;
	@FXML
	private TextArea mnemonicArea;
	@FXML
	private TextArea seedPhraseTextArea;
	@FXML
	private TextField addressField;
	@FXML
	private TextField balanceField;
	@FXML
	private TextField encryptField;
	@FXML
	private TextField keytoDecrypt;
	@Inject
	private CryptoUtils cryptoUtils;
	@FXML
	private TextField decryptField;
	@FXML
	private TextField accountNameField;
	@FXML
	private Label importLabel;
	@FXML
	private Label generateLabel;
	@FXML
	private Button importButton;
	@FXML
	private Button generateButton;
	@FXML
	private StackPane importPane;
	@FXML
	private StackPane cosmosWizardPane;
	@FXML
	private StackPane cosmosMainPane;
	@FXML
	private StackPane generatePane;
	@FXML
	private StackPane passwordPane;
	@FXML
	private StackPane confirmMnemonic;
	@FXML
	private TabPane importTabPane;
	@FXML
	private Tab mnemonic12Tab;
	@FXML
	private Tab mnemonic24Tab;
	@FXML
	private TextField addressFieldPassword;
	@FXML
	private TextField toAddress;
	@FXML
	private TextField sendAmount;
	@FXML
	private PasswordField passwordField1;
	@FXML
	private PasswordField passwordField2;
	@FXML
	private Button encryptAndSaveButton;
	@FXML
	private Text sendWarnMsg12;
	@FXML
	private Text sendWarnMsg24;
	@FXML
	private TextField importPassword;
	@FXML
	private Text sendWarnPassword;
	@FXML
	private Label totalRewards;
	@FXML
	private Label delegationAmountLabel;
	@FXML
	private ListView<Balance> totalsListView;
	@FXML
	private ListView<DelegationsRequest.DelegationResponse> delegationsListView;
	@FXML
	@Named("transactionResponse")
	private ListView<TransactionResponse.TxResponse> transactionListView;
	@Inject
	@Named("transactionResponse")
	private TransactionResponse txModel;

	@FXML
	private ComboBox accountsDropdown;
	private Map<String, StackPane> paneMap = new HashMap<>();
	// List to store the actual mnemonic words
	private AddressCosmosService addressService = new AddressCosmosService();
	private AddressCosmos addressCosmos = new AddressCosmos();
	private BigDecimal scaleFactor = new BigDecimal("100000000");

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		paneMap.put("importPane", importPane);
		paneMap.put("cosmosWizardPane", cosmosWizardPane);
		paneMap.put("cosmosMainPane", cosmosMainPane);
		paneMap.put("cosmosWizardPane", cosmosWizardPane);
		paneMap.put("generatePane", generatePane);
		paneMap.put("passwordPane", passwordPane);
		paneMap.put("confirmMnemonic", confirmMnemonic);

		ObservableList<TxResponse> observableList = FXCollections
			.observableArrayList(cosmosTxList.getTxResponsesList());
		transactionListView.setItems(observableList);

		Platform.runLater(() -> {

			System.out.println("Is on FX thread: " + Platform.isFxApplicationThread());

			System.out.println("run later method called");
			// Bind the width of the labels to the width of the buttons
			importLabel.prefWidthProperty().bind(importButton.widthProperty());
			generateLabel.prefWidthProperty().bind(generateButton.widthProperty());
			// Add a listener to the first TextField for the paste event

			if (accountsService.isAccountsJsonEmpty()) {
				showPane(cosmosWizardPane);
			} else {
				showPane(cosmosMainPane);
			}
			// check whether the word changed in order to reset the value
			transactionListView.setCellFactory(
				param -> new ListCell<TransactionResponse.TxResponse>() {
				@Override
				protected void updateItem(
					TransactionResponse.TxResponse txResponse,
					boolean empty) {
					System.out.println(
						"Cell factory called for item: " + txResponse);
					System.out.println("Number of transactions: "
						+ cosmosTxList.getTxResponsesList().size());

					super.updateItem(txResponse, empty);
					if (empty || txResponse == null) {
						setText(null);
					} else {
						setText(txResponse.getTxhash() + " - "
							+ txResponse.getTimestamp());
						System.out.println("txResponse getHeight(): "
							+ txResponse.getHeight());
					}
				}
			});
			totalsListView.setCellFactory(lv -> new ListCell<Balance>() {
				@Override
				protected void updateItem(Balance item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
					} else {
						setText(item.getAmount() + " " + item.getDenom());
					}
				}
			});
			delegationsListView.setCellFactory(
				listView -> new ListCell<DelegationsRequest.DelegationResponse>() {
				@Override
				protected void updateItem(
					DelegationsRequest.DelegationResponse item,
					boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
					} else {

						BigDecimal amount = new BigDecimal(
							item.getBalance().getAmount());
						BigDecimal displayAmount = amount.divide(scaleFactor);
						String text = String.format(
							"Validator: %s, Amount: %s %s",
							item.getDelegation().getValidatorAddress(),
							displayAmount.toPlainString(),
							item.getBalance().getDenom());
						setText(text);
					}
				}
			});
		});
	}

	@FXML
	private void testBtn(ActionEvent event) {
		// System.out.println("Transaction Response: " + cosmosTxList.getTxResponse());
		// System.out.println("Transaction loadTransactions: " +
		// cosmosTxList.loadTransactions(10));

		// System.out.println("txModel.getTxResponses: " + txModel.getNewTxResponses());
		System.out.println("txModel.getResult: " + txModel.getResult());
	}

	@FXML
	private void onEncryptKeys(ActionEvent event) {
		try {
			String password1 = passwordField1.getText();
			String password2 = passwordField2.getText();
			if (!password1.equals(password2)) {

				System.out.println("Passwords do not match!");
				onErrorMessage("Passwords do not match!", sendWarnPassword);
				return;
			}

			if (password1.isEmpty()) {
				System.out.println("Password cannot be empty!");
				onErrorMessage("Password cannot be empty!", sendWarnPassword);
				return;
			}

			if (accountNameField.getText().isEmpty()) {
				System.out.println("Account name cannot be empty!");
				onErrorMessage("Account name cannot be empty!", sendWarnPassword);
				return;
			}
			accountModel.setName(accountNameField.getText());

			byte[] privateKey = accountModel.getPrivateKey();
			if (privateKey == null || privateKey.length == 0) {
				System.out.println("Private key is either null or empty!");
				return;
			}

			// Encrypt the private key
			String encryptedPrivateKey = cryptoUtils.encrypt(privateKey, password1);
			// accountModel.setEncryptedPrivateKey(encryptedPrivateKey);
			System.out.println("Encrypted Private Key: " + encryptedPrivateKey);

			// Clear out any private keys from memory
			Arrays.fill(privateKey, (byte) 0);

			// Clear the model and reset the text fields
			resetTextFieldsEvent.fire(ResetTextFieldsSignal.builder().build());
			showPane(cosmosMainPane);
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	private void resetTextFields(@Observes ResetTextFieldsSignal signal) {
		System.out.println("Resetting text fields");
		passwordField1.setText("");
		passwordField2.setText("");
		accountNameField.setText("");
		importTabPane.getSelectionModel().select(mnemonic12Tab);
		mnemonic12Tab.setDisable(false);
		mnemonic24Tab.setDisable(false);
		importPassword.setText("");
		accountModel.reset();
	}

	// @FXML
	// private void encryptTest(ActionEvent event) {
	// try {
	// String mnemonic = encryptField.getText();
	//
	// cryptoUtils.encrypt(mnemonic, "pickles");
	// String addressFromPrivateKey =
	// cryptoUtils.getAddressFromPrivateKey(mnemonic);
	// addressField.setText(addressFromPrivateKey);
	// // Show the cosmosMainPane after successful encryption and saving
	// showPane(cosmosMainPane);
	// } catch (Exception ex) {
	// Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
	// ex);
	// }
	// }
	@FXML
	private void decryptPrivateKey(ActionEvent event) {
		try {
			Account selectedAccount = accountsData.getSelectedAccount();
			if (selectedAccount == null) {
				System.out.println("No account selected!");
				return;
			}

			String encryptedPrivateKey = selectedAccount.getEncryptedPrivateKey();
			System.out.println("encryptedPrivateKey: " + encryptedPrivateKey);

			// Prompt the user to enter the password
			String password = "";
			//take care of this= getPasswordFromUser();
			if (password == null) {
				System.out.println("Password input cancelled!");
				return;
			}

			// Decrypt the private key. The returned value should be the original private
			// key bytes.
			byte[] privateKeyBytes = cryptoUtils.decrypt(encryptedPrivateKey, password);
			System.out.println(
				"Decrypted Private Key (Bytes): " + Arrays.toString(privateKeyBytes));
			System.out.println("Decrypted Private Key (HEX): "
				+ cryptoUtils.bytesToHex(privateKeyBytes));
			// Convert the private key bytes to a HEX string
			String privateKeyHex = org.bitcoinj.core.Utils.HEX.encode(privateKeyBytes);
			System.out.println("Private Key in HEX: " + privateKeyHex);
			System.out.println("Address from priv key: "
				+ cryptoUtils.getAddressFromPrivateKey(privateKeyHex));
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	// @FXML
	// private void decrypTest(ActionEvent event) {
	// try {
	//
	// // Decrypt the mnemonic
	// System.out.println("encryptedPrivateKey: " + keytoDecrypt.getText());
	// String keys = cryptoUtils.decrypt(keytoDecrypt.getText(), "pickles");
	// decryptField.setText(keys);
	//
	// } catch (Exception ex) {
	// Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
	// ex);
	// }
	// }
	public void handleSaveAddress(AddressCosmos newAddress) {
		try {
			addressService.saveAddress(newAddress);
			// Handle success, update UI, etc.
		} catch (IOException e) {
			debug.print(e.getMessage(), CosmosController.class.getSimpleName());
		}
	}

	private void showPane(StackPane paneToShow) {
		// List of all panes
		String paneName = paneMap.entrySet().stream()
			.filter(entry -> entry.getValue() == paneToShow).map(Map.Entry::getKey)
			.findFirst().orElse(null);
		List<StackPane> allPanes = Arrays.asList(importPane, cosmosWizardPane,
			cosmosMainPane, generatePane, passwordPane, confirmMnemonic);
		mnemonicModel.setCurrentPane(paneName);
		// Loop through all panes and set visibility
		for (StackPane pane : allPanes) {
			if (pane == paneToShow) {
				pane.setVisible(true);
			} else {
				pane.setVisible(false);
			}
		}
		if (paneToShow == cosmosMainPane) {
			// load the accounts json
			accountsService.loadAccounts();
		}
	}

	private void revealContent(TextField tf) {
		tf.setPromptText(tf.getText());
		tf.setText("");
	}

	private void hideContent(TextField tf) {
		tf.setText(tf.getPromptText());
		tf.setPromptText("");
	}

	private int getNextIndex() throws IOException {
		File file = DataDirectory.getCosmosAddresses();
		if (!file.exists() || file.length() == 0) {
			return 0;
		}

		ObjectMapper objectMapper = new ObjectMapper();
		List<AddressCosmos> addresses = objectMapper.readValue(file,
			new TypeReference<List<AddressCosmos>>() {
		});
		return addresses.size();
	}

	@FXML
	private void onCancelAccountGeneration(ActionEvent event) {
		try {
			System.out.println("show main pane");
			showPane(cosmosMainPane);

			resetTextFieldsEvent.fire(ResetTextFieldsSignal.builder().build());

		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	/* IMPORT VIEW */
	@FXML
	private void importPrivateKey(ActionEvent event) {
		System.out.println("Import private key: " + importPassword.getText());
		try {
			accountModel.setAddress(
				cryptoUtils.getAddressFromPrivateKey(importPassword.getText()));
			accountModel.setPublicKey(
				cryptoUtils.getPublicKeyBytes(importPassword.getText()));
			accountModel.setPrivateKey(
				cryptoUtils.getPrivateKeyBytes(importPassword.getText()));
			System.out.println("Address from private key: " + accountModel.getAddress());
			addressFieldPassword.setText(accountModel.getAddress());
		} catch (NoSuchAlgorithmException | NoSuchProviderException
			| InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		showPane(passwordPane);
	}

	@FXML
	public void onImportAction(ActionEvent event) {
		mnemonicState.setViewState(MnemonicState.ViewState.IMPORT);
		showPane(importPane);
	}

	@FXML
	public void onImportMnemonicAddress(ActionEvent event) throws Exception {

		int index = 0;

		String mnemonic = String.join(" ", mnemonicModel.getMnemonicWordList());
		System.out.println("mnemonic: " + mnemonic);
		// this.mnemonicArea.setText(mnemonic);
		byte[] privateKey = mnemonicService.derivePrivateKeyFromMnemonic(mnemonic, index);
		// Encrypt the mnemonic before setting it to the accountModel
		// String password1 = passwordField1.getText();
		// String encryptedPrivateKey = cryptoUtils.encrypt(privateKey, password1);
		// accountModel.setMnemonic(encryptedPrivateKey);
		// System.out.println("Set encrypted mnemonic: " + accountModel.getMnemonic());
		System.out.println(
			"Private Key: " + org.bitcoinj.core.Utils.HEX.encode(privateKey));

		String path = String.format("m/44'/118'/0'/0/%d", index);
		CosmosCredentials creds = AddressUtil.getCredentials(mnemonic, "", path,
			"unigrid");

		System.out.println("Address from creds: " + creds.getAddress());
		System.out.println("EcKey from creds: " + creds.getEcKey());

		// Populate the AccountModel
		accountModel.setMnemonic(mnemonic);
		System.out.println("Set mnemonic: " + accountModel.getMnemonic());

		accountModel.setAddress(creds.getAddress());
		System.out.println("Set address: " + accountModel.getAddress());

		accountModel.setPrivateKey(privateKey);
		accountModel.setPublicKey(creds.getEcKey().getPubKey());

		addressFieldPassword.setText(accountModel.getAddress());

		showPane(passwordPane);
	}

	private void showError(String message, Tab tab) {
		Text errorMsg = (tab == mnemonic12Tab) ? sendWarnMsg12 : sendWarnMsg24;
		onErrorMessage(message, errorMsg);
	}

	private void onErrorMessage(String message, Text sendWarnMsg) {
		sendWarnMsg.setFill(Color.web("#f28407"));
		sendWarnMsg.setText(message);
		sendWarnMsg.setVisible(true);

		PauseTransition pause = new PauseTransition(Duration.seconds(3));

		pause.setOnFinished(e -> {
			sendWarnMsg.setVisible(false);
			sendWarnMsg.setText("");
		});

		pause.play();
	}

	/* DELEGATION LIST VIEW */
	public void setDelegations(List<DelegationsRequest.DelegationResponse> delegations) {
		Platform.runLater(() -> {
			delegationsListView.getItems().setAll(delegations);
		});
	}

	/* REWARDS LIST VIEW */
	public void stakingRewardsValue(List<Balance> totals) {
		ObservableList<Balance> items = FXCollections.observableArrayList(totals);

		Platform.runLater(() -> {
			totalsListView.getItems().clear();
			totalsListView.setCellFactory(listView -> new ListCell<Balance>() {
				@Override
				protected void updateItem(Balance item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
					} else {
						BigDecimal amount = new BigDecimal(item.getAmount());
						BigDecimal displayAmount = amount.divide(scaleFactor);
						setText(displayAmount.toPlainString() + " " + item.getDenom());
					}
				}
			});
			totalsListView.getItems().addAll(items);
		});
	}

	@FXML
	private void createNewAccount(ActionEvent event) {
		try {
			showPane(cosmosWizardPane);
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	@FXML
	private void checkBalance(ActionEvent event) {
		QueryBalanceRequest request = QueryBalanceRequest.newBuilder()
			.setAddress(addressField.getText())
			.setDenom("ugd")
			.build();

		QueryGrpc.QueryBlockingStub stub = QueryGrpc.newBlockingStub(grpcService.getChannel());
		QueryBalanceResponse response = stub.balance(request);
		balanceField.setText(response.getBalance().getAmount());


//uncomment all		try {
//			CosmosRestApiClient unigridApiService = new CosmosRestApiClient(
//				"http://localhost:1317", "cosmosdaemon", "ugd");
//			BigDecimal balance = unigridApiService
//				.getBalanceInAtom(addressField.getText());
//			DecimalFormat formatter = new DecimalFormat("0.00000000");
//			String formattedBalance = formatter.format(balance);
//			balanceField.setText(formattedBalance);
//		} catch (Exception ex) {
//			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
//				ex);
//		}
	}

	@FXML
	private void sendTokens(ActionEvent event) {
		try {
			String toAddress = this.toAddress.getText();
			String sendAmount = this.sendAmount.getText();
			String password = "pickles"; // TODO change to user input

			String response = "crap";
			cosmosClient.sendTokens(toAddress, sendAmount, "ugd", password);

			System.out.println("Response: " + response);
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	private void handleTabRequest(@Observes TabRequestSignal request) {
		Tab selectedTab = importTabPane.getSelectionModel().getSelectedItem();
		boolean shouldProceed = false;

		System.out.println("Selected Tab: " + selectedTab.getText());
		System.out.println("Word List Length: " + request.getWordListLength());

		if ("select".equals(request.getAction())
			|| "select12".equals(request.getAction())) {
			if (selectedTab == mnemonic12Tab) {
				if (request.getWordListLength() == 12) {
					System.out.println("Correct number of words for 12-word mnemonic");
					mnemonic24Tab.setDisable(true);
					shouldProceed = true;
				} else if (request.getWordListLength() == 24) {
					System.out.println("Switching to 24-word mnemonic tab");
					mnemonic12Tab.setDisable(true);
					mnemonic24Tab.setDisable(false);
					importTabPane.getSelectionModel().select(mnemonic24Tab);
					shouldProceed = true;

				} else {
					System.out.println("Invalid number of words for 12-word mnemonic");
					showError("Invalid number of words. Please enter 12 words.",
						selectedTab);
				}
			} else if (selectedTab == mnemonic24Tab) {
				if (request.getWordListLength() == 24) {
					System.out.println("Correct number of words for 24-word mnemonic");
					mnemonic12Tab.setDisable(true);
					shouldProceed = true;
				} else if (request.getWordListLength() == 12) {
					System.out.println("Switching to 12-word mnemonic tab");
					mnemonic24Tab.setDisable(true);
					mnemonic12Tab.setDisable(false);
					importTabPane.getSelectionModel().select(mnemonic12Tab);
					shouldProceed = true;
				} else {
					System.out.println("Invalid number of words for 24-word mnemonic");
					showError("Invalid number of words. Please enter 24 words.",
						selectedTab);
				}
			}
		} else {
			showError(
				"Invalid mnemonic length. Please enter either a 12 or 24 word mnemonic.",
				selectedTab);
		}

		System.out.println("Should Proceed: " + shouldProceed);

		if (request.getCallback() != null) {
			request.getCallback().onResult(shouldProceed);
		}
	}

	/* GENERATE SECTION */
	@FXML
	public void onGenerateAction(ActionEvent event) {
		// generate a new mnemonic
		try {
			mnemonicState.setViewState(MnemonicState.ViewState.GENERATE);
			mnemonicService.generateMnemonicAddress();
			showPane(generatePane);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	private void onVerifyBackPress(ActionEvent event) {
		showPane(generatePane);
	}

	@FXML
	private void onContinue(ActionEvent event) {
		showPane(confirmMnemonic);
	}

	@FXML
	private void verifyBack(ActionEvent event) {
		showPane(generatePane);
	}

	@FXML
	private void delegateTokens(ActionEvent event) {
		// do something
		System.out.println("delegate pressed");
	}

	@FXML
	private void onMnemonicVerification(ActionEvent event) {
		// do check

		if (mnemonicService.compareMnemonicWithModel()) {
			// Mnemonics match
			showPane(passwordPane);
		} else {
			// Mnemonics do not match
			System.out.println("mnemonic does not match");
		}
	}

	private static byte[] getBits(byte[] data, int fromBits, int toBits, boolean pad) {
		final BitSet bits = BitSet.valueOf(data);
		BitSet extractedBits = bits.get(fromBits, toBits);

		int extractedBitLength = toBits - fromBits;
		int remainder = extractedBitLength % 8;

		if (pad && remainder != 0) {
			int paddingLength = 8 - remainder;
			// Increase the size of extractedBits to accommodate padding
			extractedBits.set(extractedBitLength, extractedBitLength + paddingLength, false); // Set padding bits to 0
		} else if (!pad && remainder != 0) {
			// Throw an error if padding is not allowed but is required
			throw new RuntimeException("ERR_BAD_FORMAT illegal zero padding");
		}
		return extractedBits.toByteArray();
	}
}
