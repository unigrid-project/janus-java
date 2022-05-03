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

module org.unigrid.janus {
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
	requires jakarta.ws.rs;
	requires com.sun.jna.platform;
	requires com.sun.jna;
	requires jersey.client;
	requires org.kordamp.ikonli.javafx;
	requires jsch;
	requires org.controlsfx.controls;
}
