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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import cosmos.auth.v1beta1.Auth;
import cosmos.auth.v1beta1.QueryOuterClass;
import cosmos.bank.v1beta1.QueryGrpc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import org.apache.commons.lang3.SystemUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.SignatureDecodeException;
import org.bouncycastle.util.encoders.Hex;
import org.controlsfx.control.Notifications;
import org.unigrid.janus.controller.CosmosCredentials;
import org.unigrid.janus.controller.SignUtil;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.AccountsData.Account;
import org.unigrid.janus.model.ApiConfig;
import org.unigrid.janus.model.CryptoUtils;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.PublicKeysModel;
import org.unigrid.janus.model.StakedBalanceModel;
import org.unigrid.janus.model.UnboundingBalanceModel;
import org.unigrid.janus.model.WalletBalanceModel;
import org.unigrid.janus.model.rest.entity.CollateralRequired;
import org.unigrid.janus.model.rest.entity.DelegationsRequest;
import org.unigrid.janus.model.rest.entity.RedelegationsRequest;
import org.unigrid.janus.model.rest.entity.RewardsRequest;
import org.unigrid.janus.model.rest.entity.UnbondingDelegationsRequest;
import org.unigrid.janus.model.rest.entity.WithdrawAddressRequest;
import org.unigrid.janus.model.rpc.entity.TransactionResponse;
import org.unigrid.janus.model.signal.DelegationStatusEvent;
import org.unigrid.janus.model.signal.DelegationListEvent;
import org.unigrid.janus.model.signal.PublicKeysEvent;
import org.unigrid.janus.model.signal.RedelegationsEvent;
import org.unigrid.janus.model.signal.RewardsEvent;
import org.unigrid.janus.model.signal.UnbondingDelegationsEvent;
import org.unigrid.janus.model.signal.WithdrawAddressEvent;

@ApplicationScoped
public class CosmosService {

	@Inject
	private AccountsData accountData;

	@Inject
	private CryptoUtils cryptoUtils;
	@Inject
	private GridnodeDelegationService gridnodeDelegationService;
	@Inject
	private Event<DelegationListEvent> delegationListEvent;
	@Inject
	private Event<RewardsEvent> rewardsEvent;
	@Inject
	private Event<UnbondingDelegationsEvent> unbondingDelegationsEvent;
	@Inject
	private Event<RedelegationsEvent> redelegationsEvent;
	@Inject
	private GrpcService grpcService;
	@Inject
	private Event<WithdrawAddressEvent> withdrawAddressEvent;
	@Inject
	private Event<DelegationStatusEvent> delegationAmountEvent;
	@Inject
	private Hedgehog hedgehog;
	@Inject
	private AccountsData accountsData;
	@Inject
	private CollateralRequired collateral;
	@Inject
	private PublicKeysModel publicKeysModel;
	@Inject
	private Event<PublicKeysEvent> publicKeysEvent;
	@Inject
	private Event<WalletBalanceModel> walletBalanceEvent;
	@Inject
	private WalletBalanceModel balanceModel;
	@Inject
	private UnboundingBalanceModel unboundingBalanceModel;
	@Inject
	private Event<UnboundingBalanceModel> unboundingBalanceModelEvent;
	@Inject
	private StakedBalanceModel stakedBalanceModel;
	@Inject
	private Event<StakedBalanceModel> stakedBalanceModelEvent;
	@Inject
	private PollingService pollingService;

	static final String TOKEN_DECIMAL_VALUE = "100000000";

	private String apiUrl;

	public TransactionResponse txResponse(String address)
		throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request;

