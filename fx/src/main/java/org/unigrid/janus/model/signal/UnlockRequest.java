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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data @Builder
public class UnlockRequest {
	@AllArgsConstructor
	public enum Type {
		ORDINARY("UNLOCK", 2, "Unlock your wallet by entering your passphrase "
			+ "and pressing the UNLOCK button."),

		TIMED("UNLOCK", 4, "Please enter your passphrase in order to perform "
			+ "this task. The wallet will automatically lock itself after 30 seconds."),

		FOR_DUMP("EXPORT", 5, "Please enter your passphrase to export your private keys. If your "
			+ "wallet was staking you will need to enable again after the task completes."),

		FOR_GRIDNODE("START", 4, "Please enter your passphrase to enable your gridnodes. If your "
			+ "wallet was staking you will need to enable again after the task completes."),

		FOR_SEND("SEND", 3, "Please enter your passphrase to send Unigrid tokens. If your wallet "
			+ "was staking you will need to enable again after the transaction completes."),

		FOR_STAKING("STAKE", 1, "Enable staking in your wallet by entering your passphrase and "
			+ "pressing the STAKE button.");

		@Getter private final String action;
		@Getter private final int state;
		@Getter private final String description;
	}

	private Type type;
}
