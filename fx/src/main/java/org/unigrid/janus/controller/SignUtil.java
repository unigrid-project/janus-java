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

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.util.JsonFormat;
import cosmos.auth.v1beta1.Auth;
import cosmos.bank.v1beta1.Tx;
import cosmos.base.abci.v1beta1.Abci;
import cosmos.base.abci.v1beta1.Abci.TxResponse;
import cosmos.base.v1beta1.CoinOuterClass;
import cosmos.base.v1beta1.CoinOuterClass.Coin;
import cosmos.crypto.secp256k1.Keys;
import cosmos.distribution.v1beta1.Tx.MsgWithdrawDelegatorReward;
import cosmos.staking.v1beta1.Tx.MsgBeginRedelegate;
import cosmos.staking.v1beta1.Tx.MsgDelegate;
import cosmos.staking.v1beta1.Tx.MsgUndelegate;
import cosmos.tx.signing.v1beta1.Signing;
import cosmos.tx.v1beta1.ServiceGrpc;
import cosmos.tx.v1beta1.ServiceOuterClass;
import cosmos.tx.v1beta1.TxOuterClass;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.bitcoinj.core.Sha256Hash;
import org.unigrid.janus.model.service.GrpcService;
import gridnode.gridnode.v1.Tx.MsgGridnodeDelegate;
import gridnode.gridnode.v1.Tx.MsgGridnodeUndelegate;
import java.util.List;

@ApplicationScoped
public class SignUtil {

	private final GrpcService grpcService;
	private final long sequence;
	private final long accountNumber;
	private final String token;
	private final String chainId;

	public SignUtil(GrpcService grpcService, long sequence, long accountNumber, String token, String chainId) {
		this.grpcService = grpcService;
		this.sequence = sequence;
		this.accountNumber = accountNumber;
		this.token = token;
		this.chainId = chainId;
	}

	private static final JsonFormat.Printer printer = JsonToProtoObjectUtil.getPrinter();

	public Abci.TxResponse sendDelegateTx(CosmosCredentials payerCredentials, Long amount, BigDecimal feeInAtom,
		long gasLimit) throws Exception {

		MsgGridnodeDelegate msg = MsgGridnodeDelegate.newBuilder()
			.setDelegatorAddress(payerCredentials.getAddress())
			.setAmount(amount)
			.build();

		TxOuterClass.Tx tx = getTxRequest(msg, payerCredentials, null, feeInAtom, gasLimit);
		return bradcastTransaction(tx);
	}

	public Abci.TxResponse sendUndelegateTx(CosmosCredentials payerCredentials, Long amount, BigDecimal feeInAtom,
		long gasLimit) throws Exception {

		MsgGridnodeUndelegate msg = MsgGridnodeUndelegate.newBuilder()
			.setDelegatorAddress(payerCredentials.getAddress())
			.setAmount(amount)
			.build();

		TxOuterClass.Tx tx = getTxRequest(msg, payerCredentials, null, feeInAtom, gasLimit);
		return bradcastTransaction(tx);
	}

	public Abci.TxResponse sendTx(CosmosCredentials payerCredentials, SendInfo sendMsg, BigDecimal feeInAtom,
		long gasLimit) throws Exception {

		CoinOuterClass.Coin sendCoin = CoinOuterClass.Coin.newBuilder()
			.setAmount(String.valueOf(sendMsg.getAmountInAtom()))
			.setDenom(token)
			.build();

		Tx.MsgSend msg = Tx.MsgSend.newBuilder()
			.setFromAddress(sendMsg.getCredentials().getAddress())
			.setToAddress(sendMsg.getToAddress())
			.addAmount(sendCoin)
			.build();

		TxOuterClass.Tx tx = getTxRequest(msg, payerCredentials, sendMsg, feeInAtom, gasLimit);
		return bradcastTransaction(tx);
	}

