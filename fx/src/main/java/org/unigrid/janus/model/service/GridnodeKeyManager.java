

package org.unigrid.janus.model.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.DataDirectory;
import java.util.Collections;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import org.unigrid.janus.model.gridnode.GridnodeModel;
import org.unigrid.janus.model.signal.GridnodeKeyUpdateModel;
import org.unigrid.janus.model.signal.PublicKeysEvent;

@ApplicationScoped
public class GridnodeKeyManager {

	@Inject
	private AccountsData accountsData;

	@Inject
	private GridnodeModel gridnodeModel;

	@Inject
	private GridnodeKeyUpdateModel gridnodeKeyUpdateModel;

	@Inject
	private Event<GridnodeKeyUpdateModel> gridnodeKeyUpdateModelEvent;

	@Inject
	private Event<PublicKeysEvent> publicKeysEvent;

	@Getter @Setter
	private List<String> keys;

	public void initializeAndLoadKeys() {
		if (accountsData != null && accountsData.getSelectedAccount() != null) {
			this.keys = loadKeysFromFile();
			checkAndGenerateNewKeysIfNeeded();
		} else {
			System.out.println("accountsData is not initialized yet.");
			// Handle the case where accountsData is not available
		}
	}

	public List<String> loadKeysFromFile() {
		System.out.println("loading keyfile for gridnodes");

		String accountName = accountsData.getSelectedAccount().getName();
		File keysFile = DataDirectory.getGridnodeKeysFile(accountName);
		setKeys(Collections.emptyList());

		if (keysFile.exists()) {
			try {
				keys = DataDirectory.readPublicKeysFromFile(keysFile);
			} catch (IOException e) {
				System.out.println("Error reading keys from file: " + e.getMessage());
			}
		} else {
			System.out.println("send signal to change message to the user for generate keys");
		}
		updateAdditionalKeysPossible();
		// Fire an event with the loaded keys
		publicKeysEvent.fire(new PublicKeysEvent(keys));

		return keys;
	}

	private void checkAndGenerateNewKeysIfNeeded() {
		int currentKeyCount = keys.size();
		int potentialKeyCount = gridnodeModel.getPossibleGridnodes();

		if (currentKeyCount < potentialKeyCount) {
			System.out.println("send signal to change message to the user for keys");
		}
	}

	public void savePublicKeysToFile(String accountName, List<String> publicKeys) throws IOException {
		File keysFile = DataDirectory.getGridnodeKeysFile(accountName);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(keysFile, false))) {
			for (String key : publicKeys) {
				writer.write(key);
				writer.newLine();
			}
		}
	}

	public List<String> getKeys() {
		return keys;
	}

	private void updateAdditionalKeysPossible() {
		int currentKeyCount = keys.size();
		int potentialKeyCount = gridnodeModel.getPossibleGridnodes();
		boolean additionalKeysPossible = currentKeyCount < potentialKeyCount;

		// Always set the current state
		gridnodeKeyUpdateModel.setAdditionalKeysPossible(additionalKeysPossible);

		// Construct the message based on the current state
		if (additionalKeysPossible) {
			int additionalKeys = potentialKeyCount - currentKeyCount;
			String message = "You can generate " + additionalKeys + " new keys. Click generate.";
			gridnodeKeyUpdateModel.setMessage(message);
		} else {
			gridnodeKeyUpdateModel.setMessage("");
		}

		// Always fire the event
		gridnodeKeyUpdateModelEvent.fire(gridnodeKeyUpdateModel);
	}

}