		String url = String.format(
			ApiConfig.getBASE_URL() + "cosmos/tx/v1beta1/txs?events=message.sender='%s'",
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
			Logger.getLogger(CosmosService.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	public void startAccountPoll() {
		pollingService.startPoll();
	}

	public void loadAccountData(String account) throws IOException, InterruptedException {
		System.out.println("Load Account Data");
		RestService restService = new RestService();

		// Delegations
		DelegationsRequest delegationsRequest = new DelegationsRequest(account);
		DelegationsRequest.Response delegationsResponse = new RestCommand<>(
			delegationsRequest, restService).execute();
		// fire event	
		delegationListEvent.fire(new DelegationListEvent(delegationsResponse.getDelegationResponses()));

		// Rewards
		RewardsRequest rewardsRequest = new RewardsRequest(account);
		RewardsRequest.Response rewardsResponse = new RestCommand<>(rewardsRequest,
			restService).execute();
		//fire event
		rewardsEvent.fire(new RewardsEvent(rewardsResponse));

		// Unbonding Delegations
		UnbondingDelegationsRequest unbondingDelegationsRequest = new UnbondingDelegationsRequest(
			account);
		UnbondingDelegationsRequest.Response unbondingDelegationsResponse = new RestCommand<>(
			unbondingDelegationsRequest, restService).execute();
		unbondingDelegationsEvent.fire(new UnbondingDelegationsEvent(unbondingDelegationsResponse));

		// Redelegations
		RedelegationsRequest redelegationsRequest = new RedelegationsRequest(account);
		RedelegationsRequest.Response redelegationsResponse = new RestCommand<>(
			redelegationsRequest, restService).execute();
		redelegationsEvent.fire(new RedelegationsEvent(redelegationsResponse));

		// Withdraw Address
		WithdrawAddressRequest withdrawAddressRequest = new WithdrawAddressRequest(
			account);
		WithdrawAddressRequest.Response withdrawAddressResponse = new RestCommand<>(
			withdrawAddressRequest, restService).execute();
		withdrawAddressEvent.fire(new WithdrawAddressEvent(withdrawAddressResponse.getWithdrawAddress()));

		fetchDelegationAmount(account);

		hedgehog.fetchCollateralRequired();

		String balance = getWalletBalance(account);
		balanceModel.setBalance(balance);
		walletBalanceEvent.fire(balanceModel);

		double unboundingBalance = convertLongToUgd(getUnboundingBalance(account));
		unboundingBalanceModel.setUnboundingAmount(unboundingBalance);
		unboundingBalanceModelEvent.fire(unboundingBalanceModel);

		double stakedBalance = convertLongToUgd(getStakedBalance(account));


		stakedBalanceModel.setStakedBalance(stakedBalance);
		stakedBalanceModelEvent.fire(stakedBalanceModel);
	}

	public long getAccountNumber(String address) {
		cosmos.auth.v1beta1.QueryGrpc.QueryBlockingStub authQueryClient = cosmos.auth.v1beta1.QueryGrpc
			.newBlockingStub(grpcService.getChannel());
		QueryOuterClass.QueryAccountRequest accountRequest = QueryOuterClass.QueryAccountRequest.newBuilder().setAddress(address).build();

		try {
			QueryOuterClass.QueryAccountResponse response = authQueryClient.account(accountRequest);
			Any accountAny = response.getAccount();
			System.out.println("Type URL in getAccountNumber: " + accountAny.getTypeUrl());
			Auth.BaseAccount baseAccount = accountAny.unpack(Auth.BaseAccount.class);
			System.out.println("baseAccount.getPubKey(): " + baseAccount.getPubKey()
				+ " \naddress :" + baseAccount.getAddress());

			// Process baseAccount as needed
			// we need the account number and not the sequence here
			System.out.println("ACCOUNT NUMBER: " + baseAccount.getAccountNumber());
			return baseAccount.getAccountNumber();
		} catch (Exception e) {
			e.printStackTrace();
			return -1; // Handle this as per your application's requirement
		}
	}
	
	private long getSequence(String address) {
		// Set up the auth query client
		cosmos.auth.v1beta1.QueryGrpc.QueryBlockingStub authQueryClient = cosmos.auth.v1beta1.QueryGrpc
			.newBlockingStub(grpcService.getChannel());

		// Prepare the account query request
		QueryOuterClass.QueryAccountRequest accountRequest = QueryOuterClass.QueryAccountRequest.newBuilder()
			.setAddress(address)
			.build();

		try {
			// Query the account information
			QueryOuterClass.QueryAccountResponse response = authQueryClient.account(accountRequest);

			Any accountAny = response.getAccount();
			Auth.BaseAccount baseAccount = accountAny.unpack(Auth.BaseAccount.class);
			// Process baseAccount as needed
			System.out.println("SEQUENCE NUMBER: " + baseAccount.getSequence());
			return baseAccount.getSequence();

		} catch (Exception e) {
			// Handle exceptions (e.g., account not found, gRPC errors, unpacking errors)
			e.printStackTrace();
			return -1; // or handle it as per your application's requirement
		}

	}

	public void fetchDelegationAmount(String account) throws IOException, InterruptedException {
		double delegationAmount = getDelegatedBalance(account);
		int gridnodesTotal = gridnodeNumberForUser(delegationAmount);
		// Fire an event with both the delegation amount and the gridnode count
		DelegationStatusEvent event = DelegationStatusEvent.builder()
			.delegatedAmount(delegationAmount)
			.gridnodeCount(gridnodesTotal)
			.build();
		delegationAmountEvent.fire(event);
	}

	// TODO
	// this should add this data to a model
	public int gridnodeNumberForUser(double delegated) {
		int amountPerGridnode;

		if (hedgehog.fetchCollateralRequired()) {
			amountPerGridnode = collateral.getAmount();
			int gridnodeNumber = (int) Math.floor(delegated / amountPerGridnode);
			System.out.println("user can run: " + gridnodeNumber + " gridnode(s)!");
			return gridnodeNumber;
		}

		throw new IllegalStateException("Collateral amount was not fetched.");
	}

	// TODO
	// this should add this data to a model
	public double getDelegatedBalance(String address) {
		gridnode.gridnode.v1.QueryGrpc.QueryBlockingStub delegateStub = gridnode.gridnode.v1.QueryGrpc.newBlockingStub(grpcService.getChannel());

		gridnode.gridnode.v1.QueryOuterClass.QueryDelegatedAmountRequest delegatedAmountRequest = gridnode.gridnode.v1.QueryOuterClass.QueryDelegatedAmountRequest.newBuilder()
			.setDelegatorAddress(address)
			.build();
		gridnode.gridnode.v1.QueryOuterClass.QueryDelegatedAmountResponse delegatedAmountResponse = delegateStub.delegatedAmount(delegatedAmountRequest);
		double converted = convertLongToUgd(delegatedAmountResponse.getAmount());
		System.out.println("converted UGD: " + converted);
		System.out.println("delegatedAmountResponse UUGD: " + delegatedAmountResponse.getAmount());

		return converted;
	}

	// TODO
	// this should add this data to a model
	public String getWalletBalance(String address) {
		cosmos.bank.v1beta1.QueryOuterClass.QueryBalanceRequest balanceRequest = cosmos.bank.v1beta1.QueryOuterClass.QueryBalanceRequest.newBuilder()
			.setAddress(address)
			.setDenom(ApiConfig.getDENOM()) // Add this line to set the denomination
			.build();

		QueryGrpc.QueryBlockingStub stub = QueryGrpc.newBlockingStub(grpcService.getChannel());
		cosmos.bank.v1beta1.QueryOuterClass.QueryBalanceResponse balanceResponse = stub.balance(balanceRequest);
		BigDecimal rawBalance = new BigDecimal(balanceResponse.getBalance().getAmount());
		BigDecimal scaledBalance = rawBalance.divide(new BigDecimal(TOKEN_DECIMAL_VALUE), 8, RoundingMode.HALF_UP);

		return scaledBalance.toString();

	}

	// TODO
	// this should add this data to a model
	public long getUnboundingBalance(String address) {
		cosmos.staking.v1beta1.QueryGrpc.QueryBlockingStub unboundingStub = cosmos.staking.v1beta1.QueryGrpc.newBlockingStub(grpcService.getChannel());
		cosmos.staking.v1beta1.QueryOuterClass.QueryDelegatorUnbondingDelegationsRequest unboundingRequest = cosmos.staking.v1beta1.QueryOuterClass.QueryDelegatorUnbondingDelegationsRequest.newBuilder()
			.setDelegatorAddr(address)
			.build();
		cosmos.staking.v1beta1.QueryOuterClass.QueryDelegatorUnbondingDelegationsResponse unboundingResponse = unboundingStub.delegatorUnbondingDelegations(unboundingRequest);
		long totalUnbondingAmount = unboundingResponse.getUnbondingResponsesList().stream()
			.flatMapToLong(unbondingDelegation -> unbondingDelegation.getEntriesList().stream()
			.mapToLong(entry -> Long.parseLong(entry.getBalance())))
			.sum();
		
		
		return totalUnbondingAmount;
	}

	// TODO
	// this should add this data to a model
	public long getStakedBalance(String address) {
		cosmos.staking.v1beta1.QueryGrpc.QueryBlockingStub stakingStub = cosmos.staking.v1beta1.QueryGrpc.newBlockingStub(grpcService.getChannel());
		cosmos.staking.v1beta1.QueryOuterClass.QueryDelegatorDelegationsRequest stakingRequest = cosmos.staking.v1beta1.QueryOuterClass.QueryDelegatorDelegationsRequest.newBuilder()
			.setDelegatorAddr(address)
			.build();

		cosmos.staking.v1beta1.QueryOuterClass.QueryDelegatorDelegationsResponse stakingResponse = stakingStub.delegatorDelegations(stakingRequest);

		long totalStaked = stakingResponse.getDelegationResponsesList().stream()
			.mapToLong(delegationResponse -> Long.valueOf(delegationResponse.getBalance().getAmount()))
			.sum();

		return totalStaked;
	}

	public void generateKeys(int gridnodeCount, String password) throws SignatureDecodeException, Exception {
		AccountsData.Account selectedAccount = accountsData.getSelectedAccount();
		String encryptedPrivateKey = selectedAccount.getEncryptedPrivateKey();
		System.out.println("encryptedPrivateKey: " + encryptedPrivateKey);
		System.out.println("Gridnode Count: " + gridnodeCount);
		// Prompt the user to enter the password

		// Assuming privateKeyBytes is the decrypted private key bytes
		byte[] privateKeyBytes = cryptoUtils.decrypt(encryptedPrivateKey, password);

		// Create an ECKey instance from private key bytes
		ECKey privateKey = ECKey.fromPrivate(privateKeyBytes);
		String pubKey = Hex.toHexString(privateKey.getPubKey());
		List<ECKey> keys = cryptoUtils.generateKeysFromCompressedPublicKey(pubKey, gridnodeCount);
		cryptoUtils.printKeys(keys);
		System.out.println("Original Public Key: " + pubKey);

		// Create a test message and hash it
		String message = "GRIDNODE_KEYS";
		Sha256Hash messageHash = Sha256Hash.wrap(Sha256Hash.hash(message.getBytes()));

		// Sign the message hash
		ECKey.ECDSASignature signature = privateKey.sign(messageHash);

		// For verification, assume you have the public key in hex format
		ECKey publicKey = ECKey.fromPublicOnly(Hex.decode(pubKey));

		// Verify the signature
		boolean isSignatureValid = publicKey.verify(messageHash, signature);
		System.out.println("Is the signature valid? " + isSignatureValid);
		List<String> publicKeysHex = keys.stream()
			.map(key -> Hex.toHexString(key.getPubKey()))
			.collect(Collectors.toList());
		savePublicKeysToFile(accountsData.getSelectedAccount().getName(), publicKeysHex);
		publicKeysEvent.fire(new PublicKeysEvent(publicKeysHex));
	}

	public long convertLongToUugd(long amount) {
		long amountInUugd = (long) (amount * 100000000);
		return amountInUugd;
	}

	public long convertBigDecimalInUugd(double amount) {
		double conversionFactor = 100000000.0;
		double result = amount * conversionFactor;
		return (long) result;
	}

	public double convertLongToUgd(long amountInUugd) {
		double amountInUgd = amountInUugd / 100000000.0;
		return amountInUgd;
	}

	public BigDecimal convertBigDecimalToUgd(BigDecimal amountInUugd) {
		BigDecimal conversionFactor = new BigDecimal("100000000");
		return amountInUugd.divide(conversionFactor, 8, RoundingMode.HALF_UP);
	}

	public void onPublicKeysEvent(@Observes PublicKeysEvent event) {
		publicKeysModel.setPublicKeys(event.getPublicKeys());
	}

	private void savePublicKeysToFile(String accountName, List<String> publicKeys) throws IOException {
		File keysFile = DataDirectory.getGridnodeKeysFile(accountName);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(keysFile, false))) {
			for (String key : publicKeys) {
				writer.write(key);
				writer.newLine();
			}
		}
	}
	
