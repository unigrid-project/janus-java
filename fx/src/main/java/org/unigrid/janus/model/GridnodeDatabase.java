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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Cleanup;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.unigrid.janus.model.GridnodeDeployment.State;

@Slf4j
@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class GridnodeDatabase implements Serializable {
	public static final String GRIDNODE_DB_FILE = "gridnode.db";
	private List<GridnodeDeployment> gridnodeDeployments;

	public static GridnodeDatabase load(Path path) throws IOException {
		
		@Cleanup final InputStream stream = Files.newInputStream(path, StandardOpenOption.READ);
		final GridnodeDatabase db = SerializationUtils.deserialize(stream);
		
		System.out.println(db.gridnodeDeployments.get(0).getGridnodes());
		return db;
	}

	public static void persist(Path path, GridnodeDatabase gridnodeDatabase) throws IOException {
		@Cleanup final OutputStream stream = Files.newOutputStream(path,
			StandardOpenOption.CREATE, StandardOpenOption.WRITE
		);

		SerializationUtils.serialize(gridnodeDatabase, stream);
	}

	public boolean add(GridnodeDeployment deployment) {
		return gridnodeDeployments.add(deployment);
	}

	public Optional<GridnodeDeployment> get(GridnodeDeployment gridnodeDeployment) {
		for (GridnodeDeployment gd : gridnodeDeployments) {
			if (gd.equals(gridnodeDeployment)) {
				return Optional.of(gd);
			}
		}

		return Optional.empty();
	}

	public boolean contains(Gridnode gridnode) {
		return getIndividualGridnodes().contains(gridnode);
	}

	public Optional<GridnodeDeployment> getNewlyDeployed() {
		for (GridnodeDeployment gd : gridnodeDeployments) {
			if (gd.isNewlyDeployed()) {
				return Optional.of(gd);
			}
		}

		return Optional.empty();
	}

	public List<Pair<Gridnode, State>> getIndividualGridnodesWithState() {
		final List<Pair<Gridnode, State>> nodesToReturn = new ArrayList<>();

		for (GridnodeDeployment gd : gridnodeDeployments) {
			synchronized (gd.getGridnodes()) {
				for (Gridnode n : gd.getGridnodes().keySet()) {
					nodesToReturn.add(Pair.of(n, gd.getGridnodes().get(n)));
				}
			}
		}

		Collections.sort(nodesToReturn);
		return nodesToReturn;
	}

	public void setIndividualGridnodeState(Gridnode gridnode, State state) {
		for (GridnodeDeployment gd : gridnodeDeployments) {
			synchronized (gd.getGridnodes()) {
				for (Gridnode n : gd.getGridnodes().keySet()) {
					if (n.equals(gridnode)) {
						gd.getGridnodes().put(gridnode, state);
					}
				}
			}
		}
	}

	public boolean isDeployingGridnode(GridnodeDeployment gridnodeDeployment) {
		return gridnodeDeployment.getGridnodes().values().stream().filter(n -> n.equals(State.THREE_DEPLOYMENT)).
			count() != 0;
	}

	public List<Gridnode> getIndividualGridnodes() {
		final List<Pair<Gridnode, State>> nodesToReturn = getIndividualGridnodesWithState();
		return nodesToReturn.stream().map(n -> n.getLeft()).toList();
	}

	public Optional<GridnodeDeployment> getParent(Gridnode gridnode) {
		return gridnodeDeployments.stream().filter(n -> n.getGridnodes().containsKey(gridnode)).findFirst();
	}

	public List<Output> getAvailableOutputs(List<Output> outputs) {
		final List<Gridnode> nodes = getIndividualGridnodes();

		final List<Output> usedOutputs = nodes.stream().map(n ->
			new Output(n.getTxHash(), n.getOutputIndex())
		).toList();

		final List<Output> resultingOutputs = new ArrayList<>(outputs);
		resultingOutputs.removeAll(usedOutputs);
		return resultingOutputs;
	}
}
