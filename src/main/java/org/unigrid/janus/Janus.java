package org.unigrid.janus;

import javafx.application.Application;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.unigrid.janus.fx.view.MainWindow;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.unigrid.janus.model.Daemon;
import org.unigrid.janus.model.Preferences;
import org.unigrid.janus.model.setup.Certificate;

@SpringBootApplication
public class Janus {
	@Inject
	private Daemon daemon;

	@PostConstruct @SneakyThrows
	private void init() {
		daemon.startOrConnect();
	}

	@PreDestroy @SneakyThrows
	private void destroy() {
		daemon.stopOrDisconnect();
	}

	public static void main(String[] args) throws Exception {
		final Certificate certificate = new Certificate();
		certificate.getCurrent();

		final ConfigurableApplicationContext applicationContext = SpringApplication.run(Janus.class);

		/* Effectively changes the default values of these properties as used in JavaFX, we do this to speed up
		   refreshes and custom resizing of undecorated windows. */

		Preferences.changePropertyDefault(Boolean.class, "prism.vsync", false);
		Preferences.changePropertyDefault(String.class, "prism.order", "sw");

		Application.launch(MainWindow.class, args);
		applicationContext.stop();
	}
}
