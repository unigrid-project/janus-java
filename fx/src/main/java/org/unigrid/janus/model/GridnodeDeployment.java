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
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.unigrid.janus.model.GridnodeDeployment.Authentication;

@Builder
@EqualsAndHashCode
public class GridnodeDeployment implements Serializable, ObservableCollectionMember {
	@Getter @Builder.Default
	@EqualsAndHashCode.Include
	private SerializableOptional<Authentication> authentication = SerializableOptional.empty();

	@Getter @Builder.Default private Map<Gridnode, State> gridnodes = Collections.synchronizedMap(new HashMap());
	@Getter @Setter @Builder.Default private int count = 0;
	@Getter @Setter @Builder.Default private transient Optional<SSHConnection> connection = Optional.empty();

	@Data @Builder
	public static class Authentication implements Serializable {
		@EqualsAndHashCode.Exclude private String username;
		@EqualsAndHashCode.Exclude private String password;
		private InetSocketAddress address;

		public static class AuthenticationBuilder {
			public AuthenticationBuilder auth(String username, String password, InetSocketAddress address) {
				this.username = username;
				this.password = password;
				this.address = address;

				return this;
			}

		}
	}

	public enum State {
		ONE_PREDEPLOYING, TWO_PENDING, THREE_DEPLOYMENT, FOUR_DEPLOYED, FIVE_POSTDEPLOYMENT;
	}

	public boolean isNewlyDeployed() {
		return gridnodes.isEmpty();
	}

	@Override
	public int observableHashCode() {
		HashCodeBuilder builder = new HashCodeBuilder()
			.append(authentication)
			.append(count)
			.append(connection)
			.append(super.hashCode());

		for (Gridnode gridnode : gridnodes.keySet()) {
			builder = builder.append(gridnode.observableHashCode());
		}

		for (State state : gridnodes.values()) {
			builder = builder.append(state);
		}

		if (connection.isPresent()) {
			builder.append(connection.get().isConnected());
		}

		int hashCode = builder.build();
		System.out.println("builder hashcode: " + hashCode);

		return hashCode;
	}
}