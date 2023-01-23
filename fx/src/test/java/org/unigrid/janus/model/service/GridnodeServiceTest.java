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

package org.unigrid.janus.model.service;

import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import mockit.Mock;
import mockit.MockUp;
import lombok.SneakyThrows;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.BeforeContainer;
import static com.shazam.shazamcrest.matcher.Matchers.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;
import org.unigrid.janus.jqwik.NotNull;
import org.unigrid.janus.model.Gridnode;
import org.unigrid.janus.model.GridnodeDatabase;
import org.unigrid.janus.model.DataDirectory;

public class GridnodeServiceTest extends BaseMockedWeldTest {
	@Inject
	private GridnodeService gridnodeService;

	@Inject
	private GridnodeDatabase gridnodeDatabase;

	@BeforeContainer
	public static void before() throws IOException {
		new MockUp<DataDirectory>() {
			final Path path = Files.createTempDirectory("unigrid_");

			@Mock
			public String get() {
				return path.toString();
			}
		};
	}

	@Provide
	public Arbitrary<Set<Gridnode>> provideGridnode(
		@ForAll @AlphaChars @StringLength(min = 1, max = 10) String alias,
		@ForAll @AlphaChars @StringLength(value = 32) String address,
		@ForAll @AlphaChars @StringLength(value = 32) String privateKey,
		@ForAll @IntRange(min = 0, max = 99) int outputidx,
		@ForAll @AlphaChars @StringLength(value = 64) String txhash,
		@ForAll Gridnode.Status status,
		@ForAll boolean availableTxhash) {

		return provide(alias, address, privateKey, outputidx, txhash, status, availableTxhash).set().ofMaxSize(3);
	}

	public Arbitrary<Gridnode> provide(String alias, String address, String privateKey, int outputidx, String txhash,
		Gridnode.Status status, boolean availableTxhash) {

		/*final Gridnode gridnode = new Gridnode();

		gridnode.setAlias(alias);
		gridnode.setAddress(address);
		gridnode.setPrivateKey(privateKey);
		gridnode.setOutputidx(outputidx);
		gridnode.setTxhash(txhash);
		gridnode.setStatus(status);
		gridnode.setAvailableTxhash(availableTxhash);

		return Arbitraries.of(gridnode);*/
		return null;
	}

	@SneakyThrows
	private GridnodeDatabase db(Path path) {
		GridnodeDatabase database;

		if (Files.exists(path)) {
			database = GridnodeDatabase.load(path);
		} else {

			database = GridnodeDatabase.builder().build();
		}

		if (Objects.isNull(database.getGridnodeDeployments())) {
			database.setGridnodeDeployments(Collections.synchronizedList(new ArrayList<>()));
		}

		return database;
	}
/*
	@SneakyThrows
	@Property(tries = 5)
	public void shouldLoadAndPersistData(@ForAll("provideGridnode") @NotNull Set gridnode) {
		gridnodeService.toString();
		final Path path = Path.of(DataDirectory.get(), GridnodeDatabase.GRIDNODE_DB_FILE);
		final GridnodeDatabase gridnodekDatabase = db(path);

		gridnodekDatabase.setGridnodeDeployments(gridnode);
		GridnodeDatabase.persist(path, gridnodekDatabase);
		final GridnodeDatabase deserializedSporkDatabase = GridnodeDatabase.load(path);

		assertThat(deserializedSporkDatabase, sameBeanAs(gridnodekDatabase));
		assertThat(deserializedSporkDatabase, equalTo(gridnodekDatabase));
	}*/
}
