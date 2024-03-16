

package org.unigrid.janus.model.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bouncycastle.util.encoders.Hex;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.CryptoUtils;
import org.unigrid.janus.model.gridnode.ActivateGridnode;
import org.unigrid.janus.model.gridnode.GridnodeData;
import org.unigrid.janus.model.gridnode.GridnodeListViewItem;
import org.unigrid.janus.model.setup.AppConfig;
import org.unigrid.janus.model.ssl.InsecureTrustManager;
import org.unigrid.janus.model.signal.GridnodeEvents;

@ApplicationScoped
public class GridnodeHandler {

	@Inject
	private AppConfig appConfig;
	@Inject
	private AccountsData accountData;
	@Inject
	private CryptoUtils cryptoUtils;
	@Inject
	private Event<GridnodeEvents> gridnodeEvents;

	public List<GridnodeData> fetchGridnodes() {
		Client client = null;
		try {
			client = createClient();
			String gridnodeUri = appConfig.getGridnodeUri();
			Response response = client.target(URI.create(gridnodeUri))
				.request(MediaType.APPLICATION_JSON).get();

			if (response.getStatus() == Response.Status.OK.getStatusCode()) {
				String jsonResponse = response.readEntity(String.class);
				return parseGridnodesJson(jsonResponse);
			} else {
				System.err.println("Failed to fetch gridnodes. Status: " + response.getStatus());
				return Collections.emptyList();
			}
		} catch (Exception e) {
			System.err.println("Error fetching gridnodes: " + e.getMessage());
			return Collections.emptyList();
		} finally {
			if (client != null) {
				client.close();
			}
		}
	}

	private jakarta.ws.rs.client.Client createClient() throws Exception {
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, InsecureTrustManager.create(), new SecureRandom());
		return ClientBuilder.newBuilder().sslContext(sc)
			.hostnameVerifier((hostname, session) -> true).build();
	}

	public List<GridnodeData> parseGridnodesJson(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, new TypeReference<List<GridnodeData>>() {
		});
	}

	public List<GridnodeListViewItem> compareGridnodesWithLocalKeys(List<GridnodeData> gridnodes, List<String> localKeys) {
		List<GridnodeListViewItem> listViewItems = new ArrayList<>();
		for (GridnodeData gridnode : gridnodes) {
			if (localKeys.contains(gridnode.getId())) {
				listViewItems.add(new GridnodeListViewItem(gridnode.getStatus(), gridnode.getId(), gridnode.getHostName()));
			}
		}
		return listViewItems;
	}

	public void startGridnode(String gridnodeId, String password) throws Exception {
		AccountsData.Account selectedAccount = accountData.getSelectedAccount();
		String encryptedPrivateKey = selectedAccount.getEncryptedPrivateKey();

		// Decrypt the private key
		byte[] privateKeyBytes = cryptoUtils.decrypt(encryptedPrivateKey, password);

		// Create an ECKey instance from private key bytes
		ECKey privateKey = ECKey.fromPrivate(privateKeyBytes);
		String pubKey = Hex.toHexString(privateKey.getPubKey());

		// Sign the gridnode ID
		ECKey.ECDSASignature signature = privateKey.sign(Sha256Hash.of(gridnodeId.getBytes()));
		String encodedSignature = Base64.getEncoder().encodeToString(signature.encodeToDER());

		// Prepare the ActivateGridnode object
		ActivateGridnode gridnode = new ActivateGridnode();
		gridnode.setGridnodeId(gridnodeId);
		gridnode.setPublicKey(pubKey);

		// Create a REST client and send the PUT request
		Client client = null;
		try {
			client = createClient(); // Make sure this handles SSL configuration
			String gridnodeStartUri = appConfig.getGridnodeStartUri();
			Response response = client.target(URI.create(gridnodeStartUri))
				.request(MediaType.APPLICATION_JSON)
				.header("sign", encodedSignature)
				.put(Entity.entity(gridnode, MediaType.APPLICATION_JSON));

			// Handle the response
			if (response.getStatus() == Response.Status.ACCEPTED.getStatusCode()) {
				System.out.println("Gridnode started successfully.");
				// fire an event to resfresh the list
				gridnodeEvents.fire(GridnodeEvents.builder()
					.eventType(GridnodeEvents.EventType.GRIDNODE_STARTED)
					.build());
			} else {
				System.err.println("Failed to start gridnode. Status: " + response.getStatus());
			}
		} catch (Exception e) {
			System.err.println("Error starting gridnode: " + e.getMessage());
			throw e; // Rethrow the exception to handle it at a higher level
		} finally {
			if (client != null) {
				client.close();
			}
		}
	}

}
