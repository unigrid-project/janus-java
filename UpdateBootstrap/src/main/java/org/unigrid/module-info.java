module org.unigrid.updatebootstrap {
	requires javafx.controls;
	requires javafx.fxml;
	requires java.base;
	requires org.update4j;
	requires java.instrument;
	requires java.sql;
	requires org.slf4j;
	requires ch.qos.logback.classic;
	
    opens org.unigrid.updatebootstrap to javafx.fxml, org.update4j;
    opens org.unigrid.updatebootstrap.controllers to javafx.fxml;
    
    exports org.unigrid.updatebootstrap to org.update4j, javafx.graphics;
    exports org.unigrid.updatebootstrap.controllers;
}
