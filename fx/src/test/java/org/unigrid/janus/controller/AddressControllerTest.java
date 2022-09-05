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

package org.unigrid.janus.controller;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.TableViewMatchers.hasTableCell;
//import static org.testfx.matcher.control.TextMatchers.hasText;
import org.unigrid.janus.FXApplicationTest;
import org.unigrid.janus.model.UpdateWallet;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.external.ResponseMockUp;
import org.unigrid.janus.model.service.Daemon;
import org.unigrid.janus.model.service.DaemonMockUp;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.PollingService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.external.JerseyInvocationMockUp;
import org.unigrid.janus.model.service.external.WebTargetMockUp;

@Slf4j
@ExtendWith(WeldJunit5Extension.class)
public class AddressControllerTest extends FXApplicationTest {

	@WeldSetup
	private static WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
		.beanClasses(AddressController.class, DebugService.class,
			UpdateWallet.class, PollingService.class, RPCService.class, Daemon.class
		)
	);

	@Inject
	private RPCService rpc;
/*
	@Test
	public void testGenerateAddress() {
		new ResponseMockUp();
		new JerseyInvocationMockUp();
		new WebTargetMockUp();
		new DaemonMockUp();
		Eager.instantiate(rpc);

		clickOn("#btnAddress");
		clickOn("#btnGenerateAddress");
		verifyThat("#addressDisplay", hasText("A7HitmzXEMPL3P7McZABtm9BuS3t9eZaf4"));
	}
*/
	@Test
	public void testHideZeroBalances() {
		new ResponseMockUp();
		new JerseyInvocationMockUp();
		new WebTargetMockUp();
		new DaemonMockUp();
		Eager.instantiate(rpc);

		clickOn("#btnAddress");
		clickOn("#chkAddress");
		verifyThat("#tblAddresses", hasTableCell(2000.0));
		clickOn("#chkAddress");
		verifyThat("#tblAddresses", hasTableCell(0.0));
		verifyThat("#tblAddresses", hasTableCell(2000.0));
	}
}
