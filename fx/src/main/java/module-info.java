/*
    The Janus Wallet
    Copyright © 2021-2022 The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
*/

module fx {
	requires static lombok;
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.fxml;
	requires org.apache.commons.lang3;
	requires org.apache.commons.configuration2;
	requires java.annotation;
	requires java.desktop;
	requires java.prefs;
	requires java.naming;
	requires jakarta.annotation;
	requires jakarta.cdi;
	requires jakarta.inject;
	requires jakarta.json.bind;
	requires jakarta.json;
	requires jakarta.ws.rs;
	requires com.sun.jna.platform;
	requires com.sun.jna;
	requires jersey.client;
	requires org.kordamp.ikonli.javafx;
	requires org.eclipse.yasson;

	requires jsch;
	requires java.sql;
	requires java.instrument;
	requires org.controlsfx.controls;
	requires static org.update4j;
	
	uses org.update4j.service.Launcher;
	provides org.update4j.service.Launcher with org.unigrid.janus.JanusLauncher;
	
	opens org.unigrid.janus to weld.core.impl, org.update4j;
	opens org.unigrid.janus.controller.component to javafx.fxml, javafx.base, javafx.controls, org.update4j, javafx.graphics;
	opens org.unigrid.janus.controller.view to javafx.fxml, weld.core.impl, javafx.base, javafx.controls, org.update4j, javafx.graphics;
	opens org.unigrid.janus.view to weld.core.impl;
	opens org.unigrid.janus.view.component to weld.core.impl, javafx.fxml, javafx.base, javafx.controls;
	opens org.unigrid.janus.model to weld.core.impl, javafx.base;
	opens org.unigrid.janus.model.rpc.entity to weld.core.impl, org.eclipse.yasson;
	opens org.unigrid.janus.model.service to weld.core.impl, org.update4j, org.apache.commons.configuration2;

	exports org.unigrid.janus;
	exports org.unigrid.janus.controller.component to weld.core.impl, javafx.fxml, javafx.base, javafx.controls, org.update4j, javafx.graphics;
	exports org.unigrid.janus.controller.view to weld.core.impl ,javafx.fxml, javafx.base, javafx.controls, org.update4j, javafx.graphics;
	exports org.unigrid.janus.model.event to weld.core.impl;
	exports org.unigrid.janus.model.producer to weld.core.impl;
	exports org.unigrid.janus.model.setup to weld.core.impl;
	exports org.unigrid.janus.model.rpc to weld.core.impl;
	exports org.unigrid.janus.view.component to weld.core.impl;
	exports org.unigrid.janus.view.decorator to weld.core.impl;
	exports org.unigrid.janus.model to org.eclipse.yasson;
}
