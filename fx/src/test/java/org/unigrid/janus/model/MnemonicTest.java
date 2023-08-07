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

import com.sun.jna.platform.win32.KnownFolders;
import com.sun.jna.platform.win32.Shell32Util;
import jakarta.inject.Inject;
import java.nio.file.Paths;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;
import org.unigrid.janus.jqwik.WeldSetup;
import net.jqwik.api.Example;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.lang3.SystemUtils;

@WeldSetup(DeprecatedUpdateWalletModel.class)
public class MnemonicTest extends BaseMockedWeldTest {

	@Inject
	private FxMnemonic mnemonic;

	@Example
	public void shouldMakeANewWallet() {
		new Mockup<DataDirectory>() {
			private static final String APPLICATION_NAME = "UNIGRID";
			public static final String CONFIG_FILE = "unigrid.conf";
			public static final String GRIDNODE_FILE = "gridnode.conf";
			public static final String DEBUG_LOG = "debug.log";
			private static final String OSX_SUPPORT_DIR = "Library/Application Support";

			public static final String DATADIR_CONFIG_RPCUSER_KEY = "rpcuser";
			public static final String DATADIR_CONFIG_RPCPASSWORD_KEY = "rpcpassword";

			@Mock
			public static String get() {
				String head;
				String tail;

				if (SystemUtils.IS_OS_WINDOWS) {
					head = Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_RoamingAppData);
					tail = APPLICATION_NAME;
					System.out.println("OS is windows");
				} else {
					head = SystemUtils.getUserHome().getAbsolutePath();

					if (SystemUtils.IS_OS_MAC_OSX) {
						head = Paths.get(head, OSX_SUPPORT_DIR).toString();
						tail = APPLICATION_NAME;
					} else {
						tail = Paths.get(".".concat(APPLICATION_NAME).toLowerCase()).toString();
					}
				}

				return Paths.get(head, tail).toString();
			}
		};
			
		String mnemonic = mnemonic.generateWallet("123456", true);

		System.out.println(mnemonic);
	}
}
