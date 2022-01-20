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

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import org.unigrid.janus.model.rpc.entity.Info;

public class Wallet {
	public static final String BALANCE_PROPERTY = "balance";
	public static final String TOTALBALANCE_PROPERTY = "totalbalance";
	public static final String MONEYSUPPLY_PROPERTY = "moneysupply";
	public static final String BLOCKS_PROPERTY = "blocks";
	public static final String CONNECTIONS_PROPERTY = "connections";
	public static final String LOCKED_PROPERTY = "locked";
	private static  PropertyChangeSupport pcs;

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

	// balance property
	private static double balance;

	public double getBalance() {
		return this.balance;
	}

	public void setBalance(double newValue) {
		double oldValue = this.balance;
		this.balance = newValue;
		this.pcs.firePropertyChange(this.BALANCE_PROPERTY, oldValue, newValue);
	}

	// totalbalance property
	private static double totalbalance;

	public double getTotalBalance() {
		return this.totalbalance;
	}

	public void setTotalBalance(double newValue) {
		double oldValue = this.totalbalance;
		this.totalbalance = newValue;
		this.pcs.firePropertyChange(this.TOTALBALANCE_PROPERTY, oldValue, newValue);
	}

	// moneysupply property
	private static double moneysupply;

	public double getMoneysupply() {
		return this.moneysupply;
	}

	public void setMoneysupply(double newValue) {
		double oldValue = this.moneysupply;
		this.moneysupply = newValue;
		this.pcs.firePropertyChange(this.MONEYSUPPLY_PROPERTY, oldValue, newValue);
	}

	// blacklisted property
	private static double blacklisted;

	public double getBlacklisted() {
		return this.blacklisted;
	}

	public void setBlacklisted(double newValue) {
		double oldValue = this.blacklisted;
		this.blacklisted = newValue;
		this.pcs.firePropertyChange("blacklisted", oldValue, newValue);
	}

	// blocks property
	private static int blocks;

	public int getBlocks() {
		return this.blocks;
	}

	public void setBlocks(int newValue) {
		int oldValue = this.blocks;
		this.blocks = newValue;
		this.pcs.firePropertyChange(this.BLOCKS_PROPERTY, oldValue, newValue);
	}

	// connections property
	private static int connections;

	public int getConnections() {
		return this.connections;
	}

	public void setConnections(int newValue) {
		int oldValue = this.connections;
		this.connections = newValue;
		this.pcs.firePropertyChange(this.CONNECTIONS_PROPERTY, oldValue, newValue);
	}

	// version property
	private static int version;

	public double getVersion() {
		return this.version;
	}

	public void setVersion(int newValue) {
		int oldValue = this.version;
		this.version = newValue;
		this.pcs.firePropertyChange("version", oldValue, newValue);
	}

	// walletVersion property
	private static int walletVersion;

	public double getWalletVersion() {
		return this.walletVersion;
	}

	public void setWalletVersion(int newValue) {
		int oldValue = this.walletVersion;
		this.walletVersion = newValue;
		this.pcs.firePropertyChange("walletVersion", oldValue, newValue);
	}

	// locked property
	private static boolean locked;

	public boolean getLocked() {
		return this.locked;
	}

	public void setLocked(boolean newValue) {
		boolean oldValue = this.locked;
		this.locked = newValue;
		this.pcs.firePropertyChange(this.LOCKED_PROPERTY, oldValue, newValue);
	}

	public void setInfo(Info newInfo) {
		this.setBalance(newInfo.getResult().getBalance());
		this.setTotalBalance(newInfo.getResult().getTotalbalance());
		this.setMoneysupply(newInfo.getResult().getMoneysupply());
		this.setBlocks(newInfo.getResult().getBlocks());
		this.setConnections(newInfo.getResult().getConnections());
	}

}
