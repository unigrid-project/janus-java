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

package org.unigrid.janus.model.signal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.unigrid.janus.model.rpc.entity.ListUnspent;
import java.util.List;

@Data @Builder
public class MergeInputsRequest {
	private String address;
	private double amount;
	private List<ListUnspent.Result> utxos;

	@AllArgsConstructor
	public enum Type {
		MERGE("MERGE", "Merge your UTXOs by entering the necessary details and pressing the MERGE button.");

		@Getter private final String action;
		@Getter private final String description;
	}

	private Type type;
}
