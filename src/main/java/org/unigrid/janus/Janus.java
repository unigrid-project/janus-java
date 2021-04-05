package org.unigrid.janus;

import javafx.application.Application;
import org.unigrid.janus.fx.view.MainWindow;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.unigrid.janus.model.Preferences;
import org.unigrid.janus.model.setup.Certificate;

@SpringBootApplication
public class Janus {
	public static void main(String[] args) throws Exception  {
		final Certificate certificate = new Certificate();
		certificate.getCurrent();

		/* Effectively changes the default values of these properties as used in JavaFX, we do this to speed up
		   refreshes and custom resizing of undecorated windows. */

		Preferences.changePropertyDefault(Boolean.class, "prism.vsync", false);
		Preferences.changePropertyDefault(String.class, "prism.order", "sw");

		Application.launch(MainWindow.class, args);
	}
}
