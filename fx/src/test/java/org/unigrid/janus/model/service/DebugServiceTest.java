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
import net.jqwik.api.Example;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;

public class DebugServiceTest extends BaseMockedWeldTest {

	private final PrintStream standardOut = System.out;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

	@Inject private DebugService debugService;

	// test that debugService is not null
	@Example
	public void testDebugServiceNotNull() {
		assertThat(debugService, equalTo(debugService));
	}

	// test that debugService.getCurrentDate() returns a string with the current date
	@Example
	void testGetCurrentDate() {
		String currentDate = new SimpleDateFormat("yyyy.MM.dd.HH.mm").format(new Date());
		String date = debugService.getCurrentDate();

		assertThat(date, equalTo(currentDate));
	}

	// test that debugService.print() prints a message to the console
	@Example
	void testPrint() {
		System.setOut(new PrintStream(outputStreamCaptor));
		debugService.print("test", "DebugServiceTest");
		System.setOut(standardOut);

		assertThat(outputStreamCaptor.toString().trim(), equalTo("test"));
	}

	@Example
	public void testTrace() {
		System.setOut(new PrintStream(outputStreamCaptor));
		debugService.trace("test");
		System.setOut(standardOut);

		assertThat(outputStreamCaptor.toString().trim(), equalTo("test"));
	}

}
