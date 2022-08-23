module org.unigrid.updatebootstrap {
	requires javafx.controls;
	requires javafx.fxml;
	requires java.base;
	requires org.update4j;
	requires java.instrument;
	requires java.sql;
	//requires static fx;
	
	//provides fx with org.update4j.service.DefaultLauncher;

	
    opens org.unigrid.updatebootstrap to javafx.fxml, org.update4j, fx;
    
    exports org.unigrid.updatebootstrap to org.update4j, javafx.graphics, fx;
    exports org.unigrid.updatebootstrap.controllers;
}
