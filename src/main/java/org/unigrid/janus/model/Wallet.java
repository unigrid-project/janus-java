/*
	The Janus Wallet
	Copyright © 2021 The Unigrid Foundation

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

import jakarta.inject.Inject;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.rpc.entity.StakingStatus;
import org.unigrid.janus.model.service.DebugService;

public class Wallet {

	public static final String BALANCE_PROPERTY = "balance";
	public static final String TOTALBALANCE_PROPERTY = "totalbalance";
	public static final String MONEYSUPPLY_PROPERTY = "moneysupply";
	public static final String BLOCKS_PROPERTY = "blocks";
	public static final String CONNECTIONS_PROPERTY = "connections";
	public static final String LOCKED_PROPERTY = "locked";
	public static final String STAKING_PROPERTY = "staking";
	public static final String PROCESSING_PROPERTY = "processing";
	public static final String ENCRYPTED_STATUS = "encrypted";
	private static double balance;
	private static double totalbalance;
	private static double moneysupply;
	private static double blacklisted;
	private static int blocks;
	private static int connections;
	private static int version;
	private static int walletVersion;
	private static Boolean locked = true;
	private static Boolean isStaking;
	private static Boolean processingStatus = false;
	private static String status;
	@Getter @Setter
	private static int unlockState = 0;
	@Getter
	private static long stakingStartTime = 45126460800000L;
	private static Boolean encrypted;
	@Getter @Setter
	private Object[] sendArgs;
	@Inject
	private static DebugService debug = new DebugService();
	public static final String STATUS_PROPERTY = "walletstatus";
	private static PropertyChangeSupport pcs;

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

	public double getBalance() {
		return this.balance;
	}

	public void setBalance(double newValue) {
		double oldValue = this.balance;
		this.balance = newValue;
		this.pcs.firePropertyChange(this.BALANCE_PROPERTY, oldValue, newValue);
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

	public void setEncrypted(Boolean newValue) {
		Boolean oldValue = this.getEncrypted();
		this.encrypted = newValue;
		this.pcs.firePropertyChange(this.ENCRYPTED_STATUS, oldValue, newValue);
	}

	public Boolean getEncrypted() {
		return this.encrypted;
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
		long timestamp = newInfo.getResult().getUnlockUntil();
		// only 4999 == an unencrypted wallet
		this.setEncrypted(timestamp != 4999);
		//debug.log(String.format("ENCRYPTED: %s", getEncrypted()));
		this.setStatus(newInfo.getResult().getBootstrapping().getWalletstatus());
		long time = timestamp;
		Date date = new Date(time);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.get(Calendar.YEAR);
		int yearFromDaemon = calendar.get(Calendar.YEAR);
		//debug.log(String.format("Year from daemon: %s", (int) calendar.get(Calendar.YEAR)));
		Date date2 = new Date(stakingStartTime);
		calendar.setTime(date2);
		calendar.get(Calendar.YEAR);
		int yeahFromGui = calendar.get(Calendar.YEAR);
		//debug.log(String.format("Year from GUI: %s", (int) calendar.get(Calendar.YEAR)));

		if (yearFromDaemon >= yeahFromGui) {
			this.setLocked(true);
			//debug.log(String.format("Wallet Unlocked for staking only"));
		} else if (timestamp > 0) {
			this.setLocked(false);
		} else if (timestamp == 0) {
			this.setLocked(true);
		}
		//String unlock = String.format("Unlock Until: %s", newInfo.getResult().getUnlockUntil());
		//debug.log(unlock);

	}

	public Boolean getProcessingStatus() {
		return this.processingStatus;
	}

	public void setProcessingStatus() {
		Boolean oldValue = this.processingStatus;
		this.processingStatus ^= true;
		this.pcs.firePropertyChange(this.PROCESSING_PROPERTY, oldValue, this.getProcessingStatus());
	}

}
