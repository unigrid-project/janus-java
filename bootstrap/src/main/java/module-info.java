module org.unigrid.bootstrap {
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

	opens org.unigrid.bootstrap to javafx.fxml, org.update4j;
	opens org.unigrid.bootstrap.controller to javafx.fxml;

	exports org.unigrid.bootstrap to org.update4j, javafx.graphics;
	exports org.unigrid.bootstrap.controller;
}
