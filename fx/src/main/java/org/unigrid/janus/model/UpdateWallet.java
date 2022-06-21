/*
	The Janus Wallet
	Copyright © 2021 The Unigrid Foundation

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

import java.io.IOException;
import java.util.TimerTask;
import javafx.beans.property.SimpleBooleanProperty;
import org.update4j.Configuration;

public class UpdateWallet extends TimerTask {
	
	private Configuration config;
	private SimpleBooleanProperty running;
	
	@Override
	public void run() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}
		
	private Boolean checkUpdate() {
		boolean update = false;
		try {
			update = config.requiresUpdate();
			
		}
		catch(IOException e) {
			update = false;
		}
		
		return update;
	}
	
	private void doUpdate(){
		running.set(true);
		
		
	}

}
