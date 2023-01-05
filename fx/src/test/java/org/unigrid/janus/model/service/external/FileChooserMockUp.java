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

package org.unigrid.janus.model.service.external;

import java.io.File;
import java.io.IOException;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.lang3.RandomStringUtils;

public class FileChooserMockUp extends MockUp<FileChooser> {
	@Mock
	public File showOpenDialog(Window window) throws IOException {
		return File.createTempFile(RandomStringUtils.randomAlphabetic(20), "");
	}

	@Mock
	public File showSaveDialog(Window window) throws IOException {
		return File.createTempFile(RandomStringUtils.randomAlphabetic(20), "");
	}
}