	public Abci.TxResponse sendStakingTx(CosmosCredentials payerCredentials, String validatorAddress, Long amount,
		BigDecimal feeInAtom, long gasLimit) throws Exception {

		MsgDelegate msg = MsgDelegate.newBuilder()
			.setDelegatorAddress(payerCredentials.getAddress())
			.setValidatorAddress(validatorAddress)
			.setAmount(Coin.newBuilder().setDenom(token).setAmount(amount.toString()).build())
			.build();

		TxOuterClass.Tx tx = getTxRequest(msg, payerCredentials, null, feeInAtom, gasLimit);
		return bradcastTransaction(tx);
	}

	public Abci.TxResponse sendUnstakingTx(CosmosCredentials payerCredentials, String validatorAddress, Long amount,
		BigDecimal feeInAtom, long gasLimit) throws Exception {

		MsgUndelegate msg = MsgUndelegate.newBuilder()
			.setDelegatorAddress(payerCredentials.getAddress())
			.setValidatorAddress(validatorAddress)
			.setAmount(Coin.newBuilder().setDenom(token).setAmount(amount.toString()).build())
			.build();

		TxOuterClass.Tx tx = getTxRequest(msg, payerCredentials, null, feeInAtom, gasLimit);
		return bradcastTransaction(tx);
	}

	public Abci.TxResponse sendSwitchDelegatorTx(CosmosCredentials payerCredentials, String srcValidator, String dstValidator, Long amount,
		BigDecimal feeInAtom, long gasLimit) throws Exception {

		MsgBeginRedelegate msg = MsgBeginRedelegate.newBuilder()
			.setDelegatorAddress(payerCredentials.getAddress())
			.setValidatorSrcAddress(srcValidator)
			.setValidatorDstAddress(dstValidator)
			.setAmount(Coin.newBuilder().setDenom(token).setAmount(amount.toString()).build())
			.build();

		TxOuterClass.Tx tx = getTxRequest(msg, payerCredentials, null, feeInAtom, gasLimit);
		return bradcastTransaction(tx);
	}

	public void sendClaimStakingRewardsTx(CosmosCredentials payerCredentials, List<String> validatorAddresses,
		BigDecimal feeInAtom, long gasLimit) throws Exception {

		for (String validatorAddress : validatorAddresses) {
			String delegatorAddress = payerCredentials.getAddress();

			MsgWithdrawDelegatorReward msg = MsgWithdrawDelegatorReward.newBuilder()
				.setDelegatorAddress(delegatorAddress)
				.setValidatorAddress(validatorAddress)
				.build();
			TxOuterClass.Tx tx = getTxRequest(msg, payerCredentials, null, feeInAtom, gasLimit);
			bradcastTransaction(tx);
		}
	}

	public TxResponse bradcastTransaction(TxOuterClass.Tx tx) throws Exception {
		ServiceGrpc.ServiceBlockingStub stub = ServiceGrpc.newBlockingStub(grpcService.getChannel());

		ServiceOuterClass.BroadcastTxRequest broadcastTxRequest = ServiceOuterClass.BroadcastTxRequest.newBuilder()
			.setTxBytes(tx.toByteString())
			.setMode(ServiceOuterClass.BroadcastMode.BROADCAST_MODE_SYNC)
			.build();

		ServiceOuterClass.BroadcastTxResponse broadcastTxResponse = stub.broadcastTx(broadcastTxRequest);

		if (!broadcastTxResponse.hasTxResponse()) {
			throw new Exception("broadcastTxResponse no body\n" + printer.print(tx));
		}
		Abci.TxResponse txResponse = broadcastTxResponse.getTxResponse();
		if (txResponse.getTxhash().length() != 64) {
			throw new Exception("Txhash illegal\n" + printer.print(tx));
		}
		return txResponse;
	}

