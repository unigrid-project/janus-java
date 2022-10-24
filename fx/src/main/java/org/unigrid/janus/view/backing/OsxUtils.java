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

package org.unigrid.janus.view.backing;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.unigrid.janus.model.DataDirectory;

public class OsxUtils {

    public void openFileOsx(String file) {
		File path = new File(DataDirectory.get());
		String[] cmds = new String[3];
		cmds[0] = "open";
		cmds[1] = "-t";
		cmds[2] = file;
		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.directory(path);
		try {
			pb.start();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

    public void openDirectory(String directory) throws IOException {
        if(!Desktop.isDesktopSupported()) {
            System.out.println("Cannot open data directory as desktop is not supported");
            return;
        }

        File location = new File(directory);
        Desktop desktop = Desktop.getDesktop();
        if(location.exists()) {
            desktop.open(location);
        }

    }
}
