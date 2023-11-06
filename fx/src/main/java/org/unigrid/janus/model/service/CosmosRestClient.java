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

package org.unigrid.janus.model.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeongen.cosmos.CosmosRestApiClient;
import com.jeongen.cosmos.crypro.CosmosCredentials;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.unigrid.janus.model.AccountModel;
import org.unigrid.janus.model.CryptoUtils;
import org.unigrid.janus.model.rpc.entity.TransactionResponse;

import org.unigrid.janus.model.transaction.GridnodeTransaction;
//import org.unigrid.janus.utils.MnemonicToPrivateKey;

@ApplicationScoped
public class CosmosRestClient extends CosmosRestApiClient {

	@Inject
	private AccountModel accountModel;

	@Inject
	private CryptoUtils cryptoUtils;
	private String apiUrl;

	public CosmosRestClient(String baseUrl, String chainId, String token) {
		super(baseUrl, chainId, token);
	}

	// public CosmosRestClient(String apiUrl) {
	// this.apiUrl = apiUrl;
	// }
	public String sendDelegation(GridnodeTransaction transaction) throws Exception {
		// Convert the transaction to JSON
		String jsonPayload = convertToJson(transaction);
		PrivateKey privateKey = null;
		// Sign the transaction (you'll need a method to do this)

		String signedTransaction = signTransaction(jsonPayload, privateKey);

		// Send the transaction
		return post("/gridnode/delegate-tokens", signedTransaction);
	}

	private String post(String endpoint, String payload) throws Exception {
		URL url = new URL(apiUrl + endpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json; utf-8");
		conn.setRequestProperty("Accept", "application/json");
		conn.setDoOutput(true);

		try (OutputStream os = conn.getOutputStream()) {
			byte[] input = payload.getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);
		}

		try (BufferedReader br = new BufferedReader(
			new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder response = new StringBuilder();
			String responseLine;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			return response.toString();
		}
	}

	private String signTransaction(String payload, PrivateKey privateKey)
		throws Exception {
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(privateKey);
		signature.update(payload.getBytes(StandardCharsets.UTF_8));
		byte[] signedData = signature.sign();
		return Base64.getEncoder().encodeToString(signedData);
	}

	private String convertToJson(Object object) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(object);
	}

	public String checkBalanceForAddress(String address) {
		try {
			CosmosRestApiClient unigridApiService = new CosmosRestApiClient(
				"https://rest-testnet.unigrid.org/", "unigrid-testnet-1", "ugd");
			BigDecimal balance = unigridApiService.getBalanceInAtom(address);
			DecimalFormat formatter = new DecimalFormat("0.00000000");
			String formattedBalance = formatter.format(balance);
			return formattedBalance;
		} catch (Exception ex) {
			System.out.println(ex);
			return null;
		}
	}

	public TransactionResponse txResponse(String address)
		throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request;
		String url = String.format(
			"http://194.233.95.48:1317/cosmos/tx/v1beta1/txs?events=message.sender='%s'",
			address);
		System.out.println("url: " + url);

		try {
			request = HttpRequest.newBuilder().uri(new URI(url)).build();
			HttpResponse<String> response = client.send(request,
				HttpResponse.BodyHandlers.ofString());
			System.out.println("txResponse: " + response.body());
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
				false);

			TransactionResponse txResponse = objectMapper.readValue(response.body(),
				TransactionResponse.class);
			return txResponse;
		} catch (URISyntaxException ex) {
			Logger.getLogger(CosmosRestClient.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	public void delegateTokens(String delegatorAddress, String validatorAddress,
		BigDecimal amountInAtom, CosmosCredentials credentials) throws Exception {
		// // Step 1: Create MsgDelegate message
		// Coin delegateAmount = Coin.newBuilder()
		// .setDenom("ugd")
		// .setAmount(ATOMUnitUtil.atomToMicroAtom(amountInAtom).toPlainString())
		// .build();
		// MsgDelegate msgDelegate = MsgDelegate.newBuilder()
		// .setDelegatorAddress(delegatorAddress)
		// .setValidatorAddress(validatorAddress)
		// .setAmount(delegateAmount)
		// .build();
		//
		// // Step 2: Create TxBody object
		// TxBody txBody = TxBody.newBuilder()
		// .addMessages(Any.pack(msgDelegate, "/"))
		// .build();
		//
		// // ... rest of the steps
		// // Step 6: Broadcast Tx object to the network
		// ServiceOuterClass.BroadcastTxRequest broadcastTxRequest =
		// ServiceOuterClass.BroadcastTxRequest.newBuilder()
		// .setTxBytes(tx.toByteString())
		// .setMode(ServiceOuterClass.BroadcastMode.BROADCAST_MODE_SYNC)
		// .build();
		// ServiceOuterClass.BroadcastTxResponse broadcastTxResponse =
		// broadcastTx(broadcastTxRequest);

		// Check for errors and handle response...
	}

//	public String sendTokens(String recipientAddress, String amount, String denom,
//		String password) throws Exception {
//
//		try {
//			// Decrypt the private key using the encryptedPrivateKey from AccountModel and
//			// password
//			byte[] decryptedPrivateKey = cryptoUtils
//				.decrypt(accountModel.getEncryptedPrivateKey(), password);
//
//			// Convert the decrypted private key to CosmosCredentials
//			CosmosCredentials credentials = CosmosCredentials.create(decryptedPrivateKey,
//				"unigrid");
//
//			CosmosRestApiClient unigridCosmosService = new CosmosRestApiClient(
//				"https://rest-testnet.unigrid.org/", "unigrid-testnet-1", "ugd");
//
//			System.out.println("address:" + credentials.getAddress());
//			List<SendInfo> sendList = new ArrayList<>();
//			sendList.add(
//				SendInfo.builder().credentials(credentials).toAddress(recipientAddress)
//					.amountInAtom(new BigDecimal(amount)).build());
//			Abci.TxResponse txResponse = unigridCosmosService.sendMultiTx(credentials,
//				sendList, new BigDecimal("0.000001"), 200000);
//			//System.out.println(txResponse);
//
//			ServiceOuterClass.GetTxsEventResponse txsEventByHeight = unigridCosmosService
//				.getTxsEventByHeight(10099441L, "");
//			//System.out.println(txsEventByHeight);
//
//			return "complete";
//		} catch (Exception e) {
//			throw new Exception("An unexpected error occurred while sending tokens.", e);
//		}
//	}
}