	public TxOuterClass.Tx getTxRequest(GeneratedMessageV3 msg, CosmosCredentials payerCredentials, SendInfo sendMsg, BigDecimal feeInAtom,
		long gasLimit) throws Exception {

		Map<String, Auth.BaseAccount> baseAccountCache = new HashMap<>();
		TxOuterClass.TxBody.Builder txBodyBuilder = TxOuterClass.TxBody.newBuilder();
		TxOuterClass.AuthInfo.Builder authInfoBuilder = TxOuterClass.AuthInfo.newBuilder();

		TxOuterClass.Tx.Builder txBuilder = TxOuterClass.Tx.newBuilder();
		Map<String, Boolean> signerInfoExistMap = new HashMap<>();
		Map<String, Boolean> signaturesExistMap = new HashMap<>();

		txBodyBuilder.addMessages(Any.pack(msg, "/"));

		if (!signerInfoExistMap.containsKey(payerCredentials.getAddress())) {
			authInfoBuilder.addSignerInfos(getSignInfo(payerCredentials, baseAccountCache));
			signerInfoExistMap.put(payerCredentials.getAddress(), true);
		}

		CoinOuterClass.Coin feeCoin = CoinOuterClass.Coin.newBuilder()
			.setAmount(ATOMUnitUtil.atomToMicroAtom(feeInAtom).toPlainString())
			.setDenom(token)
			.build();

		TxOuterClass.Fee fee = TxOuterClass.Fee.newBuilder()
			.setGasLimit(gasLimit)
			.setPayer(payerCredentials.getAddress())
			.addAmount(feeCoin)
			.build();

		authInfoBuilder.setFee(fee);

		TxOuterClass.TxBody txBody = txBodyBuilder.build();

		TxOuterClass.AuthInfo authInfo = authInfoBuilder.build();

		if (!signaturesExistMap.containsKey(payerCredentials.getAddress())) {
			txBuilder.addSignatures(getSignBytes(payerCredentials, txBody, authInfo, baseAccountCache));
			signaturesExistMap.put(payerCredentials.getAddress(), true);
		}

		txBuilder.setBody(txBody);
		txBuilder.setAuthInfo(authInfo);
		TxOuterClass.Tx tx = txBuilder.build();
		return tx;
	}

	public TxOuterClass.SignerInfo getSignInfo(CosmosCredentials credentials, Map<String, Auth.BaseAccount> baseAccountCache) throws Exception {
		byte[] encodedPubKey = credentials.getEcKey().getPubKeyPoint().getEncoded(true);
		Keys.PubKey pubKey = Keys.PubKey.newBuilder()
			.setKey(ByteString.copyFrom(encodedPubKey))
			.build();
		TxOuterClass.ModeInfo.Single single = TxOuterClass.ModeInfo.Single.newBuilder()
			.setMode(Signing.SignMode.SIGN_MODE_DIRECT)
			.build();

		TxOuterClass.SignerInfo signerInfo = TxOuterClass.SignerInfo.newBuilder()
			.setPublicKey(Any.pack(pubKey, "/"))
			.setModeInfo(TxOuterClass.ModeInfo.newBuilder().setSingle(single))
			.setSequence(sequence)
			.build();
		return signerInfo;
	}

	public ByteString getSignBytes(CosmosCredentials credentials, TxOuterClass.TxBody txBody, TxOuterClass.AuthInfo authInfo, Map<String, Auth.BaseAccount> baseAccountCache) throws Exception {
		byte[] sigBytes = signDoc(credentials, credentials.getEcKey().getPrivKeyBytes(), txBody, authInfo);
		return ByteString.copyFrom(sigBytes);
	}

	public byte[] signDoc(CosmosCredentials credentials, byte[] privateKey, TxOuterClass.TxBody txBody,
		TxOuterClass.AuthInfo authInfo) {

		ECKeyPair keyPair = ECKeyPair.create(privateKey);
		TxOuterClass.SignDoc signDoc = TxOuterClass.SignDoc.newBuilder()
			.setBodyBytes(txBody.toByteString())
			.setAuthInfoBytes(authInfo.toByteString())
			.setAccountNumber(accountNumber)
			.setChainId(chainId)
			.build();
		byte[] hash = Sha256Hash.hash(signDoc.toByteArray());
		Sign.SignatureData signature = Sign.signMessage(hash, keyPair, false);
		return mergeBytes(signature.getR(), signature.getS());
	}

	private static byte[] mergeBytes(byte[] array1, byte[] array2) {
		byte[] joinedArray = new byte[array1.length + array2.length];
		System.arraycopy(array1, 0, joinedArray, 0, array1.length);
		System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
		return joinedArray;
	}
}
