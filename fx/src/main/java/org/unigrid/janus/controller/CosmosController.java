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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.unigrid.janus.model.CryptoUtils;
import org.unigrid.janus.model.service.DebugService;

import com.jeongen.cosmos.CosmosRestApiClient;
import com.jeongen.cosmos.crypro.CosmosCredentials;
import com.jeongen.cosmos.util.AddressUtil;

import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Optional;
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
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Sha256Hash;

import org.unigrid.janus.model.AccountModel;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.AccountsData.Account;
import org.unigrid.janus.model.AddressCosmos;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.MnemonicModel;
import org.unigrid.janus.model.rpc.entity.TransactionResponse;
import org.unigrid.janus.model.rpc.entity.TransactionResponse.TxResponse;
import org.unigrid.janus.model.service.AccountsService;
import org.unigrid.janus.model.service.AddressCosmosService;
import org.unigrid.janus.model.service.CosmosRestClient;
import org.unigrid.janus.model.signal.MnemonicState;
import org.unigrid.janus.model.signal.TabRequestSignal;
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
	private CosmosRestClient restClient;
	@Inject
	private MnemonicModel mnemonicModel;
	@Inject
	private CosmosTxList cosmosTxList;
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
	private PasswordField passwordField1;
	@FXML
	private PasswordField passwordField2;
	@FXML
	private Button encryptAndSaveButton;

	@FXML
	@Named("transactionResponse")
	private ListView<TransactionResponse.TxResponse> transactionListView;
	@Inject
	@Named("transactionResponse")
	private TransactionResponse txModel;
	@FXML
	private ListView<String> testListView;

	@FXML
	private ComboBox accountsDropdown;
	private Map<String, StackPane> paneMap = new HashMap<>();
	// List to store the actual mnemonic words
	private AddressCosmosService addressService = new AddressCosmosService();
	private AddressCosmos addressCosmos = new AddressCosmos();

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		paneMap.put("importPane", importPane);
		paneMap.put("cosmosWizardPane", cosmosWizardPane);
		paneMap.put("cosmosMainPane", cosmosMainPane);
		paneMap.put("cosmosWizardPane", cosmosWizardPane);
		paneMap.put("generatePane", generatePane);
		paneMap.put("passwordPane", passwordPane);
		paneMap.put("passwordPane", confirmMnemonic);
		ObservableList<String> testList = FXCollections.observableArrayList("Test 1", "Test 2", "Test 3");
		testListView.setItems(testList);
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

			if (isAccountsJsonEmpty()) {
				showPane(cosmosWizardPane);
			} else {
				showPane(cosmosMainPane);
			}
			// check whether the word changed in order to reset the value
			transactionListView.setCellFactory(param -> new ListCell<TransactionResponse.TxResponse>() {

				@Override
				protected void updateItem(TransactionResponse.TxResponse txResponse, boolean empty) {
					System.out.println("Cell factory called for item: " + txResponse);
					System.out.println("Number of transactions: "
						+ cosmosTxList.getTxResponsesList().size());

					super.updateItem(txResponse, empty);
					if (empty || txResponse == null) {
						setText(null);
					} else {
						setText(txResponse.getTxhash() + " - " + txResponse.getTimestamp());
						System.out.println("txResponse getHeight(): " + txResponse.getHeight());
					}
				}
			});
		});
	}

	@FXML
	private void testBtn(ActionEvent event) {
		//System.out.println("Transaction Response: " + cosmosTxList.getTxResponse());
		//System.out.println("Transaction loadTransactions: " + cosmosTxList.loadTransactions(10));

		//System.out.println("txModel.getTxResponses: " + txModel.getNewTxResponses());
		System.out.println("txModel.getResult: " + txModel.getResult());
	}

	@FXML
	private void encryptKeys(ActionEvent event) {
		try {
			String password1 = passwordField1.getText();
			String password2 = passwordField2.getText();
			if (!password1.equals(password2)) {
				// Passwords do not match
				System.out.println("Passwords do not match!");
				return;
			}

			// Ensure the password is not empty
			if (password1.isEmpty()) {
				System.out.println("Password cannot be empty!");
				return;
			}

			if (accountNameField.getText().isEmpty()) {
				System.out.println("Account name cannot be empty!");
				return;
			}
			accountModel.setName(accountNameField.getText());
			String mnemonic = accountModel.getMnemonic();

			// Encrypt the mnemonic
			String encryptedMnemonic = cryptoUtils.encrypt(mnemonic, password1);
			// Set the encrypted mnemonic to the accountModel
			accountModel.setEncryptedMnemonic(encryptedMnemonic);
			System.out.println("encryptedMnemonic: " + encryptedMnemonic);

			if (accountModel.getEncryptedMnemonic() == null
				|| accountModel.getEncryptedMnemonic().isEmpty()) {
				System.out.println("Encrypted mnemonic is either null or empty!");
				return;
			}

			// clear out any private keys from memory
			accountModel.setMnemonic("");
			accountModel.setPrivateKey(null);
			mnemonic = "";
			showPane(cosmosMainPane);
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	@FXML
	private void encryptTest(ActionEvent event) {
		try {
			String mnemonic = encryptField.getText();

			cryptoUtils.encrypt(mnemonic, "pickles");
			String addressFromPrivateKey = cryptoUtils.getAddressFromPrivateKey(mnemonic);
			addressField.setText(addressFromPrivateKey);
			// Show the cosmosMainPane after successful encryption and saving
			showPane(cosmosMainPane);
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	@FXML
	private void decryptKeys(ActionEvent event) {
		try {
			File encryptedKeysFile = DataDirectory.getEncryptedKeys();
			String jsonContent = new String(
				Files.readAllBytes(encryptedKeysFile.toPath()),
				StandardCharsets.UTF_8);

			// Parse the JSON content
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(jsonContent);
			JsonNode accountsNode = rootNode.get("accounts");
			if (accountsNode.isArray() && accountsNode.size() > 0) {
				JsonNode firstAccountNode = accountsNode.get(0); // only one account for
				// now
				String encryptedMnemonic = firstAccountNode.get("encryptedMnemonic")
					.asText();

				// Decrypt the mnemonic
				System.out.println("encryptedMnemonic: " + encryptedMnemonic);
				String keys = cryptoUtils.decrypt(encryptedMnemonic, "pickles");

				System.out.println(keys);
				byte[] privKey = derivePrivateKeyFromMnemonic(keys, 0);
				System.out.println("private key from menmonic: "
					+ org.bitcoinj.core.Utils.HEX.encode(privKey));
				System.out.println("address: " + cryptoUtils
					.getAddressFromPrivateKey(org.bitcoinj.core.Utils.HEX.encode(privKey)));

				ECKey[] derivedKeys = cryptoUtils
					.deriveKeys(org.bitcoinj.core.Utils.HEX.encode(privKey), 100);
				cryptoUtils.printKeys(derivedKeys);

				// Now iterate through the allKeys array, signing and verifying a message with each key
				String messageStr = "Start gridnode message.";
				byte[] messageBytes = messageStr.getBytes();

				for (int i = 0; i < derivedKeys.length; i++) {
					// Sign the message with the current derived key
					System.out.println("derivedKeys[i]: " + derivedKeys[i]);
					// this uses the private key of the ECKey that was generated
					ECDSASignature signature = derivedKeys[i].sign(Sha256Hash.of(messageBytes));
					byte[] signatureBytes = signature.encodeToDER();

					// Create a single-key array for verification
					ECKey[] singleKeyArray = {derivedKeys[i]};

					// Verify the signature
					boolean isVerified = cryptoUtils.verifySignature(messageBytes,
						signatureBytes, singleKeyArray);
					System.out.println("Verification for key " + i + ": "
						+ (isVerified ? "Succeeded" : "Failed"));
				}
				// Choose a key from derivedKeys for signing
				ECKey signingKey = derivedKeys[0];
				// Create an array of bad keys
				ECKey[] badKeys = {
					ECKey.fromPrivate(new BigInteger("deadbeefdeadbeefdeadbeefdeadbeef", 16)),
					ECKey.fromPrivate(new BigInteger("badbadbadbadbadbadbadbadbadbadbad", 16)),
					ECKey.fromPrivate(new BigInteger("facefacefacefacefacefacefaceface", 16))
				};

				// Sign the message with the wrong key
				ECDSASignature signature = signingKey.sign(Sha256Hash.of(messageBytes));
				byte[] signatureBytes = signature.encodeToDER();

				for (int i = 0; i < badKeys.length; i++) {
					// Create a single-key array for verification
					ECKey[] singleKeyArray = {badKeys[i]};

					// Verify the signature
					boolean isVerified = cryptoUtils.verifySignature(messageBytes,
						signatureBytes, singleKeyArray);
					System.out.println("Verification for bad key " + i + ": "
						+ (isVerified ? "Succeeded" : "Failed"));
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	@FXML
	private void decrypTest(ActionEvent event) {
		try {

			// Decrypt the mnemonic
			System.out.println("encryptedMnemonic: " + keytoDecrypt.getText());
			String keys = cryptoUtils.decrypt(keytoDecrypt.getText(), "pickles");
			decryptField.setText(keys);

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
			try {
				// load the accounts json
				loadAccounts();
			} catch (IOException ex) {
				Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
					ex);
			}
		}
	}

	private boolean isAccountsJsonEmpty() {
		File accountsFile = DataDirectory.getAccountsFile();
		return !accountsFile.exists() || accountsFile.length() == 0;
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
		List<AddressCosmos> addresses
			= objectMapper.readValue(file, new TypeReference<List<AddressCosmos>>() {
			});
		return addresses.size();
	}

	public static byte[] derivePrivateKeyFromMnemonic(String mnemonic, int index)
		throws Exception {
		// Convert mnemonic to seed
		List<String> mnemonicWords = Arrays.asList(mnemonic.split(" "));

		byte[] seed = MnemonicCode.toSeed(mnemonicWords, "");

		// Create master key from seed
		DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed);

		// Derive the key step by step following the path "M/44'/118'/0'/0/0"
		DeterministicKey level1 = HDKeyDerivation.deriveChildKey(masterKey,
			new ChildNumber(44, true));
		DeterministicKey level2 = HDKeyDerivation.deriveChildKey(level1,
			new ChildNumber(118, true));
		DeterministicKey level3 = HDKeyDerivation.deriveChildKey(level2,
			new ChildNumber(0, true));
		DeterministicKey level4 = HDKeyDerivation.deriveChildKey(level3,
			new ChildNumber(0, false));
		DeterministicKey key = HDKeyDerivation.deriveChildKey(level4,
			new ChildNumber(index, false));

		// Optionally print the address (requires additional logic for accurate Cosmos
		// address computation)
		// byte[] publicKey = key.getPubKey();
		// byte[] address = computeAddress(publicKey);
		// System.out.println("Address: " + Base64.encodeBase64String(address)); //
		// Replace with correct encoding
		// Return private key bytes
		byte[] privateKeyBytes = key.getPrivKeyBytes();
		return privateKeyBytes;
	}

	/* IMPORT VIEW */
	@FXML
	private void cancelImport(ActionEvent event) {
		try {
			System.out.println("show main pane");
			showPane(cosmosMainPane);
			importTabPane.getSelectionModel().select(mnemonic12Tab);
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	@FXML
	private void importPrivateKey(ActionEvent event) {
		System.out.println("Import private key");
	}

	@FXML
	public void handleImportAction(ActionEvent event) {
		mnemonicState.setViewState(MnemonicState.ViewState.IMPORT);
		showPane(importPane);
	}

	@FXML
	public void importMnemonicAddress(ActionEvent event) throws Exception {

		int index = 0;

		String mnemonic = String.join(" ", mnemonicModel.getMnemonicWordList());
		System.out.println("mnemonic: " + mnemonic);
		// this.mnemonicArea.setText(mnemonic);

		// Encrypt the mnemonic before setting it to the accountModel
		String password1 = passwordField1.getText();
		String encryptedMnemonic = cryptoUtils.encrypt(mnemonic, password1);
		accountModel.setMnemonic(encryptedMnemonic);
		System.out.println("Set encrypted mnemonic: " + accountModel.getMnemonic());

		// Utilize the derivePrivateKeyFromMnemonic method and CosmosCredentials block
		byte[] privateKey = derivePrivateKeyFromMnemonic(mnemonic, index);
		System.out.println(
			"Private Key: " + org.bitcoinj.core.Utils.HEX.encode(privateKey));

		String path = String.format("m/44'/118'/0'/0/%d", index);
		CosmosCredentials creds = AddressUtil.getCredentials(mnemonic, "", path,
			"unigrid");

		System.out.println("Address from creds: " + creds.getAddress());
		System.out.println("EcKey from creds: " + creds.getEcKey());

		// Update UI fields
		// addressCosmos.setAddress(creds.getAddress());
		// addressCosmos.setPublicKey(
		// org.bitcoinj.core.Utils.HEX.encode(creds.getEcKey().getPubKey()));
		// addressCosmos.setName("pickles_" + index);
		// addressCosmos.setKeyIndex(index);
		// handleSaveAddress(addressCosmos);
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

	/* MAIN VIEW */
	private void loadAccounts() throws IOException {
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
			Optional<Account> selectedAccount = accountsService.findAccountByName(selectedAccountName);
			if (selectedAccount.isPresent()) {
				accountsData.setSelectedAccount(selectedAccount.get());
				System.out.println("Selected Account:" + accountsData.getSelectedAccount());
				addressLabel.setText(accountsData.getSelectedAccount().getAddress());

				// Create a background task for the network call
				Task<Void> fetchDataTask = new Task<Void>() {
					@Override
					protected Void call() throws Exception {
						String accountQuery = restClient
							.checkBalanceForAddress(selectedAccount.get().getAddress());
						Platform.runLater(() -> {
							balanceLabel.setText(accountQuery);
						});
						cosmosTxList.loadTransactions(10);
						System.out.println("cosmosTxList.getTxResponse(): "
							+ cosmosTxList.getTxResponsesList());
						return null;
					}
				};

				// Handle exceptions
				fetchDataTask.setOnFailed(e -> {
					Throwable exception = fetchDataTask.getException();
					Logger.getLogger(CosmosController.class.getName())
						.log(Level.SEVERE, null, exception);
					// Optionally show an error message to the user
				});

				// Start the task in a new thread
				new Thread(fetchDataTask).start();
			}
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
		try {
			CosmosRestApiClient unigridApiService = new CosmosRestApiClient(
				"http://localhost:1317", "cosmosdaemon", "ugd");
			BigDecimal balance = unigridApiService
				.getBalanceInAtom(addressField.getText());
			DecimalFormat formatter = new DecimalFormat("0.00000000");
			String formattedBalance = formatter.format(balance);
			balanceField.setText(formattedBalance);
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}
	}

	private void handleTabRequest(@Observes TabRequestSignal request) {
		if ("select".equals(request.getAction())) {
			mnemonic12Tab.setDisable(true);
			importTabPane.getSelectionModel().select(mnemonic24Tab);
			// Handle other actions as needed
		}
	}

	/* GENERATE SECTION */
	@FXML
	public void handleGenerateAction(ActionEvent event) {
		// generate a new mnemonic
		try {
			mnemonicState.setViewState(MnemonicState.ViewState.GENERATE);
			generateMnemonicAddress();
			showPane(generatePane);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	private void verifyBackPress(ActionEvent event) {
		showPane(generatePane);
	}

	@FXML
	private void handleContinue(ActionEvent event) {
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
	private void handleMnemonicVerification(ActionEvent event) {
		// do check

		if (compareMnemonicWithModel()) {
			// Mnemonics match
			showPane(passwordPane);
		} else {
			// Mnemonics do not match
			System.out.println("mnemonic does not match");
		}
	}

	private boolean compareMnemonicWithModel() {
		// Convert mnemonicWordList to a space-separated string
		String copiedMnemonic = String.join(" ", mnemonicModel.getMnemonicWordList());

		String modelMnemonic = accountModel.getMnemonic();
		System.out.println("modelMnemonic: " + modelMnemonic);
		System.out.println("copiedMnemonic: " + copiedMnemonic);

		// Compare the two mnemonics
		return copiedMnemonic.equals(modelMnemonic);
	}

	public void generateMnemonicAddress() throws Exception {
		// TODO find a better way to handle multiple accounts
		// and addresses
		int index = 0;

		// Step 1: Generate a new 12-word mnemonic
		SecureRandom secureRandom = new SecureRandom();
		List<String> mnemonicWords = MnemonicCode.INSTANCE
			.toMnemonic(secureRandom.generateSeed(32));
		String mnemonic = String.join(" ", mnemonicWords);
		// this.mnemonicArea.setText(mnemonic);

		// // Encrypt the mnemonic before setting it to the accountModel
		// String password1 = passwordField1.getText();
		// String encryptedMnemonic = cryptoUtils.encrypt(mnemonic, password1);
		// System.out.println("Set encrypted mnemonic: " + accountModel.getMnemonic());
		accountModel.setMnemonic(mnemonic);

		// Utilize the derivePrivateKeyFromMnemonic method and CosmosCredentials block
		byte[] privateKey = derivePrivateKeyFromMnemonic(mnemonic, index);
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

		// Update UI fields
		seedPhraseTextArea.setStyle(
			"-fx-font-size: 25px; -fx-background-color: rgba(0, 0, 0, 0.2);");
		seedPhraseTextArea.setText(accountModel.getMnemonic());

		addressFieldPassword.setText(accountModel.getAddress());
		// addressCosmos.setAddress(creds.getAddress());
		// addressCosmos.setPublicKey(org.bitcoinj.core.Utils.HEX.encode(creds.getEcKey().getPubKey()));
		// addressCosmos.setName("pickles_" + index);
		// addressCosmos.setKeyIndex(index);
		// handleSaveAddress(addressCosmos);
	}
}
