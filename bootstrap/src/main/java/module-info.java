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

module org.unigrid.bootstrap {
	requires static lombok;
	requires javafx.controls;
	requires javafx.fxml;
	requires java.base;
	requires org.update4j;
	requires java.instrument;
	requires java.sql;
	requires jdk.security.auth;
	requires transitive java.xml;
	requires jdk.zipfs;
	requires java.compiler;
	requires jdk.crypto.ec;
	requires sentry;
	requires weld.environment.common;
	requires jakarta.inject;

	opens org.unigrid.bootstrap to javafx.fxml, org.update4j;
	opens org.unigrid.bootstrap.controller to javafx.fxml;

	exports org.unigrid.bootstrap to org.update4j, javafx.graphics;
	exports org.unigrid.bootstrap.controller;
}
