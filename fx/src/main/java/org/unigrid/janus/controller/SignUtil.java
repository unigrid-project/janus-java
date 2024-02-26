/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.unigrid.janus.controller;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import cosmos.auth.v1beta1.Auth;
import cosmos.auth.v1beta1.QueryOuterClass.QueryAccountResponse;
import cosmos.bank.v1beta1.Tx;
import cosmos.base.abci.v1beta1.Abci;
import cosmos.base.v1beta1.CoinOuterClass;
import cosmos.crypto.secp256k1.Keys;
import cosmos.tx.signing.v1beta1.Signing;
import cosmos.tx.v1beta1.ServiceOuterClass;
import cosmos.tx.v1beta1.TxOuterClass;
import io.netty.util.internal.StringUtil;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.bitcoinj.core.Sha256Hash;

public class SignUtil {

	private final GaiaHttpClient client;

	private final String token;

	private final String chainId;

	public SignUtil(String baseUrl, String token, String chainId) {
		this.client = new GaiaHttpClient(baseUrl);
		this.token = token;
		this.chainId = chainId;
	}
	
	private static final JsonFormat.Printer printer = JsonToProtoObjectUtil.getPrinter();

	public Abci.TxResponse sendMultiTx(CosmosCredentials payerCredentials, List<SendInfo> sendList, BigDecimal feeInAtom, long gasLimit) throws Exception {
		if (sendList == null || sendList.size() == 0) {
			throw new Exception("sendList is empty");
		}

		TxOuterClass.Tx tx = getTxRequest(payerCredentials, sendList, feeInAtom, gasLimit);

		ServiceOuterClass.BroadcastTxRequest broadcastTxRequest = ServiceOuterClass.BroadcastTxRequest.newBuilder()
			.setTxBytes(tx.toByteString())
			.setMode(ServiceOuterClass.BroadcastMode.BROADCAST_MODE_SYNC)
			.build();

		ServiceOuterClass.BroadcastTxResponse broadcastTxResponse = broadcastTx(broadcastTxRequest);

		if (!broadcastTxResponse.hasTxResponse()) {
			throw new Exception("broadcastTxResponse no body\n" + printer.print(tx));
		}
		Abci.TxResponse txResponse = broadcastTxResponse.getTxResponse();
		if (txResponse.getTxhash().length() != 64) {
			throw new Exception("Txhash illegal\n" + printer.print(tx));
		}
		return txResponse;
	}

	public ServiceOuterClass.BroadcastTxResponse broadcastTx(ServiceOuterClass.BroadcastTxRequest req) throws Exception {
		String reqBody = printer.print(req);
		ServiceOuterClass.BroadcastTxResponse broadcastTxResponse = client.post("/cosmos/tx/v1beta1/txs", reqBody, ServiceOuterClass.BroadcastTxResponse.class);
		return broadcastTxResponse;
	}

