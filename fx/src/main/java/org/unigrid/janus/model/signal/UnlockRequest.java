/*
    The Janus Wallet
    Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model.signal;

import java.util.List;

import org.unigrid.janus.model.rpc.entity.ListUnspent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data @Builder
@AllArgsConstructor
public class UnlockRequest {
	public enum Type {
		ORDINARY("UNLOCK", "Unlock your wallet by entering your passphrase and "
			+ "pressing the UNLOCK button."),
		TIMED("UNLOCK", "Please enter your passphrase in order to perform this task. "
			+ "The wallet will automatically lock itself after 30 seconds."),
		FOR_DUMP("EXPORT", "Please enter your passphrase to export your private keys. "
			+ "If your wallet was staking, you will need to enable it again after the task completes."),
		FOR_GRIDNODE("START", "Please enter your passphrase to enable your grid nodes. "
			+ "If your wallet was staking, you will need to enable it again after the task completes."),
		FOR_SEND("SEND", "Please enter your passphrase to send Unigrid tokens. If your "
			+ "wallet was staking, you will need to enable it again after the transaction completes."),
		FOR_STAKING("STAKE", "Enable staking in your wallet by entering your passphrase "
			+ "and pressing the STAKE button."),
		FOR_MERGING("MERGE", "Enable auto-merging inputs in your wallet by entering your "
			+ "passphrase and pressing the MERGE button."),
		COSMOS_SEND_TOKENS("SEND", "Enter account password to complete the transaction"),
		COSMOS_DELEGATE_GRIDNODE("DELEGATE", "Enter account password to complete the transaction"),
		COSMOS_UNDELEGATE_GRIDNODE("UNDELEGATE", "Enter account password to complete the transaction"),
		COSMOS_DELEGATE_STAKING("STAKE", "Enter account password to complete the transaction"),
		COSMOS_CLAIM_REWARDS("CLAIM", "Enter account password to claim rewards"),
		COSMOS_GRIDNODE_KEYS("GENERATE", "Enter account password to generate gridnode keys"),
		COSMOS_GRIDNODE_START("START", "Enter account password to start your gridnode"),
		COSMOS_UNDELEGATE_STAKING("UNSTAKE", "Enter account password to unstake"),
		COSMOS_SWITCH_DELEGATOR("SWITCH", "Enter account password to switch delegator");

		@Getter private final String action;
		@Getter private final String description;

		Type(String action, String description) {
			this.action = action;
			this.description = description;
		}
	}

	private String address;
	private double amount;
	private List<ListUnspent.Result> utxos;

	private Type type;
}
