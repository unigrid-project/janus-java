/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.rpc.entity.GetWalletInfo;
import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.rpc.entity.StakingStatus;
import org.unigrid.janus.model.service.DebugService;

@Eager
@ApplicationScoped
public class Wallet {
	public static final String BALANCE_PROPERTY = "balance";
	public static final String TOTALBALANCE_PROPERTY = "totalbalance";
	public static final String MONEYSUPPLY_PROPERTY = "moneysupply";
	public static final String BLOCKS_PROPERTY = "blocks";
	public static final String CONNECTIONS_PROPERTY = "connections";
	public static final String LOCKED_PROPERTY = "locked";
	public static final String LOCKED_STATE_PROPERTY = "null";
	public static final String SYNC_STATE = "synced";
	public static final String STAKING_PROPERTY = "staking";
	public static final String PROCESSING_PROPERTY = "processing";
	public static final String ENCRYPTED_STATUS = "encrypted";
	public static final String IS_OFFLINE = "offline";
	public static final String STATUS_PROPERTY = "walletstatus";
	public static final String TRANSACTION_COUNT = "transactioncount";
	private static BigDecimal balance = new BigDecimal(0);
	private static double totalbalance;
	private static double moneysupply;
	private static double blacklisted;
	private static int blocks;
	private Client client;
	private static int connections;
	private static int version;
	private static int walletVersion;
	private static long transactionCount;
	private static Boolean locked = true;
	private static Boolean isStaking;
	private static Boolean processingStatus = false;
	private static String status;

	@Getter @Setter
	private static int unlockState = 0;
	@Getter @Setter
	private Boolean checkExplorer = true;

	@Getter
	private static long stakingStartTime = 100000000L;
	private static Boolean encrypted;
	private static Boolean offline = false;

	@Getter @Setter
	private Object[] sendArgs;

	@Inject
	private static DebugService debug = new DebugService();
	private static PropertyChangeSupport pcs;

	@Getter
	private LockState lockState = LockState.LOCKED;

	@AllArgsConstructor
	public enum LockState {
		UNLOCKED("unlocked"),
		LOCKED("locked"),
		UNLOCKED_FOR_STAKING("unlocked-for-staking");

		@Getter
		private String currentState;

		public static LockState from(String s) {
			return switch (s) {
				case "locked" -> LockState.LOCKED;
				case "unlocked" -> LockState.UNLOCKED;
				default -> LockState.UNLOCKED_FOR_STAKING;
			};
		}
	}

	@Getter
	private SyncStatus syncStatus = SyncStatus.SYNCED;

	@AllArgsConstructor
	public enum SyncStatus {
		SYNCED("synced"),
		SYNCING("syncing");

		@Getter
		private String currentSyncStatus;

		public static SyncStatus from(String s) {
			return switch (s) {
				case "synced" -> SyncStatus.SYNCED;
				case "syncing" -> SyncStatus.SYNCING;
				default -> SyncStatus.SYNCED;
			};
		}
	}

