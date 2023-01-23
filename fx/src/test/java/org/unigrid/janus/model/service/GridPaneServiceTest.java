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

package org.unigrid.janus.model.service;

import jakarta.inject.Inject;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import mockit.Mocked;
import net.jqwik.api.Example;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;

public class GridPaneServiceTest extends BaseMockedWeldTest {
	@Inject private GridPaneService gridPaneService;

	@Mocked
	private DebugService debug;

	@Example
	public void shouldReturnChildrenInListOfRows() {
		GridPane gridPane = new GridPane();
		int row = 0;
		gridPane.add(new Label("Name"), 0, row, 1, 1);
		gridPane.add(new Label("Address"), 1, row, 1, 1);
		gridPane.add(new Label("Progress"), 2, row, 1, 1);
		gridPane.add(new Label("Output"), 3, row, 1, 1);
		row++;
		gridPane.add(new Label("ugd_docker_1"), 0, row, 1, 1);
		gridPane.add(new Label("127.0.0.1"), 1, row, 2, 1);
		gridPane.add(new Label("0.4"), 2, row, 1, 1);
		row++;
		gridPane.add(new Label("ugd_docker_1"), 0, row, 1, 1);
		gridPane.add(new Label("127.0.0.1"), 1, row, 1, 1);
		gridPane.add(new Label("0.4"), 2, row, 2, 1);
		gridPaneService.getChildrenAsRows(gridPane);
		System.out.println("gridpane" + gridPaneService.getChildrenAsRows(gridPane));
		System.out.println("size 3 ? ? ? size : " + gridPaneService.getChildrenAsRows(gridPane).size());
	}
}
