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

package org.unigrid.janus.model.producer;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.GridnodeDatabase;
import org.unigrid.janus.model.signal.NodeUpdate;

@Slf4j
@ApplicationScoped
public class GridnodeDatabaseProducer {
	@Inject
	private Event<NodeUpdate> nodeUpdateEvent;

	private GridnodeDatabase gridnodeDatabase;

	private Path path() {
		return Path.of(DataDirectory.get(), GridnodeDatabase.GRIDNODE_DB_FILE);
	}

	@Produces
	private GridnodeDatabase produce() {
		try {
			Files.createDirectories(Path.of(DataDirectory.get()));
			gridnodeDatabase = GridnodeDatabase.load(path());

		} catch (IOException ex) {
			gridnodeDatabase = GridnodeDatabase.builder().build();
			log.atWarn().log("Creating fresh gridnode database: {}", ex.getMessage());
			log.atTrace().log(() -> ex.toString());
		}
		try {
			if (Objects.isNull(gridnodeDatabase.getGridnodeDeployments())) {
				gridnodeDatabase.setGridnodeDeployments(Collections.synchronizedList(new ArrayList<>()));
			}
		} catch (NullPointerException en) {
			en.printStackTrace();
		}

		return gridnodeDatabase;
	}

	@PreDestroy
	private void destroy() {
		try {
			Files.createDirectories(Path.of(DataDirectory.get()));
			GridnodeDatabase.persist(path(), gridnodeDatabase);

		} catch (Exception ex) {
			log.atWarn().log("Creating fresh gridnode database: {}", ex.getMessage());
			log.atTrace().log(() -> ex.toString());
		}
	}
}
