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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.ApiConfig;
import org.unigrid.janus.model.CryptoUtils;
import org.unigrid.janus.model.rest.entity.DelegationsRequest;
import org.unigrid.janus.model.rest.entity.GridnodeDelegationAmount;
import org.unigrid.janus.model.rest.entity.RedelegationsRequest;
import org.unigrid.janus.model.rest.entity.RewardsRequest;
import org.unigrid.janus.model.rest.entity.UnbondingDelegationsRequest;
import org.unigrid.janus.model.rest.entity.WithdrawAddressRequest;
import org.unigrid.janus.model.rpc.entity.TransactionResponse;
import org.unigrid.janus.model.signal.DelegationAmountEvent;
import org.unigrid.janus.model.signal.DelegationListEvent;
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
	private Event<DelegationAmountEvent> delegationAmountEvent;
	@Inject
	private Hedgehog hedgehog;

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

	public void loadAccountData(String account) throws IOException, InterruptedException {
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

	public void fetchDelegationAmount(String account) throws IOException, InterruptedException {
		gridnodeDelegationService.fetchDelegationAmount(account);
		GridnodeDelegationAmount.Response response = gridnodeDelegationService.getCurrentResponse();

		// Fire the event with the fetched data
		if (response != null) {
			delegationAmountEvent.fire(new DelegationAmountEvent(response.getAmount()));
		}
	}

}