	public Wallet() {
		if (this.pcs != null) {
			return;
		}
		this.pcs = new PropertyChangeSupport(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}

	public BigDecimal getBalance() {
		return this.balance;
	}

	public void setBalance(BigDecimal newValue) {
		BigDecimal oldValue = this.balance;
		this.balance = newValue;
		this.pcs.firePropertyChange(this.BALANCE_PROPERTY, oldValue, newValue);
	}

	public long getTransactionCount() {
		return this.transactionCount;
	}

	public void setTransactionCount(long newValue) {
		long oldValue = this.transactionCount;
		this.transactionCount = newValue;
		this.pcs.firePropertyChange(this.TRANSACTION_COUNT, oldValue, newValue);
	}

	public double getTotalBalance() {
		return this.totalbalance;
	}

	public void setTotalBalance(double newValue) {
		double oldValue = this.totalbalance;
		this.totalbalance = newValue;
		this.pcs.firePropertyChange(this.TOTALBALANCE_PROPERTY, oldValue, newValue);
	}

	public double getMoneysupply() {
		return this.moneysupply;
	}

	public void setMoneysupply(double newValue) {
		double oldValue = this.moneysupply;
		this.moneysupply = newValue;
		this.pcs.firePropertyChange(this.MONEYSUPPLY_PROPERTY, oldValue, newValue);
	}

	public double getBlacklisted() {
		return this.blacklisted;
	}

	public void setBlacklisted(double newValue) {
		double oldValue = this.blacklisted;
		this.blacklisted = newValue;
		this.pcs.firePropertyChange("blacklisted", oldValue, newValue);
	}

	public int getBlocks() {
		return this.blocks;
	}

	public void setBlocks(int newValue) {
		int oldValue = this.blocks;
		this.blocks = newValue;
		System.out.println("setting blocks: " + this.blocks);
		this.pcs.firePropertyChange(this.BLOCKS_PROPERTY, oldValue, newValue);
	}

	public int getConnections() {
		return this.connections;
	}

	public void setConnections(int newValue) {
		int oldValue = this.connections;
		this.connections = newValue;
		this.pcs.firePropertyChange(this.CONNECTIONS_PROPERTY, oldValue, newValue);
	}

	public double getVersion() {
		return this.version;
	}

	public void setVersion(int newValue) {
		int oldValue = this.version;
		this.version = newValue;
		this.pcs.firePropertyChange("version", oldValue, newValue);
	}

	public double getWalletVersion() {
		return this.walletVersion;
	}

	public void setWalletVersion(int newValue) {
		int oldValue = this.walletVersion;
		this.walletVersion = newValue;
		this.pcs.firePropertyChange("walletVersion", oldValue, newValue);
	}

	public Boolean getLocked() {
		return this.locked;
	}

	public void setLocked(Boolean newValue) {
		Boolean oldValue = this.locked;
		this.locked = newValue;
		this.pcs.firePropertyChange(this.LOCKED_PROPERTY, oldValue, newValue);
	}

	public void setEncryptedState(GetWalletInfo walletInfo) {
		long timestamp = walletInfo.getResult().getUnlockUntil();
		// only 4999 == an unencrypted wallet
		this.setEncrypted(timestamp != 4999);
	}

	public void setEncrypted(Boolean newValue) {
		Boolean oldValue = this.getEncrypted();
		this.encrypted = newValue;
		this.pcs.firePropertyChange(this.ENCRYPTED_STATUS, oldValue, newValue);
	}

	public Boolean getEncrypted() {
		return this.encrypted;
	}

	public void setOffline(Boolean newValue) {
		Boolean oldValue = this.getOffline();
		this.offline = newValue;
		if (newValue) {
			this.pcs.firePropertyChange(this.IS_OFFLINE, oldValue, newValue);
		} else {
			System.out.println("WALLET ONLINE!");
		}
	}

	public Boolean getOffline() {
		return this.offline;
	}

	public Boolean getStakingStatus() {
		return this.isStaking;
	}

	public void setIsStaking(Boolean newValue) {
		Boolean oldValue = this.isStaking;
		this.isStaking = newValue;
		this.pcs.firePropertyChange(this.STAKING_PROPERTY, oldValue, newValue);
	}

	public void setStakingStatus(StakingStatus staking) {
		this.setIsStaking(staking.getResult().getStakingStatus());
		//String unlocked = String.format("Locked Status: %s", this.getLocked());
		//debug.log(unlocked);
	}

	public String getStatus() {
		return this.status;
	}

	public boolean isLoading() {
		boolean result = true;
		if (this.status != null) {
			if (this.status.equals("Done loading")) {
				result = false;
			}
		}
		return result;
	}

	public void setStatus(String newValue) {
		String oldValue = this.status;
		this.status = newValue;
		this.pcs.firePropertyChange(this.STATUS_PROPERTY, oldValue, newValue);
	}

	public void setInfo(Info newInfo) {
		this.setBalance(newInfo.getResult().getBalance());
		this.setTotalBalance(newInfo.getResult().getTotalbalance());
		this.setMoneysupply(newInfo.getResult().getMoneysupply());
		this.setBlocks(newInfo.getResult().getBlocks());
		this.setConnections(newInfo.getResult().getConnections());
		//disable processing indicator
		this.setProcessingStatus();
		this.setStatus(newInfo.getResult().getBootstrapping().getWalletstatus());
		//String unlock = String.format("Unlock Until: %s", newInfo.getResult().getUnlockUntil());
		//debug.log(unlock);

	}

	public void setWalletState(LockState state) {
		LockState oldValue = this.lockState;
		lockState = state;
		setLocked(state == LockState.LOCKED || state == LockState.UNLOCKED_FOR_STAKING);
		this.pcs.firePropertyChange(this.LOCKED_STATE_PROPERTY, oldValue, state);
	}

	public void setSyncStatus(SyncStatus state) {
		SyncStatus oldValue = this.syncStatus;
		this.syncStatus = state;
		this.pcs.firePropertyChange(this.SYNC_STATE, oldValue, state);
	}

	public Boolean getProcessingStatus() {
		return this.processingStatus;
	}

	public void setProcessingStatus() {
		Boolean oldValue = this.processingStatus;
		this.processingStatus ^= true;
		this.pcs.firePropertyChange(this.PROCESSING_PROPERTY, oldValue, this.getProcessingStatus());
	}

	public int getExplorerHeight() {
		try {
			client = ClientBuilder.newBuilder()
				.build();
			Response response = client.target("https://explorer.unigrid.org/api/getblockcount")
				.request(MediaType.TEXT_XML).get();
			//System.out.println(response.readEntity(String.class));
			String block = response.readEntity(String.class);
			if (block.equalsIgnoreCase("This method is restricted.")) {
				return 0;
			} else {
				return Integer.parseInt(block);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(e.getCause().toString());
			return 0;
		}
	}

}