	public SignUtil createSignUtilService(){
		Account selectedAccount = accountsData.getSelectedAccount();
		long sequence = getSequence(selectedAccount.getAddress());
		long accountNumber = getAccountNumber(selectedAccount.getAddress());
		SignUtil transactionService = new SignUtil(grpcService, sequence, accountNumber, ApiConfig.getDENOM(), ApiConfig.getCHAIN_ID());
		
		return transactionService;
	}
	
	public CosmosCredentials createCredentials(String password){
		byte[] privateKey = Hex.decode(getPrivateKeyHex(password));
		CosmosCredentials credentials = CosmosCredentials.create(privateKey, "unigrid");
		return credentials;
	}
	
	public String getPrivateKeyHex(String password) {
		try {
			Account selectedAccount = accountsData.getSelectedAccount();
			String encryptedPrivateKey = selectedAccount.getEncryptedPrivateKey();

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
	
	
	public void sendDesktopNotification(String title, String body){
		// send desktop notofication
		if (SystemUtils.IS_OS_MAC_OSX) {
			Notifications
				.create()
				.title(title)
				.text(body)
				.position(Pos.TOP_RIGHT)
				.showInformation();
		} else {
			Notifications
				.create()
				.title(title)
				.text(body)
				.showInformation();
		}
	}
}
