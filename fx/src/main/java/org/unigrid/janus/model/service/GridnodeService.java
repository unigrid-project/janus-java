/*
    The Janus Wallet
    Copyright © 2021-2023 The Unigrid Foundation, UGD Software AB

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model.service;

import com.jcraft.jsch.JSchException;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import static java.lang.Thread.sleep;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.unigrid.janus.model.ConditionalLock;
import org.unigrid.janus.model.Gridnode;
import org.unigrid.janus.model.GridnodeDatabase;
import org.unigrid.janus.model.GridnodeDeployment;
import org.unigrid.janus.model.GridnodeDeployment.Authentication;
import org.unigrid.janus.model.GridnodeDeployment.State;
import org.unigrid.janus.model.ObservableCollection;
import org.unigrid.janus.model.ObservableCollectionMember;
import org.unigrid.janus.model.Output;
import org.unigrid.janus.model.SSHConnection;
import org.unigrid.janus.model.SerializableOptional;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.rpc.entity.GetNewAddress;
import org.unigrid.janus.model.rpc.entity.GridnodeEntity;
import org.unigrid.janus.model.rpc.entity.GridnodeEntity.Request;
import org.unigrid.janus.model.rpc.entity.GridnodeList;
import org.unigrid.janus.model.rpc.entity.OutputList;
import org.unigrid.janus.model.rpc.entity.SendMany;
import org.unigrid.janus.model.signal.NodeUpdate;

@Slf4j
@Eager @ApplicationScoped
public class GridnodeService {
	@Inject private RPCService rpc;
	@Inject @Getter private GridnodeDatabase gridnodeDatabase;
	@Inject private Event<NodeUpdate> nodeUpdate;

	private boolean run = true;
	private static final int UPDATE_RATE_MS = 10000;
	private static final int UPDATE_RATE_PREDEPLOYMENT_MS = 5000;

	private List<Gridnode> observedNodes = new ArrayList<>();
	private List<Output> observedOutputs = new ArrayList<>();
	private ConditionalLock conditionalLock = new ConditionalLock();

	private void handleNewDeployment(GridnodeDeployment gridnodeDeployment) {
		System.out.println("---  handleNewDeployment  ---");
		final Map<String, BigDecimal> amountsToSend = new HashMap();

		for (int i = 0; i < gridnodeDeployment.getCount().get(); i++) {
			final GetNewAddress newAddress = rpc.call(GetNewAddress.getNewAddress(), GetNewAddress.class);
			final GridnodeEntity newGridnode = rpc.call(GridnodeEntity.genKey(), GridnodeEntity.class);
			final Gridnode gridnode = new Gridnode();

			gridnode.setPrivateKey(newGridnode.getResult());
			System.out.println("gridnode: " + gridnode);

			gridnodeDeployment.getGridnodes().put(gridnode, State.ONE_PREDEPLOYING);

			/*NodeUpdate nu = NodeUpdate.builder()
				.pair(gridnode, State.ONE_PREDEPLOYING)
				.build();*/

			nodeUpdate.fire(NodeUpdate.builder()
				.pair(gridnode, State.ONE_PREDEPLOYING)
				.build()
			);

			amountsToSend.put(newAddress.getResult(), new BigDecimal(3000));
		}

		SendMany send = rpc.call(SendMany.sendMany(
			"",
			amountsToSend,
			1,
			"Multiple node setup"
		), SendMany.class);

		gridnodeDeployment.setCount(SerializableOptional.empty());
		System.out.println();
	}

	private void handlePreDeployment(Gridnode gridnode) {
		try {
			sleep(5000);
		} catch (InterruptedException ex) {
			Logger.getLogger(GridnodeService.class.getName()).log(Level.SEVERE, null, ex);
		}
		final Thread thread = new Thread(() -> {
			boolean run = true;

			while (run) {
				System.out.println("---  handlePreDeployment  ---");
				final OutputList outputs = rpc.call(OutputList.outputs(), OutputList.class);

				observedOutputs.clear();
				observedOutputs.addAll(outputs.getResult());

				System.out.println("output: " + outputs.getResult());
				for (Output output : gridnodeDatabase.getAvailableOutputs(outputs.getResult())) {
					System.out.println("Output: " + output);
					gridnode.setOutput(output);
					System.out.println("gridnode txhash: " + gridnode.getTxHash());
				}
				if (gridnode.getTxHash()!= null) {
					gridnodeDatabase.setIndividualGridnodeState(gridnode, State.TWO_PENDING);
					run = false;
				}
	
				try {
					Thread.sleep(UPDATE_RATE_PREDEPLOYMENT_MS);
				} catch (InterruptedException ex) {
					/* We don't care */
				}
			}
		});
		System.out.println();

		thread.start();
	}

	private void handlePending(Gridnode gridnode) {
		System.out.println("---  handlePending  ---");
		System.out.println(" gridnode: " + gridnode);
		gridnodeDatabase.getParent(gridnode).ifPresent(deployment -> {
			deployment.getAuthentication().ifPresent(auth -> {
			if (deployment.getConnection().get().isConnected() && !gridnodeDatabase.isDeployingGridnode(
				deployment)) {
				try {
					System.out.println("Not Deloying Gridnodes");
					final String wget = "wget -O- raw.githubusercontent.com/TimNhanTa/installer/master/node_installer.sh";
					//String wget = "wget -O- raw.githubusercontent.com/unigrid-project/unigrid-installer/main/node_installer.sh";
					//System.out.println("USER_PASSWORD: " + auth.getPassword());
					final String command = /*"USER_PASSWORD=\"" + auth.getPassword() + "\" "
								+ */wget + " | bash -s --"
						+ " -t \"" + gridnode.getTxHash()+ " " + gridnode.getOutputIndex()+ "\""
						+ " -k \"" + gridnode.getPrivateKey() + "\" -p \"" + auth.getPassword() + "\" |& tee ~/output.log";
					System.out.println("command: command: " + command);

					deployment.getConnection().get().send(command, "exec", s -> {
						System.out.println("s: " + s);
						setGridnodeOutput(gridnode, s);
					});
					gridnodeDatabase.setIndividualGridnodeState(gridnode, State.THREE_DEPLOYMENT);
					System.out.println();

					/*nodeUpdate.fire(NodeUpdate.builder()
								.pair(gridnode,
									GridnodeDeployment.State.THREE_DEPLOYMENT)
								.build()
							);*/
				} catch (JSchException ex) {
					ex.printStackTrace();
				}
			}});
		});

		System.out.println();
	}

	private void handleDeployment(Gridnode gridnode) {
		System.out.println("---  handleDeployment  ---");
		System.out.println(" gridnode: " + gridnode);
		final GridnodeList confList = rpc.call(GridnodeList.listConf(), GridnodeList.class);
		observedNodes.clear();
		observedNodes.addAll(confList.getResult());
		System.out.println("conf-list" + confList.getResult());
		if (Gridnode.isGridnodeConfAdded(confList.getResult(), gridnode)) {
			System.out.println("           :         All done           :  ");
			gridnodeDatabase.setIndividualGridnodeState(gridnode, State.FOUR_DEPLOYED);
			nodeUpdate.fire(NodeUpdate.builder()
				.pair(gridnode, GridnodeDeployment.State.FOUR_DEPLOYED)
				.build()
			);
		} else {
			if (gridnode.isGridnodeDeployed()) {
				String[] addressSplit = gridnode.getAddress().split(":");
				InetSocketAddress address = new InetSocketAddress(
					addressSplit[0],
					Integer.valueOf(addressSplit[1])
				);
				final GridnodeEntity gridnodeAddConf = rpc.call(GridnodeEntity.addConf(gridnode.getAlias(),
					address, gridnode.getPrivateKey(),
					gridnode.getTxHash(), gridnode.getOutputIndex()), GridnodeEntity.class);
				observedNodes.add(gridnode);
			}
		}

		System.out.println();
	}

	private void handleConnection(Gridnode gridnode) {
		System.out.println("---  handleConnection  ---");
		System.out.println("gridnode: " + gridnode);
		gridnodeDatabase.getParent(gridnode).ifPresent(deployment -> {
			deployment.getAuthentication().ifPresent(auth -> {
				System.out.println("connection: " + deployment.getConnection());
				if (deployment.getConnection() == null){
					deployment.setConnection(Optional.empty());
				}
				if (deployment.getConnection().isEmpty() || !deployment.getConnection().get().isConnected()) {
					try {
						deployment.getConnection().ifPresent(connection -> connection.close());
						deployment.setConnection(Optional.of(SSHConnection.connect(
							auth.getAddress(), auth.getUsername(), auth.getPassword()
						)));
						
						/* if connection disconnected, tail output */
						if (deployment.getGridnodes().get(gridnode) == GridnodeDeployment.State.THREE_DEPLOYMENT) {
							System.out.println("---handleConnection lost connection---");
							deployment.getConnection().get().send(
								"cat ~/output.log && tail -f -n 0 ~/output.log", "exec",
								s -> {
									System.out.println("s: " + s);
									setGridnodeOutput(gridnode, s);
								});
						}
						/*nodeUpdate.fire(NodeUpdate.builder()
							.pair(gridnode,
								GridnodeDeployment.State.THREE_DEPLOYMENT)
							.build()
						);*/
					} catch(JSchException ex) {
						ex.printStackTrace();
					}
				}
			});
		});
		System.out.println();
	}

	private void setGridnodeOutput(Gridnode gridnode, String s) {
		if (checkStringIsGridnodeOutput(s)) {
			System.out.println(":::: found match : setGridnodeOutput:::: ");
			//System.out.println("output: " + s);
			//System.out.println("gridnode: " + gridnode);
			//System.out.println("hashcode before: " + gridnodeDatabase.getGridnodeDeployments().stream().map(c -> c.observableHashCode()).reduce(0, Integer::sum));
			String[] split = s.split(" ");
			/*for (String str : split) {
				System.out.println("split str: " + str);
			}*/
			gridnode.setAlias(split[0]);
			gridnode.setAddress(split[1]);
			//gridnode.setPrivateKey(split[2]);
			//gridnode.setTxhash(split[3]);
			gridnode.setOutputIndex(Integer.parseInt(split[4]));
			//System.out.println("hashcode after: " + gridnodeDatabase.getGridnodeDeployments().stream().map(c -> c.observableHashCode()).reduce(0, Integer::sum));
			//System.out.println("gridnode: " + gridnode);
			/* The HashMap key Gridnode has changed, set the new State again */
			gridnodeDatabase.setIndividualGridnodeState(gridnode, State.THREE_DEPLOYMENT);
			//System.out.println(":: End :: ");
		}
	}

	private boolean checkStringIsGridnodeOutput(String str) {
		String[] split = str.split(" ");
		String pattern[] = new String[]{"ugd_docker_[0-9]+", "[0-9]+(\\.[0-9]+){3}:[0-9]+", "[A-Za-z0-9]{51}",
			"[A-Za-z0-9]{64}", "[0-9]+"};
		boolean exactListSize = split.length == 5;

		if (!exactListSize) {
			return false;
		}

		boolean match = exactListSize;

		for (int i = 0; i < split.length; i++) {
			match &= Pattern.matches(pattern[i], split[i]);
		}

		return match;
	}

	private Thread thread = new Thread(() -> {
		final ObservableCollection observeGridnodes = new ObservableCollection(
			gridnodeDatabase.getGridnodeDeployments(), observedNodes
		);

		while (run) {
			observeGridnodes.onChange(() -> {
				System.out.println("---  onchange is triggered!!!");
				gridnodeDatabase.getNewlyDeployed().ifPresent(n -> {
					handleNewDeployment(n);
				});
				/* test */
				synchronized (gridnodeDatabase.getIndividualGridnodesWithState()) {
					System.out.println("---  All gridnodes: " + gridnodeDatabase.getIndividualGridnodesWithState());
				}
				gridnodeDatabase.getIndividualGridnodesWithState().stream()
					.filter(n -> n.getRight() == State.ONE_PREDEPLOYING).forEach(n -> {
					handlePreDeployment(n.getLeft());
				});

				gridnodeDatabase.getIndividualGridnodesWithState().stream()
					.filter(n -> n.getRight() == State.TWO_PENDING
					|| n.getRight() == State.THREE_DEPLOYMENT).forEach(n -> {
					handleConnection(n.getLeft());
				});
 
				gridnodeDatabase.getIndividualGridnodesWithState().stream()
					.filter(n -> n.getRight() == State.TWO_PENDING).forEach(n -> {
					handlePending(n.getLeft());
				});

				gridnodeDatabase.getIndividualGridnodesWithState().stream()
					.filter(n -> n.getRight() == State.THREE_DEPLOYMENT).forEach(n -> {
					handleDeployment(n.getLeft());
				});
			}, () -> {
				System.out.println("locking");
				conditionalLock.wait(UPDATE_RATE_MS);
				System.out.println("unlocking");
			});

			
		}
	});

	public void deploy(Authentication authentication, int count) {
		System.out.println("---  deploy  ---");
		final GridnodeDeployment deployment = GridnodeDeployment.builder()
			.authentication(SerializableOptional.of(authentication))
			.count(SerializableOptional.of(count)
			).build();

		gridnodeDatabase.add(deployment);
		conditionalLock.fire();
	}

	public void start() {
		thread.start();
	}

	public void stop() {
		run = false;
	}

	@PreDestroy
	private void destroy() {
		stop();
	}
}
