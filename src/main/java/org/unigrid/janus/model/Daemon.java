package org.unigrid.janus.model;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javax.annotation.PostConstruct;
import javax.naming.ConfigurationException;
import javax.transaction.Transactional;
import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class Daemon {
	private static final String PROPERTY_LOCATION_KEY = "janus.daemon.location";
	private static final String PROPERTY_LOCATION = Preferences.PROPS.getString(PROPERTY_LOCATION_KEY);
	private Optional<Process> process = Optional.empty();
	@Getter private User rpcCredentials;

	@PostConstruct
	private void init() {
		rpcCredentials = new User();
		rpcCredentials.setName(RandomStringUtils.randomAlphabetic(30));
		rpcCredentials.setPassword(RandomStringUtils.randomAlphabetic(50));
	}

	private boolean isLocalFile(String path) {
		return new File(path).exists();
	}

	private void start() throws IOException {
		process = Optional.of(Runtime.getRuntime().exec(new String[]{PROPERTY_LOCATION}));
	}

	private void connect() {
		/* TODO: Implement connecting to remote instances */
	}

	public void startOrConnect() throws ConfigurationException, IOException {
		if (StringUtils.isNotBlank(PROPERTY_LOCATION)) {
			if (isLocalFile(PROPERTY_LOCATION)) {
				start();
			} else {
				connect();
			}
		} else {
			new Alert(AlertType.NONE).show();
			throw new ConfigurationException(("No location to the daemon specified in property '%s'. "
				+ "This should point to either a local file, "
				+ "or a remote http location.").formatted(PROPERTY_LOCATION_KEY)
			);
		}
	}

	public void stopOrDisconnect() throws InterruptedException {
		if (process.isPresent()) {
			process.get().destroy();
			process.get().waitFor();
		}

		/* TODO: Implement disconnecting from remote instances whenever we are not on a local daemon */
	}
}