	public TxOuterClass.Tx getTxRequest(CosmosCredentials payerCredentials, List<SendInfo> sendList, BigDecimal feeInAtom, long gasLimit) throws Exception {
		Map<String, Auth.BaseAccount> baseAccountCache = new HashMap<>();
		TxOuterClass.TxBody.Builder txBodyBuilder = TxOuterClass.TxBody.newBuilder();
		TxOuterClass.AuthInfo.Builder authInfoBuilder = TxOuterClass.AuthInfo.newBuilder();

		TxOuterClass.Tx.Builder txBuilder = TxOuterClass.Tx.newBuilder();
		Map<String, Boolean> signerInfoExistMap = new HashMap<>();
		Map<String, Boolean> signaturesExistMap = new HashMap<>();
		for (SendInfo sendInfo : sendList) {
			BigInteger sendAmountInMicroAtom = ATOMUnitUtil.atomToMicroAtomBigInteger(sendInfo.getAmountInAtom());
			CoinOuterClass.Coin sendCoin = CoinOuterClass.Coin.newBuilder()
				.setAmount(sendAmountInMicroAtom.toString())
				.setDenom(this.token)
				.build();

			Tx.MsgSend message = Tx.MsgSend.newBuilder()
				.setFromAddress(sendInfo.getCredentials().getAddress())
				.setToAddress(sendInfo.getToAddress())
				.addAmount(sendCoin)
				.build();

			txBodyBuilder.addMessages(Any.pack(message, "/"));

			if (!signerInfoExistMap.containsKey(sendInfo.getCredentials().getAddress())) {
				authInfoBuilder.addSignerInfos(getSignInfo(sendInfo.getCredentials(), baseAccountCache));
				signerInfoExistMap.put(sendInfo.getCredentials().getAddress(), true);
			}

		}

		if (!signerInfoExistMap.containsKey(payerCredentials.getAddress())) {
			authInfoBuilder.addSignerInfos(getSignInfo(payerCredentials, baseAccountCache));
			signerInfoExistMap.put(payerCredentials.getAddress(), true);
		}

		CoinOuterClass.Coin feeCoin = CoinOuterClass.Coin.newBuilder()
			.setAmount(ATOMUnitUtil.atomToMicroAtom(feeInAtom).toPlainString())
			.setDenom(this.token)
			.build();

		String payerAddress = payerCredentials.getAddress();
		if (sendList.get(0).getCredentials().getAddress().equals(payerCredentials.getAddress())) {
			payerAddress = "";
		}
		TxOuterClass.Fee fee = TxOuterClass.Fee.newBuilder()
			.setGasLimit(gasLimit)
			.setPayer(payerAddress)
			.addAmount(feeCoin)
			.build();

		authInfoBuilder.setFee(fee);

		TxOuterClass.TxBody txBody = txBodyBuilder.build();

		TxOuterClass.AuthInfo authInfo = authInfoBuilder.build();

		for (SendInfo sendInfo : sendList) {
			if (!signaturesExistMap.containsKey(sendInfo.getCredentials().getAddress())) {
				txBuilder.addSignatures(getSignBytes(sendInfo.getCredentials(), txBody, authInfo, baseAccountCache));
				signaturesExistMap.put(sendInfo.getCredentials().getAddress(), true);
			}
		}
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

		Auth.BaseAccount baseAccount = queryBaseAccount(credentials.getAddress(), baseAccountCache);
		TxOuterClass.SignerInfo signerInfo = TxOuterClass.SignerInfo.newBuilder()
			.setPublicKey(Any.pack(pubKey, "/"))
			.setModeInfo(TxOuterClass.ModeInfo.newBuilder().setSingle(single))
			.setSequence(baseAccount.getSequence())
			.build();
		return signerInfo;
	}

	public Auth.BaseAccount queryBaseAccount(String address, Map<String, Auth.BaseAccount> cacheMap) throws Exception {
		if (cacheMap.containsKey(address)) {
			return cacheMap.get(address);
		}
		Auth.BaseAccount baseAccount = queryBaseAccount(address);
		cacheMap.put(address, baseAccount);
		return baseAccount;
	}

	public Auth.BaseAccount queryBaseAccount(String address) throws Exception {
		QueryAccountResponse res = queryAccount(address);
		if (res.hasAccount() && res.getAccount().is(Auth.BaseAccount.class)) {
			return res.getAccount().unpack(Auth.BaseAccount.class);
		}
		throw new RuntimeException("account not found:" + address);
	}

	public QueryAccountResponse queryAccount(String address) throws Exception {
		String path = String.format("/cosmos/auth/v1beta1/accounts/%s", address);
		return client.get(path, QueryAccountResponse.class);
	}

	public ByteString getSignBytes(CosmosCredentials credentials, TxOuterClass.TxBody txBody, TxOuterClass.AuthInfo authInfo, Map<String, Auth.BaseAccount> baseAccountCache) throws Exception {
		Auth.BaseAccount baseAccount = queryBaseAccount(credentials.getAddress(), baseAccountCache);
		byte[] sigBytes = signDoc(credentials.getEcKey().getPrivKeyBytes(), baseAccount, txBody, authInfo, this.chainId);
		return ByteString.copyFrom(sigBytes);
	}

	public static byte[] signDoc(byte[] privateKey, Auth.BaseAccount baseAccount, TxOuterClass.TxBody txBody, TxOuterClass.AuthInfo authInfo, String chainId) {
		ECKeyPair keyPair = ECKeyPair.create(privateKey);
		TxOuterClass.SignDoc signDoc = TxOuterClass.SignDoc.newBuilder()
			.setBodyBytes(txBody.toByteString())
			.setAuthInfoBytes(authInfo.toByteString())
			.setAccountNumber(baseAccount.getAccountNumber())
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
