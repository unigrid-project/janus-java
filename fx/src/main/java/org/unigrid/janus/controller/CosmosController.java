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

import com.google.protobuf.Any;
import cosmos.auth.v1beta1.Auth.BaseAccount;
import cosmos.auth.v1beta1.QueryOuterClass.QueryAccountRequest;
import cosmos.auth.v1beta1.QueryOuterClass.QueryAccountResponse;

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
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.SignatureDecodeException;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
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
import org.unigrid.janus.model.service.CosmosService;
import org.unigrid.janus.model.service.GrpcService;
import org.unigrid.janus.model.service.MnemonicService;
import org.unigrid.janus.model.service.RestService;
import org.unigrid.janus.model.signal.DisplaySwapPrompt;
import org.unigrid.janus.model.signal.MnemonicState;
import org.unigrid.janus.model.signal.ResetTextFieldsSignal;
import org.unigrid.janus.model.signal.TabRequestSignal;
import org.unigrid.janus.utils.AddressUtil;
import org.unigrid.janus.view.backing.CosmosTxList;
import java.math.BigInteger;
import org.bouncycastle.util.encoders.Hex;
import cosmos.base.abci.v1beta1.Abci;
import cosmos.staking.v1beta1.QueryOuterClass.QueryDelegatorDelegationsRequest;
import cosmos.staking.v1beta1.QueryOuterClass.QueryDelegatorDelegationsResponse;
import cosmos.tx.v1beta1.ServiceGrpc;
import cosmos.tx.v1beta1.ServiceOuterClass;
import cosmos.tx.v1beta1.TxOuterClass;
import io.grpc.StatusRuntimeException;
import java.security.MessageDigest;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.ValidatorInfo;
import gridnode.gridnode.v1.QueryOuterClass.QueryDelegatedAmountRequest;
import gridnode.gridnode.v1.QueryOuterClass.QueryDelegatedAmountResponse;
import javafx.beans.property.ReadOnlyObjectWrapper;
import org.unigrid.janus.model.gridnode.GridnodeData;
import org.unigrid.janus.model.gridnode.GridnodeModel;
import org.unigrid.janus.model.ApiConfig;
import org.unigrid.janus.model.rest.entity.RedelegationsRequest.RedelegationResponseEntry;
import org.unigrid.janus.model.rest.entity.UnbondingDelegationsRequest.UnbondingResponse;
import org.unigrid.janus.model.signal.CollateralUpdateEvent;
import org.unigrid.janus.model.signal.DelegationAmountEvent;
import org.unigrid.janus.model.signal.DelegationListEvent;
import org.unigrid.janus.model.signal.RedelegationsEvent;
import org.unigrid.janus.model.signal.RewardsEvent;
import org.unigrid.janus.model.signal.UnbondingDelegationsEvent;
import org.unigrid.janus.model.signal.WithdrawAddressEvent;

