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

package org.unigrid.janus.model.rpc.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.List;

@Data
@ApplicationScoped
@Named("transactionResponse")
public class TransactionResponse extends BaseResult<List<TransactionResponse.TxResponse>> {

	@JsonProperty("txs")
	private List<Transaction> transactions;
	@JsonProperty("tx_responses")
	private List<TxResponse> txResponsesList;

	@Data
	public static class Result extends TransactionResponse {
		// empty for now
	}

	@Data
	public static class Transaction {
		@JsonProperty("body")
		private Body body;
	}

	@Data
	public static class Body {
		@JsonProperty("messages")
		private List<Message> messages;
	}

	@Data
	public static class Message {
		@JsonProperty("from_address")
		private String fromAddress;

		@JsonProperty("to_address")
		private String toAddress;
	}

	@Data
	public static class TxResponse {
		@JsonProperty("height")
		private String height;

		@JsonProperty("txhash")
		private String txhash;

		@JsonProperty("codespace")
		private String codespace;

		@JsonProperty("code")
		private int code;

		@JsonProperty("data")
		private String data;

		@JsonProperty("raw_log")
		private String rawLog;

		@JsonProperty("logs")
		private List<Log> logs;

		@JsonProperty("info")
		private String info;

		@JsonProperty("gas_wanted")
		private String gasWanted;

		@JsonProperty("gas_used")
		private String gasUsed;

		@JsonProperty("tx")
		private Tx tx;

		@JsonProperty("timestamp")
		private String timestamp;

		@JsonProperty("events")
		private List<Event> events;

		@Data
		public static class Log {
			@JsonProperty("msg_index")
			private int msgIndex;

			@JsonProperty("log")
			private String log;

			@JsonProperty("events")
			private List<Event> events;
		}

		@Data
		public static class Tx {
			@JsonProperty("@type")
			private String type;

			@JsonProperty("body")
			private Body body;

			@JsonProperty("auth_info")
			private AuthInfo authInfo;

			@JsonProperty("signatures")
			private List<String> signatures;
		}

		@Data
		public static class Body {
			@JsonProperty("messages")
			private List<Message> messages;

			@JsonProperty("memo")
			private String memo;

			@JsonProperty("timeout_height")
			private String timeoutHeight;

			@JsonProperty("extension_options")
			private List<String> extensionOptions;

			@JsonProperty("non_critical_extension_options")
			private List<String> nonCriticalExtensionOptions;
		}

		@Data
		public static class AuthInfo {
			@JsonProperty("signer_infos")
			private List<SignerInfo> signerInfos;

			@JsonProperty("fee")
			private Fee fee;

			@JsonProperty("tip")
			private Object tip;
		}

		@Data
		public static class SignerInfo {
			@JsonProperty("public_key")
			private PublicKey publicKey;

			@JsonProperty("mode_info")
			private ModeInfo modeInfo;

			@JsonProperty("sequence")
			private String sequence;
		}

		@Data
		public static class PublicKey {
			@JsonProperty("@type")
			private String type;

			@JsonProperty("key")
			private String key;
		}

		@Data
		public static class ModeInfo {
			@JsonProperty("single")
			private Single single;
		}

		@Data
		public static class Single {
			@JsonProperty("mode")
			private String mode;
		}

		@Data
		public static class Fee {
			@JsonProperty("amount")
			private List<Amount> amount;

			@JsonProperty("gas_limit")
			private String gasLimit;

			@JsonProperty("payer")
			private String payer;

			@JsonProperty("granter")
			private String granter;
		}

		@Data
		public static class Amount {
			@JsonProperty("denom")
			private String denom;

			@JsonProperty("amount")
			private String amount;
		}

		@Data
		public static class Event {
			@JsonProperty("type")
			private String type;

			@JsonProperty("attributes")
			private List<Attribute> attributes;
		}

		@Data
		public static class Attribute {
			@JsonProperty("key")
			private String key;

			@JsonProperty("value")
			private String value;

			@JsonProperty("index")
			private boolean index;
		}
	}

}
