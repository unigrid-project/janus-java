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

import lombok.Getter;
import lombok.Setter;

public class UpdateURL {
	@Getter @Setter
	private static String linuxUrl = "https://raw.githubusercontent.com/unigrid-project/"
		+ "unigrid-update/main/legacy-linux.xml";

	@Getter @Setter
	private static String macUrl = "https://raw.githubusercontent.com/unigrid-project/"
		+ "unigrid-update/main/legacy-mac.xml";

	@Getter @Setter
	private static String windowsUrl = "https://raw.githubusercontent.com/unigrid-project/"
		+ "unigrid-update/main/legacy-windows.xml";

	@Getter
	private static String linuxTestUrl = "https://raw.githubusercontent.com/unigrid-project/"
		+ "unigrid-update-testing/main/legacy-linux-test.xml";

	@Getter
	private static String macTestUrl = "https://raw.githubusercontent.com/unigrid-project/"
		+ "unigrid-update-testing/main/legacy-mac-test.xml";

	@Getter
	private static String windowsTestUrl = "https://raw.githubusercontent.com/unigrid-project/"
		+ "unigrid-update-testing/main/legacy-windows-test.xml";

	@Getter @Setter
	private static String bootstrapUrl = "https://github.com/unigrid-project/janus-java/releases.atom";

}
