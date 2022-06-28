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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TimerTask;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import lombok.Getter;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.Notifications;
import org.unigrid.janus.model.cdi.Eager;
import org.update4j.Configuration;

@Eager
@ApplicationScoped
public class UpdateWallet extends TimerTask {
	
	public enum UpdateState {
		UPDATE_READY,
		UPDATE_NOT_READY
	}
	
	@Getter
	private final String UPDATE_PROPERTY = "update";
	private UpdateState oldValue = UpdateState.UPDATE_NOT_READY;
	private Configuration config;
	private SimpleBooleanProperty running;
	private PropertyChangeSupport pcs;
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}
	
	@PostConstruct
	private void init() {
	
		if(this.pcs != null) {
			return;
		}
		this.pcs = new PropertyChangeSupport(this);

		URL configUrl = null;
		
		try {
			configUrl = new URL("https://raw.githubusercontent.com/Fim-84/test/main/config.xml");		
		}
		catch(MalformedURLException mle) {
			System.out.println("Unable to find url to config.xml");
			System.err.println(mle.getMessage());
		}
		Configuration config = null;
		
		
		
		try(Reader in = new InputStreamReader(configUrl.openStream(), StandardCharsets.UTF_8)) {
			config = Configuration.read(in);
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
			try(Reader in = Files.newBufferedReader(Paths.get("/home/marcus/Documents/unigrid/config/UpdateWalletConfig/config.xml"))) {
				System.out.println("reading local config xml");
				config = Configuration.read(in);
			}
			catch(IOException ioe) {
				
			}
		}
	}
	
	@Override
	public void run() {
		if(checkUpdate()) {
			this.pcs.firePropertyChange(this.UPDATE_PROPERTY, oldValue, UpdateState.UPDATE_READY);
			if(SystemUtils.IS_OS_MAC_OSX){
				Notifications
					.create()
					.title("Update Ready")
					.text("New update ready")
					.position(Pos.TOP_RIGHT)
					.showInformation();
			}
			else {
				Notifications
					.create()
					.title("Update Ready")
					.text("New update ready")
					.showInformation();
			}
		}
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
