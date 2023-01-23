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

package org.unigrid.janus.model;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gridnode implements Serializable, Comparable<Gridnode>, ObservableCollectionMember {
	private String alias;
	private String address;
	private String privateKey;
	private int outputIndex;
	private String txHash;
	private Status status;
	private boolean availableTxhash;

	public void setOutput(Output output) {
		this.outputIndex = output.getOutputidx();
		this.txHash = output.getTxhash();
	}

	public enum Status {
		ENABLED, EXPIRED, MISSING, OUTPOINT_SPENT, POSE_BAN, PRE_ENABLED, REMOVE, UNKNOWN, WATCHDOG_EXPIRED
	}

	@Override
	public int compareTo(Gridnode n) {
		final int addressCompare = this.address.compareTo(n.address);

		if (addressCompare == 0) {
			return this.alias.compareTo(n.alias);
		} else {
			return addressCompare;
		}
	}

	public static boolean isGridnodeConfAdded(List<Gridnode> list, Gridnode gridnode) {
		if (gridnode.getPrivateKey() == null) {
			return false;
		}

		return list.stream().filter(n -> n.getPrivateKey().equals(gridnode.getPrivateKey())
			&& n.getOutputIndex()== gridnode.getOutputIndex()
		).count() != 0;
	}
	
	public boolean isGridnodeDeployed() {
		return alias != null && address != null && privateKey != null && txHash != null ? true : false;
	}

	@Override
	public int observableHashCode() {
		return new HashCodeBuilder()
			.append(alias)
			.append(address)
			.append(privateKey)
			.append(outputIndex)
			.append(txHash)
			.append(status)
			.append(availableTxhash).build();
	}


	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
}
