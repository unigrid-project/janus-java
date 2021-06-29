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

package org.unigrid.janus;

import javafx.application.Application;
//import javax.annotation.PostConstruct;
//import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.jboss.weld.environment.se.Weld;
//import lombok.SneakyThrows;
import org.unigrid.janus.view.MainWindow;
//import org.springframework.context.ConfigurableApplicationContext;
import org.unigrid.janus.model.Daemon;
import org.unigrid.janus.model.Preferences;
import org.unigrid.janus.model.setup.Certificate;

public class Janus {
	@Inject
	private Daemon daemon;

	/*@PostConstruct @SneakyThrows
	private void init() {
		//daemon.startOrConnect();
	}

	@PreDestroy @SneakyThrows
	private void destroy() {
		//daemon.stopOrDisconnect();
	}*/

	public static void main(String[] args) throws Exception {
		final Weld weld = new Weld();
		final Certificate certificate = new Certificate();
		certificate.getCurrent();

		//final ConfigurableApplicationContext applicationContext = SpringApplication.run(Janus.class);

		/* Effectively changes the default values of these properties as used in JavaFX, we do this to speed up
		   refreshes and custom resizing of undecorated windows. */

		Preferences.changePropertyDefault(Boolean.class, "prism.vsync", false);
		Preferences.changePropertyDefault(String.class, "prism.order", "sw");

		Application.launch(MainWindow.class, args);
		weld.shutdown();
	}
}
