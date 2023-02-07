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
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;

public class DebugServiceTest extends BaseMockedWeldTest {

	private final PrintStream standardOut = System.out;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

	@Inject private DebugService debugService;

	public boolean testFormattedCurrentDate() {
		String currentDate = new SimpleDateFormat("yyyy.MM.dd.HH.mm").format(new Date());
		return currentDate.equals(debugService.getCurrentDate());
	}

	/*@Example
	public void testTrace() {
		System.setOut(new PrintStream(outputStreamCaptor));

		debugService.trace("Hello!!!");

		assertThat("Hello!!!", equalTo(outputStreamCaptor.toString()
			.trim()));

		System.setOut(standardOut);
	}*/

}