import javafx.scene.layout.Pane;
import org.unigrid.janus.model.signal.CosmosWalletRequest;
import org.unigrid.janus.model.signal.OverlayRequest;
import org.unigrid.janus.model.signal.UnlockRequest;
import javafx.beans.property.ReadOnlyObjectWrapper;
import org.unigrid.janus.model.gridnode.GridnodeData;
import org.unigrid.janus.model.gridnode.GridnodeModel;
import org.unigrid.janus.model.ApiConfig;
import org.unigrid.janus.model.rest.entity.RedelegationsRequest.RedelegationResponseEntry;
import org.unigrid.janus.model.rest.entity.UnbondingDelegationsRequest.UnbondingResponse;
import org.unigrid.janus.model.signal.CollateralUpdateEvent;
import org.unigrid.janus.model.signal.DelegationAmountEvent;
import org.unigrid.janus.model.signal.DelegationListEvent;
import org.unigrid.janus.model.signal.RedelegationsEvent;
import org.unigrid.janus.model.signal.RewardsEvent;
import org.unigrid.janus.model.signal.UnbondingDelegationsEvent;
import org.unigrid.janus.model.signal.WithdrawAddressEvent;


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
	private CosmosService cosmosClient;
	@Inject
	private MnemonicModel mnemonicModel;
	@Inject
	private CosmosTxList cosmosTxList;
	@Inject
	private Event<ResetTextFieldsSignal> resetTextFieldsEvent;
	@Inject
	private Event<DisplaySwapPrompt> displaySwapEvent;
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
	@Inject
	private Event<OverlayRequest> overlayRequest;
	@Inject
	private Event<UnlockRequest> unlockRequestEvent;
	@Inject
	private Event<CosmosWalletRequest> cosmosWalletEvent;
	@FXML
	private Pane pnlOverlay;

	private GridnodeModel gridnodeModel;
	@Inject
	private CosmosService cosmosService;

	private Account currentSelectedAccount;
	@FXML
	private Label addressLabel;
	@FXML
	private Label balanceLabel;
	@FXML
	private Label stakingAmountLabel;
	@FXML
	private Label unboundingAmountLabel;
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
	private Button copyBtn;
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
	private Button btnStartAllGridnodes;
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
	private TextField delegateAmountTextField;
	@FXML
	private TextField undelegateAmount;
	@FXML
	private TextField validatorAddressTextField;
	@FXML
	private TextField stakeAmountTextField;
	@FXML
	private ListView<Balance> totalsListView;
	@FXML
	private TableView<String> tableTransactionsSent;
	@FXML
	private TableView<String> tableTransactionsReceived;
	@FXML
	private TableView<GridnodeData> tblGridnodeList;
	@FXML
	private ComboBox validatorListComboBox;
	@FXML
	private TableColumn<String, String> colTrxReceived;
	@FXML
	private TableColumn<String, String> colTrxSent;
	@FXML
	private TableColumn<String, String> colGridnodeId;
	@FXML
	private TableColumn<String, String> colStatus;
	@FXML
	private TableColumn<String, String> colStartGridnode;
	private ObservableList<String> transactionsObReceived = FXCollections.observableArrayList();
	private ObservableList<String> transactionsObSent = FXCollections.observableArrayList();
	private ObservableList<GridnodeData> gridnodeData = FXCollections.observableArrayList();
	@FXML
	private ListView<DelegationsRequest.DelegationResponse> delegationsListView;
	@FXML
	@Named("transactionResponse")
	private ListView<TransactionResponse.TxResponse> transactionListView;
	@Inject
	@Named("transactionResponse")
	private TransactionResponse txModel;

	static final String VALIDATORS_DECIMAL_DEVIDER = "10000000000000000";

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

		pnlOverlay.setVisible(false);

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

			displaySwapEvent.fire(DisplaySwapPrompt.builder().build());

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
			totalsListView.addEventFilter(MouseEvent.ANY, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseEvent) {
					mouseEvent.consume();
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
			delegationsListView.addEventFilter(MouseEvent.ANY, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseEvent) {
					mouseEvent.consume();
				}
			});

			colTrxReceived.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()));
			colTrxSent.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()));
			fetchAccountTransactions(accountsData.getSelectedAccount().getAddress());
			fetchGridnodes();
		});
	}

	@FXML
	public void initCopyButton() {

		FontIcon fontIcon = new FontIcon("fas-clipboard");
		fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
		copyBtn.setGraphic(fontIcon);

		copyBtn.setOnAction(e -> {
			final Clipboard cb = Clipboard.getSystemClipboard();
			final ClipboardContent content1 = new ClipboardContent();

			Account selectedAccount = accountsData.getSelectedAccount();

			content1.putString(selectedAccount.getAddress());
			cb.setContent(content1);

			if (SystemUtils.IS_OS_MAC_OSX) {
				Notifications
					.create()
					.title("Address copied to clipboard")
					.text("")
					.position(Pos.TOP_RIGHT)
					.showInformation();
			} else {
				Notifications
					.create()
					.title("Address copied to clipboard")
					.text("")
					.showInformation();
			}
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
	private void testKeys(ActionEvent event) throws SignatureDecodeException, Exception {
		Account selectedAccount = accountsData.getSelectedAccount();
		String pubKey = selectedAccount.getPublicKey();
		byte[] seed = Sha256Hash.hash(pubKey.getBytes());
		System.out.println("pubKey: " + pubKey);
		System.out.println("seed: " + seed);
		String encryptedPrivateKey = selectedAccount.getEncryptedPrivateKey();
		System.out.println("encryptedPrivateKey: " + encryptedPrivateKey);

		// Prompt the user to enter the password
		String password = getPasswordFromUser();
		if (password == null) {
			System.out.println("Password input cancelled!");
			return;
		}

		// Assuming privateKeyBytes is the decrypted private key bytes
		byte[] privateKeyBytes = cryptoUtils.decrypt(encryptedPrivateKey, password);

		// Create an ECKey instance from private key bytes
		ECKey privateKey = ECKey.fromPrivate(privateKeyBytes);
		String originalPubKey = Hex.toHexString(privateKey.getPubKey());
		List<ECKey> keys = cryptoUtils.generateKeysFromCompressedPublicKey(originalPubKey, 10);
		cryptoUtils.printKeys(keys);
		System.out.println("Original Public Key: " + originalPubKey);

		// Create a test message and hash it
		String message = "Test Message";
		Sha256Hash messageHash = Sha256Hash.wrap(Sha256Hash.hash(message.getBytes()));

		// Sign the message hash
		ECKey.ECDSASignature signature = privateKey.sign(messageHash);

		// Convert the signature to DER format
		byte[] signatureDER = signature.encodeToDER();

		// For verification, assume you have the public key in hex format
		ECKey publicKey = ECKey.fromPublicOnly(Hex.decode(pubKey));

		// Verify the signature
		boolean isSignatureValid = publicKey.verify(messageHash, signature);
		System.out.println("Is the signature valid? " + isSignatureValid);
	}

	@FXML
	private void generateKeys(ActionEvent event) throws SignatureDecodeException, Exception {

		BigDecimal currentDelegationAmount = gridnodeDelegationService
			.getCurrentDelegationAmount();
		BigDecimal collateralAmount = BigDecimal.valueOf(collateral.getAmount());
		BigDecimal numberOfNodes = currentDelegationAmount.divide(collateralAmount, 0,
			RoundingMode.DOWN);
		int numberOfNodesInt = numberOfNodes.intValue();
		System.out.println("Nodes we can run: " + numberOfNodesInt);
		int keysToCreate = numberOfNodesInt;
		Account selectedAccount = accountsData.getSelectedAccount();
		String pubKey = selectedAccount.getPublicKey();
		byte[] seed = Sha256Hash.hash(pubKey.getBytes());
		System.out.println("pubKey: " + pubKey);
		System.out.println("seed: " + seed);

		// Current time in milliseconds since epoch
		long creationTimeSeconds = System.currentTimeMillis() / 1000L;

		// Generate the HD wallet from the seed
		DeterministicSeed deterministicSeed = new DeterministicSeed(seed, "",
			creationTimeSeconds);
		DeterministicKeyChain chain = DeterministicKeyChain.builder()
			.seed(deterministicSeed).build();

		// Derive child keys
		List<ECKey> derivedKeysList = new ArrayList<>();
		DeterministicKey parentKey = chain.getWatchingKey();
		for (int i = 0; i < keysToCreate; i++) {
			DeterministicKey childKey = HDKeyDerivation.deriveChildKey(parentKey,
				new ChildNumber(i));
			derivedKeysList.add(ECKey.fromPrivate(childKey.getPrivKey()));
		}

		ECKey[] derivedKeys = derivedKeysList.toArray(new ECKey[0]);

		// cryptoUtils.printKeys(derivedKeys);
		// Now iterate through the allKeys array, signing and verifying a message with
		// each key
		String messageStr = "Start gridnode message";
		byte[] messageBytes = messageStr.getBytes();
		// get private key to sign with
		String encryptedPrivateKey = selectedAccount.getEncryptedPrivateKey();
		System.out.println("encryptedPrivateKey: " + encryptedPrivateKey);

		// Prompt the user to enter the password
		String password = getPasswordFromUser();
		if (password == null) {
			System.out.println("Password input cancelled!");
			return;
		}

		// Decrypt the private key. The returned value should be the original private
		byte[] privateKeyBytes = cryptoUtils.decrypt(encryptedPrivateKey, password);
		byte[] signedMessage = cryptoUtils.signMessage(messageBytes, privateKeyBytes);

		// Verify the signature and the derived keys
		ECKey publicKey = ECKey.fromPrivate(privateKeyBytes);
		List<ECKey> publicKeysToVerify = derivedKeysList;
		System.out.println("publicKey.getPubKey(): " + publicKey.getPubKey());

		long startTime = System.currentTimeMillis(); // Capture the start time

		boolean areKeysVerified = cryptoUtils.verifySignatureKeys(messageBytes,
			signedMessage, derivedKeys, keysToCreate, pubKey);

		long endTime = System.currentTimeMillis(); // Capture the end time

		long elapsedTime = endTime - startTime; // Calculate the elapsed time

		System.out.println("Are keys verified: " + (areKeysVerified ? "Yes" : "No"));
		System.out.println("Verification time: " + elapsedTime + " milliseconds");

	}

	private void verifyWithBadKeys(byte[] messageBytes, ECKey signingKey)
		throws SignatureDecodeException {
		ECKey.ECDSASignature signature = signingKey.sign(Sha256Hash.of(messageBytes));
		byte[] signatureBytes = signature.encodeToDER();

		ECKey[] badKeys = {
			ECKey.fromPrivate(new BigInteger("deadbeefdeadbeefdeadbeefdeadbeef", 16)),
			ECKey.fromPrivate(
			new BigInteger("badbadbadbadbadbadbadbadbadbadbad", 16)),
			ECKey.fromPrivate(
			new BigInteger("facefacefacefacefacefacefaceface", 16))};

		// for (int i = 0; i < badKeys.length; i++) {
		// ECKey[] singleKeyArray = { badKeys[i] };
		// boolean isVerified = cryptoUtils.verifySignature(messageBytes,
		// signatureBytes,
		// singleKeyArray);
		// System.out.println("Verification for bad key " + i + ": "
		// + (isVerified ? "Succeeded" : "Failed"));
		// }
	}

	private String getPasswordFromUser() {
		// just use this default for testing right now

		return "pickles";
	}

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
			// take care of this= getPasswordFromUser();
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
			loadAccounts();
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
		org.unigrid.janus.utils.CosmosCredentials creds = AddressUtil.getCredentials(mnemonic, "", path,
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
	public void onDelegationListEvent(@Observes DelegationListEvent event) {
		Platform.runLater(() -> {
			delegationsListView.getItems().setAll(event.getDelegationList());
		});
	}

	// signal event for staking rewards
	public void onRewardsEvent(@Observes RewardsEvent event) {
		Platform.runLater(() -> {
			stakingRewardsValue(event.getRewardsResponse().getTotal());
		});
	}

	public void onWithdrawAddressEvent(@Observes WithdrawAddressEvent event) {
		Platform.runLater(() -> {
			System.out.println("getWithdrawAddress: " + event.getWithdrawAddress());
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

	/* STAKING */
	public void onRedelegationsEvent(@Observes RedelegationsEvent event) {
		Platform.runLater(() -> {
			// Update the view with redelegations data
			updateRedelegationsView(event.getRedelegationsResponse().getRedelegationResponses());
		});
	}

	// The method to update the view with redelegations data
	private void updateRedelegationsView(List<RedelegationResponseEntry> redelegationResponses) {
		// Update your UI components with redelegation data
		// For example, if you have a ListView for redelegations:
		// redelegationsListView.getItems().setAll(redelegationResponses);
		System.out.println("redelegationResponses: " + redelegationResponses);
	}

	/* UNBONDING */
	public void onUnbondingDelegationsEvent(@Observes UnbondingDelegationsEvent event) {
		Platform.runLater(() -> {
			// Call a method to update the view with the unbonding delegations data
			updateUnbondingDelegationsView(event.getUnbondingDelegationsResponse().getUnbondingResponses());
		});
	}

	// The method to update the view (the implementation depends on how you want to display the unbonding delegations)
	private void updateUnbondingDelegationsView(List<UnbondingResponse> unbondingResponses) {
		// Update your UI components with unbonding delegations data
		// For example, if you have a ListView for unbonding delegations:
		// unbondingDelegationsListView.getItems().setAll(unbondingResponses);
		System.out.println("unbondingResponses: " + unbondingResponses);
	}

	private void createNewAccount() {
		try {
			showPane(cosmosWizardPane);
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	public String getPrivateKeyHex() {
		try {
			Account selectedAccount = accountsData.getSelectedAccount();
			String encryptedPrivateKey = selectedAccount.getEncryptedPrivateKey();

			String password = getPasswordFromUser();
			if (password == null) {
				System.out.println("Password is null! Error in getPasswordFromUser method.");
				return null;
			}
			System.out.println("encryptedPrivateKey: " + encryptedPrivateKey + " password: " + password);
			// Decrypt the private key
			byte[] privateKeyBytes = cryptoUtils.decrypt(encryptedPrivateKey, password);
			if (privateKeyBytes == null) {
				System.out.println("Decryption returned null! Check decryption method.");
				return null;
			}

			// Convert the private key bytes to a HEX string
			String privateKeyHex = org.bitcoinj.core.Utils.HEX.encode(privateKeyBytes);
			System.out.println("Decrypted Private Key (HEX): " + privateKeyHex);

			return privateKeyHex;
		} catch (Exception e) {
			System.err.println("Error while decrypting private key: " + e.getMessage());
			e.printStackTrace(); // Print the full stack trace for detailed error information
			return null;
		}
	}

	private long getSequence(String address) {
		// Set up the auth query client
		cosmos.auth.v1beta1.QueryGrpc.QueryBlockingStub authQueryClient = cosmos.auth.v1beta1.QueryGrpc
			.newBlockingStub(grpcService.getChannel());

		// Prepare the account query request
		QueryAccountRequest accountRequest = QueryAccountRequest.newBuilder()
			.setAddress(address)
			.build();

		try {
			// Query the account information
			QueryAccountResponse response = authQueryClient.account(accountRequest);

			Any accountAny = response.getAccount();
			BaseAccount baseAccount = accountAny.unpack(BaseAccount.class);
			// Process baseAccount as needed
			System.out.println("SEQUENCE NUMBER: " + baseAccount.getSequence());
			return baseAccount.getSequence();

		} catch (Exception e) {
			// Handle exceptions (e.g., account not found, gRPC errors, unpacking errors)
			e.printStackTrace();
			return -1; // or handle it as per your application's requirement
		}

	}

	public static byte[] longToBytes(long value) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(value);
		return buffer.array();
	}

	@FXML
	public void sendTokens() throws Exception {
		byte[] privateKey = Hex.decode(getPrivateKeyHex());

		System.out.println("privateKeyHex from send: " + getPrivateKeyHex());

		CosmosCredentials credentials = CosmosCredentials.create(privateKey, "unigrid");

		Account selectedAccount = accountsData.getSelectedAccount();
		long sequence = getSequence(selectedAccount.getAddress());
		long accountNumber = cosmosService.getAccountNumber(selectedAccount.getAddress());

		SignUtil transactionService = new SignUtil(grpcService, sequence, accountNumber, ApiConfig.getDENOM(), ApiConfig.getCHAIN_ID());

		SendInfo sendMsg = SendInfo.builder()
			.credentials(credentials)
			.toAddress(toAddress.getText())
			.amountInAtom(new BigDecimal(sendAmount.getText()))
			.build();

		Abci.TxResponse txResponse = transactionService.sendTx(credentials, sendMsg, new BigDecimal("0.000001"), 200000);
		System.out.println("RESPONSE");
		System.out.println(txResponse);
		toAddress.setText("");
		sendAmount.setText("");

		// send desktop notofication
		if (SystemUtils.IS_OS_MAC_OSX) {
			Notifications
				.create()
				.title("Transaction hash")
				.text(txResponse.getTxhash())
				.position(Pos.TOP_RIGHT)
				.showInformation();
		} else {
			Notifications
				.create()
				.title("Transaction hash")
				.text(txResponse.getTxhash())
				.showInformation();
		}

	}

	public void delegation(boolean delegate, String validatorAddress) throws Exception {
		byte[] privateKey = Hex.decode(getPrivateKeyHex());
		System.out.println("privateKeyHex from delegate tokens: " + getPrivateKeyHex());

		CosmosCredentials credentials = CosmosCredentials.create(privateKey, "unigrid");

		Account selectedAccount = accountsData.getSelectedAccount();
		long sequence = getSequence(selectedAccount.getAddress());
		long accountNumber = cosmosService.getAccountNumber(selectedAccount.getAddress());

		SignUtil transactionService = new SignUtil(grpcService, sequence, accountNumber, ApiConfig.getDENOM(), ApiConfig.getCHAIN_ID());

		long amount = 0;
		if (validatorAddress == null) {
			if (delegate) {
				amount = Long.parseLong(delegateAmountTextField.getText());

			} else {
				amount = Long.parseLong(undelegateAmount.getText());
			}
		} else {
			amount = Long.parseLong(stakeAmountTextField.getText());
		}

		Abci.TxResponse txResponse = null;

		if (delegate) {
			txResponse = transactionService.sendDelegateTx(credentials, amount, new BigDecimal("0.000001"), 200000);
		} else if (!delegate && validatorAddress == null) {
			txResponse = transactionService.sendUndelegateTx(credentials, amount, new BigDecimal("0.000001"), 200000);
		} else if (!delegate && validatorAddress != null) {
			txResponse = transactionService.sendStakingTx(credentials, validatorAddress, amount, new BigDecimal("0.000001"), 200000);
		}

		System.out.println("Response Tx Delegate");
		delegateAmountTextField.setText("");
		undelegateAmount.setText("");
		stakeAmountTextField.setText("");
		System.out.println(txResponse);

		// send desktop notofication
		if (SystemUtils.IS_OS_MAC_OSX) {
			Notifications
				.create()
				.title("Transaction hash")
				.text(txResponse.getTxhash())
				.position(Pos.TOP_RIGHT)
				.showInformation();
		} else {
			Notifications
				.create()
				.title("Transaction hash")
				.text(txResponse.getTxhash())
				.showInformation();
		}

	}

	@FXML
	public void delegateToGridnode() throws Exception {
		delegation(true, null);
	}

	@FXML
	public void undelegateFromGridnode() throws Exception {
		delegation(false, null);
	}

	@FXML
	public void delegateForStaking() throws Exception {
		ValidatorInfo selectedValidator = (ValidatorInfo) validatorListComboBox.getSelectionModel().getSelectedItem();
		String operatorAddress = "";
		if (selectedValidator != null) {
			operatorAddress = selectedValidator.getOperatorAddress();
			System.out.println("validator address: " + operatorAddress);
			// Now you can use the operatorAddress for further processing
		}
		if (!"".equals(operatorAddress)) {
			delegation(false, operatorAddress);
		} else {
			System.out.println("ERROR: Validator address is empty");
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

			// Update UI fields
			seedPhraseTextArea.setStyle(
				"-fx-font-size: 25px; -fx-background-color: rgba(0, 0, 0, 0.2);");
			seedPhraseTextArea.setText(accountModel.getMnemonic());
			addressFieldPassword.setText(accountModel.getAddress());

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

	@FXML
	private void onStartAllGridnodes(ActionEvent event) {
		gridnodeModel.StartGridnode();
	}

	/* MAIN VIEW */
	public void loadAccounts() {
		try {
			accountsService.loadAccountsFromJson();
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}

		// Clear the existing items from the ComboBox
		accountsDropdown.getItems().clear();

		// Populate the ComboBox with account names
		for (AccountsData.Account account : accountsData.getAccounts()) {
			if (account.getName() != null) {
				accountsDropdown.getItems().add(account.getName());
			} else {
				System.out.println("Account name is null");
			}

		}

		// Add the special "+Add Account" option
		accountsDropdown.getItems().add("+Add New Account");

		// Set the first account as the default selection
		if (!accountsDropdown.getItems().isEmpty()) {
			accountsDropdown.getSelectionModel().selectFirst();
			String defaultAccountName = (String) accountsDropdown.getValue();
			if (defaultAccountName != null) {
				Optional<Account> defaultAccount = accountsService
					.findAccountByName(defaultAccountName);
				if (defaultAccount.isPresent()) {
					accountsData.setSelectedAccount(defaultAccount.get());
				}
			}
		}

		// Set up an action listener for the ComboBox
		accountsDropdown.setOnAction(event -> {
			String selectedAccountName = (String) accountsDropdown.getValue();
			Optional<Account> selectedAccount = accountsService
				.findAccountByName(selectedAccountName);
			// Check if the "+Add Account" option was selected
			if ("+Add New Account".equals(selectedAccountName)) {
				createNewAccount();
				return; // Exit the method early
			}

			if (selectedAccount.isPresent()) {
				accountsData.setSelectedAccount(selectedAccount.get());
				System.out
					.println("Selected Account:" + accountsData.getSelectedAccount());
				addressLabel.setText(accountsData.getSelectedAccount().getAddress());
				System.out.println("getEncryptedPrivateKey: "
					+ accountsData.getSelectedAccount().getEncryptedPrivateKey());
				// Create a background task for the network call
				Task<Void> fetchDataTask = new Task<Void>() {
					@Override
					protected Void call() throws Exception {
						Platform.runLater(() -> {
							System.out.println("user can run: " + cosmosService.gridnodeNumberForUser() + " gridnode(s)!");

							getValidators();
							// Update UI with the balance received from the response
							balanceLabel.setText(cosmosService.getWalletBalance(selectedAccount.get().getAddress()) + " ugd");
							delegationAmountLabel.setText(cosmosService.getDelegatedBalance(selectedAccount.get().getAddress()) + " ugd");
							unboundingAmountLabel.setText(cosmosService.getUnboundingBalance(selectedAccount.get().getAddress()) + " ugd");
							stakingAmountLabel.setText(cosmosService.getStakedBalance(selectedAccount.get().getAddress()) + " ugd");
						});

						// Load transactions and other account data (assuming these methods are adapted
						// for gRPC)
						cosmosTxList.loadTransactions(10);
						cosmosService.loadAccountData(accountsData.getSelectedAccount().getAddress());

						return null;
					}

				};

				// Handle exceptions
				fetchDataTask.setOnFailed(e -> {
					Throwable exception = fetchDataTask.getException();
					Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE,
						null, exception);
					// Optionally show an error message to the user
				});

				// Start the task in a new thread
				new Thread(fetchDataTask).start();
			}
		});
		initCopyButton();
	}

	public void getValidators() {
		cosmos.staking.v1beta1.QueryGrpc.QueryBlockingStub stub = cosmos.staking.v1beta1.QueryGrpc.newBlockingStub(grpcService.getChannel());
		cosmos.staking.v1beta1.QueryOuterClass.QueryValidatorsRequest request = cosmos.staking.v1beta1.QueryOuterClass.QueryValidatorsRequest.newBuilder()
			.setStatus("BOND_STATUS_BONDED")
			.build();
		try {
			cosmos.staking.v1beta1.QueryOuterClass.QueryValidatorsResponse response = stub.validators(request);
			System.out.println("Number of validators: " + response.getValidatorsCount());
			List<ValidatorInfo> validatorInfoList = new ArrayList<>();
			for (cosmos.staking.v1beta1.Staking.Validator validator : response.getValidatorsList()) {
				String moniker = validator.getDescription().getMoniker();
				String operatorAddress = validator.getOperatorAddress();

				BigInteger rate = new BigInteger(validator.getCommission().getCommissionRates().getRate());
				BigInteger devideDecimals = new BigInteger(VALIDATORS_DECIMAL_DEVIDER);
				BigInteger commission = rate.divide(devideDecimals);

				String commissionPercentage = commission.toString() + "%";

				validatorInfoList.add(new ValidatorInfo(moniker, operatorAddress, commissionPercentage));
			}

			validatorListComboBox.getItems().setAll(validatorInfoList);
		} catch (StatusRuntimeException e) {
			System.err.println("RPC error: " + e.getStatus());
		}

	}

	public void onDelegationAmountEvent(@Observes DelegationAmountEvent event) {
		Platform.runLater(() -> {
			String text = event.getAmount().toPlainString() + " UGD";
			delegationAmountLabel.setText(text);
			System.out.println("Delegation Amount: " + text);
		});
	}

	public void onCollateralUpdateEvent(@Observes CollateralUpdateEvent event) {
		if (event.isSuccess()) {
			int amount = event.getAmount();
			System.out.println("Collateral Required: " + amount);
			// Update the UI with the amount
		} else {
			System.out.println("Error fetching collateral");
			// Update the UI to indicate an error
		}
	}

	public void fetchGridnodes() {

		GridnodeData data = new GridnodeData();

		data.setGridnodeId("testing testing");
		data.setStatus("testing");

		gridnodeData.add(data);

		colStartGridnode.setCellValueFactory(cell -> {
			Button button = new Button();
			button.setText("Start Gridnode");

			return new ReadOnlyObjectWrapper(button);
		});

		tblGridnodeList.setItems(gridnodeData);
	}

	public void fetchAccountTransactions(String address) {

		List<TxOuterClass.Tx> transactionsSent = new ArrayList<>();
		List<TxOuterClass.Tx> transactionsReceived = new ArrayList<>();

		ServiceGrpc.ServiceBlockingStub stub = ServiceGrpc.newBlockingStub(grpcService.getChannel());

		// fetch sender transactions
		String querySender = "transfer.sender='" + address + "'";
		transactionsSent.addAll(fetchTransactions(querySender, stub));

		// fetch recipient transactions
		String queryRecipient = "transfer.recipient='" + address + "'";
		transactionsReceived.addAll(fetchTransactions(queryRecipient, stub));

		for (TxOuterClass.Tx tx : transactionsSent) {
			byte[] txBytes = tx.toByteArray();
			byte[] hashBytes = sha256(txBytes);
			String transactionHash = bytesToHex(hashBytes);
			transactionsObSent.add(transactionHash);
		}

		for (TxOuterClass.Tx tx : transactionsReceived) {
			byte[] txBytes = tx.toByteArray();
			byte[] hashBytes = sha256(txBytes);
			String transactionHash = bytesToHex(hashBytes);
			transactionsObReceived.add(transactionHash);
		}

		tableTransactionsSent.setItems(transactionsObSent);
		tableTransactionsReceived.setItems(transactionsObReceived);
	}

	private List<TxOuterClass.Tx> fetchTransactions(String query, ServiceGrpc.ServiceBlockingStub stub) {
		ServiceOuterClass.GetTxsEventRequest request = ServiceOuterClass.GetTxsEventRequest.newBuilder()
			.setLimit(10)
			.setQuery(query)
			.build();

		return stub.getTxsEvent(request).getTxsList();
	}

	private static byte[] sha256(byte[] input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(input);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte b : bytes) {
			result.append(String.format("%02X", b));
		}
		return result.toString();
	}

	@FXML
	private void sendTokensPasswordRequest() {
		unlockRequestEvent.fire(UnlockRequest.builder().type(UnlockRequest.Type.COSMOS_SEND_TOKENS).build());
		overlayRequest.fire(OverlayRequest.OPEN);
	}

	@FXML
	private void delegateToGridnodePasswordRequest() {
		unlockRequestEvent.fire(UnlockRequest.builder().type(UnlockRequest.Type.COSMOS_DELEGATE_GRIDNODE).build());
		overlayRequest.fire(OverlayRequest.OPEN);
	}

	@FXML
	private void undelegateFromGridnodePasswordRequest() {
		unlockRequestEvent.fire(UnlockRequest.builder().type(UnlockRequest.Type.COSMOS_UNDELEGATE_GRIDNODE).build());
		overlayRequest.fire(OverlayRequest.OPEN);
	}

	@FXML
	private void delegateToStakingPasswordRequest() {
		unlockRequestEvent.fire(UnlockRequest.builder().type(UnlockRequest.Type.COSMOS_DELEGATE_STAKING).build());
		overlayRequest.fire(OverlayRequest.OPEN);
	}

	private void eventCosmosWalletRequest(@Observes CosmosWalletRequest cosmosWalletRequest) throws Exception {
		switch (cosmosWalletRequest) {
			case SEND_TOKENS: {
				System.out.println("Send Tokens ADDR " + toAddress.getText());
				System.out.println("Send Tokens AMOUNT " + sendAmount.getText());
				sendTokens();
				break;
			}
			case DELEGATE_GRIDNODE: {
				delegateToGridnode();
				break;
			}
			case UNDELEGATE_GRIDNODE: {
				undelegateFromGridnode();
				break;
			}
			case DELEGATE_STAKING: {
				delegateForStaking();
				break;
			}
			default:
				throw new AssertionError();
		}
	}

}
