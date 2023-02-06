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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.unigrid.janus.model.ConditionalLock;
import org.unigrid.janus.model.Gridnode;
import org.unigrid.janus.model.GridnodeDatabase;
import org.unigrid.janus.model.GridnodeDeployment;
import org.unigrid.janus.model.GridnodeDeployment.Authentication;
import org.unigrid.janus.model.GridnodeDeployment.State;
import org.unigrid.janus.model.NodeStatus;
import org.unigrid.janus.model.ObservableCollection;
import org.unigrid.janus.model.Output;
import org.unigrid.janus.model.SSHConnection;
import org.unigrid.janus.model.SerializableOptional;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.rpc.entity.GetNewAddress;
import org.unigrid.janus.model.rpc.entity.GridnodeEntity;
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
		final int nodesToConfigure = gridnodeDeployment.getCount();

		for (int i = 0; i < nodesToConfigure; i++) {
			final GetNewAddress newAddress = rpc.call(GetNewAddress.getNewAddress(), GetNewAddress.class);
			final GridnodeEntity newGridnode = rpc.call(GridnodeEntity.genKey(), GridnodeEntity.class);
			final Gridnode gridnode = new Gridnode();

			gridnode.setPrivateKey(newGridnode.getResult());
			gridnodeDeployment.getGridnodes().put(gridnode, State.ONE_PREDEPLOYING);

			nodeUpdate.fire(NodeUpdate.builder()
				.pair(gridnode, State.ONE_PREDEPLOYING)
				.step(1)
				.stepName("New Deployment")
				.build()
			);

			gridnodeDeployment.setCount(gridnodeDeployment.getCount() - 1);
			amountsToSend.put(newAddress.getResult(), new BigDecimal(3000));
		}

		SendMany send = rpc.call(SendMany.sendMany(
			"",
			amountsToSend,
			1,
			"Multiple node setup"
		), SendMany.class);

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
				// Test
				List<Output> result = new ArrayList();
				Output outputss = new Output();
				
				Random random = new Random();
				int randomNumber = random.nextInt(100) + 1;
				outputss.setOutputidx(randomNumber);
				outputss.setTxhash("2bcd3c84c84f87eaa86e4e56834c92927a07f9e18718810b92e0d0324456a67c");
				result.add(outputss);
				outputs.setResult(result);
				// Test
				observedOutputs.clear();
				observedOutputs.addAll(outputs.getResult());

				for (Output output : gridnodeDatabase.getAvailableOutputs(outputs.getResult())) {
					System.out.println("Output: " + output);
					gridnode.setOutput(output);
				}
				if (gridnode.getTxHash()!= null) {
					gridnodeDatabase.setIndividualGridnodeState(gridnode, State.TWO_PENDING);
					nodeUpdate.fire(NodeUpdate.builder()
						.pair(gridnode, State.TWO_PENDING)
						.step(2)
						.stepName("Deployment Pending...")
						.build()
					);
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

	private void handleConnection(Gridnode gridnode) {
		System.out.println("---  handleConnection  ---");
		gridnodeDatabase.getParent(gridnode).ifPresent(deployment -> {
			deployment.getAuthentication().ifPresent(auth -> {
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
							deployment.getConnection().get().send(
								"cat ~/output.log && tail -f -n 0 ~/output.log", "exec",
								s -> {
									s = s.replaceAll("\u001B\\[.*?m", "");
									//NodesController.setUserData(gridnode, s);
									Pair<String, Pair<Integer, Double>> status = checkStepAndProgressFromOutput(
										gridnode, s);
									System.out.println("s: " + s);
									setGridnodeOutput(gridnode, s);
									String alias = checkStringIsAlias(s);
									if (!alias.isEmpty()) {
										gridnode.setAlias(alias);
									}

									nodeUpdate.fire(NodeUpdate.builder()
										.pair(gridnode, State.THREE_DEPLOYMENT)
										.stepName(status.getLeft())
										.step(status.getRight().getLeft())
										.progress(status.getRight().getRight())
										.build()
									);
								});
						}
					} catch(JSchException ex) {
						ex.printStackTrace();
					}
				}
			});
		});
		System.out.println();
	}

	private void handlePending(Gridnode gridnode) {
		System.out.println("---  handlePending  ---");
		gridnodeDatabase.getParent(gridnode).ifPresent(deployment -> {
			deployment.getAuthentication().ifPresent(auth -> {
				deployment.getConnection().ifPresent(connection -> {
					if (connection.isConnected()) {
						if (!gridnodeDatabase.isDeployingGridnode(deployment)) {
							gridnodeDatabase.setIndividualGridnodeState(gridnode, State.THREE_DEPLOYMENT);
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
								//AtomicInteger count = new AtomicInteger(0);
								deployment.getConnection().get().send(command, "exec", s -> {
									s = s.replaceAll("\u001B\\[.*?m", "");
									//NodesController.setUserData(gridnode, s);
									Pair<String, Pair<Integer,Double>> status = checkStepAndProgressFromOutput(gridnode, s);
									System.out.println("s: " + s);
									gridnode.responseAddLine(s + "\n");
									setGridnodeOutput(gridnode, s);
									String alias = checkStringIsAlias(s);
									if (!alias.isEmpty()) {
										gridnode.setAlias(alias);
									}

									nodeUpdate.fire(NodeUpdate.builder()
										.pair(gridnode, State.THREE_DEPLOYMENT)
										.stepName(status.getLeft())
										.step(status.getRight().getLeft())
										.progress(status.getRight().getRight())
										.build()
									);
								});
							} catch (JSchException ex) {
								ex.printStackTrace();
							}
						}
					}
				});
			});
		});

		System.out.println();
	}

	private void handleDeployment(Gridnode gridnode) {
		System.out.println("---  handleDeployment  ---");
		final GridnodeList confList = rpc.call(GridnodeList.listConf(), GridnodeList.class);
		observedNodes.clear();
		observedNodes.addAll(confList.getResult());

		//System.out.println("conf-list" + confList.getResult());
		if (Gridnode.isGridnodeConfAdded(confList.getResult(), gridnode)) {
			System.out.println("           :         All done           :  ");
			nodeUpdate.fire(NodeUpdate.builder()
				.pair(gridnode, GridnodeDeployment.State.THREE_DEPLOYMENT)
				.stepName("Node Is Deployed!")
				.progress(1)
				.step(2)
				.build()
			);
			gridnodeDatabase.setIndividualGridnodeState(gridnode, State.FOUR_DEPLOYED);
			/*Instant start = Instant.now();
			Instant now = Instant.now();
			while (Duration.between(start, now).toMillis() < 3000) {
				now = Instant.now();
			}*/
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

	/*private void handlePostDeployment() {
		final GridnodeList confList = rpc.call(GridnodeList.listConf(), GridnodeList.class);
		nodeUpdate.fire(NodeUpdate.builder()
			.confList(confList.getResult())
			.build()
		);
	}*/

	private Thread thread = new Thread(() -> {
		final ObservableCollection observeGridnodes = new ObservableCollection(
			gridnodeDatabase.getGridnodeDeployments(), observedNodes
		);

		while (run) {
			observeGridnodes.onChange(() -> {
				gridnodeDatabase.getNewlyDeployed().ifPresent(n -> {
					handleNewDeployment(n);
				});
				/* test */
				/*synchronized (gridnodeDatabase.getIndividualGridnodesWithState()) {
					System.out.println("---  All gridnodes: " + gridnodeDatabase.getIndividualGridnodesWithState());
				}*/
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
				//handlePostDeployment();
			}, () -> {
				//System.out.println("locking");
				conditionalLock.wait(UPDATE_RATE_MS);
				//System.out.println("unlocking");
			});

			
		}
	});

	private void setGridnodeOutput(Gridnode gridnode, String s) {
		if (checkStringIsGridnodeOutput(s)) {
			String[] split = s.split(" ");
			gridnode.setAlias(split[0]);
			gridnode.setAddress(split[1]);
			gridnode.setOutputIndex(Integer.parseInt(split[4]));
			/* The HashMap key Gridnode has changed, set the new State again */
			gridnodeDatabase.setIndividualGridnodeState(gridnode, State.THREE_DEPLOYMENT);
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

	private String checkStringIsAlias(String str) {
		String regexTarget = "NEW_SERVER_NAME: ";
		return Pattern.matches(regexTarget + "ugd_docker_[0-9]+", str)
			? str.replace(regexTarget, "")
			: "";
	}

	public Pair<String, Pair<Integer,Double>> checkStepAndProgressFromOutput(Gridnode gridnode, String output) {
		String stepOutputs[] = new String[]{"", "Unigrid sync status", "Loading the Unigrid backend"};
		String stepDescription[] = new String[]{"Copy Volume", "Sync Unigrid", "Loading Backend"};
		String description = "";
		int step = 0;
		double progress = 0;

		String normalSpacedString = StringUtils.normalizeSpace(output);
		String stringRegex = "[0-9]+\\.[0-9]+\\w+ ([0-9]+)% [0-9]+\\.[0-9]+\\w+\\/\\w+ [0-9]+:[0-9]+:[0-9]+";

		Pattern patternVolume = Pattern.compile(stringRegex);
		Matcher matcher = patternVolume.matcher(normalSpacedString);

		if (matcher.find()) {
			description = stepDescription[0];
			step = 1;
			try {
				if (!matcher.group(1).equals("")) {
					progress = Integer.parseInt(matcher.group(1)) / 100.0;
					return Pair.of(description, Pair.of(step, progress));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for (int i = 1; i < stepOutputs.length; i++) {
			if (output.contains(stepOutputs[i])) {
				description = stepDescription[i];
				step = i;
				if (i == 1) {
					String[] split = output.split("\\.");
					String clean = split[split.length - 2].replaceAll("\\D+", "");

					try {
						if (!clean.equals("")) {
							progress = Integer.parseInt(clean) / 100.0;
							return Pair.of(description, Pair.of(step, progress));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (i == 2) {
					String clean = output.replaceAll("\\D+", "");

					try {
						if (!clean.equals("")) {
							progress = ((Integer.parseInt(clean) + 1) * 3.34) / 100;
							return Pair.of(description, Pair.of(step, progress));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		return Pair.of("", Pair.of(0, 0.0));
	}

	public void deploy(Authentication authentication, int count) {
		System.out.println("---  deploy  ---");
		final GridnodeDeployment newDeployment = GridnodeDeployment.builder()
			.authentication(SerializableOptional.of(authentication))
			.count(count)
			.build();

		gridnodeDatabase.getGridnodeDeployment(newDeployment).ifPresentOrElse(oldDeployment -> {
			oldDeployment.setCount(oldDeployment.getCount() + count);
		}, () -> {
			gridnodeDatabase.add(newDeployment);
			for (GridnodeDeployment g : gridnodeDatabase.getGridnodeDeployments()) {
			}
		});

		conditionalLock.fire();
	}

	public void start() {
		thread.start();
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				System.out.println("  -- start timer task");
				final GridnodeList confList = rpc.call(GridnodeList.listConf(), GridnodeList.class);
				nodeUpdate.fire(NodeUpdate.builder()
					.confList(confList.getResult())
					.build()
				);
			}
		};
		timer.schedule(task, 0, 10 * 1000);
	}

	public void stop() {
		run = false;
	}

	@PreDestroy
	private void destroy() {
		stop();
	}
}
