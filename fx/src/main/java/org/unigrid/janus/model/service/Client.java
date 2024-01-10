package org.unigrid.janus.model.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import cosmos.bank.v1beta1.MsgGrpc;
import cosmos.bank.v1beta1.QueryGrpc;
import cosmos.bank.v1beta1.QueryOuterClass.QueryBalanceRequest;
import cosmos.bank.v1beta1.QueryOuterClass.QueryBalanceResponse;
import cosmos.bank.v1beta1.Tx.MsgSend;
import cosmos.bank.v1beta1.Tx.MsgSendResponse;
import cosmos.base.v1beta1.CoinOuterClass.Coin;
import io.grpc.StatusRuntimeException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.unigrid.janus.model.rpc.entity.TransactionResponse;
import org.unigrid.janus.model.transaction.GridnodeTransaction;

@ApplicationScoped
public class Client {

	@Inject
	GrpcService grpcService;
	private String apiUrl;

	public String checkBalanceForAddress(String address) {
		QueryBalanceRequest request = QueryBalanceRequest.newBuilder()
			.setAddress(address)
			.setDenom("ugd")
			.build();
		try {
			QueryGrpc.QueryBlockingStub stub = QueryGrpc.newBlockingStub(grpcService.getChannel());
			QueryBalanceResponse response = stub.balance(request);
			System.out.println("Responce balance: " + response.toString());
			return response.toString();
		} catch (StatusRuntimeException e) {
			System.out.println("Exception: " + e);
			return null;
		}

	}

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

	public String sendTokens(String senderAddress, String recipientAddress, String amount, String denom) throws Exception {
		MsgGrpc.MsgBlockingStub stub = MsgGrpc.newBlockingStub(grpcService.getChannel());
		PrivateKey privateKey = null;
		try {
			// Create the MsgSend request
			MsgSend request = MsgSend.newBuilder()
				.setFromAddress(senderAddress)
				.setToAddress(recipientAddress)
				.addAmount(Coin.newBuilder().setDenom(denom).setAmount(amount).build())
				.build();

			// Sign the transaction
			String signedPayload = signTransaction(request.toString(), privateKey);

			// Send the transaction
			MsgSendResponse response = stub.send(request);

			// Handle the response here (for now, just printing it)
			System.out.println(response);

			return "Transaction successful";
		} catch (StatusRuntimeException e) {
			// Handle the exception here (for now, just printing it)
			e.printStackTrace();
			return "Transaction failed: " + e.getMessage();
		}
	}
}
