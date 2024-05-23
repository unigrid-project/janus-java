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

package org.unigrid.janus;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import javafx.application.Application;
import javafx.stage.Stage;
import org.unigrid.janus.model.producer.HostServicesProducer;

public class ApplicationLoader extends Application {
	private BaseApplication application;

	@Override
	public void init() throws Exception {
		HostServicesProducer.setHostServices(getHostServices());
		// TODO remove this.. just adding to change the fx file size
		// and recompile new configs
		final SeContainer container = SeContainerInitializer.newInstance().initialize();
		application = container.select(Janus.class).get();
	}

	@Override
	public void start(Stage stage) throws Exception {
		application.start(stage, getParameters(), getHostServices());
	}
}
