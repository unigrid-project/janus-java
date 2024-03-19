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
package org.unigrid.janus.model.service;

// import jakarta.annotation.PostConstruct;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import lombok.Setter;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.UpdateWallet;
import org.unigrid.janus.model.cdi.CDIUtil;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.unigrid.janus.model.signal.AccountSelectedEvent;

@ApplicationScoped
public class PollingService {

	private Timer pollingTimer;
	private Timer updateTimer;
	private Timer syncTimer;
	private Timer longSyncTimer;
	private Timer timer;
	private ScheduledFuture<?> cosmosPoll = null;
	@Getter
	@Setter
	private Boolean syncTimerRunning = false;
	@Getter
	@Setter
	private Boolean longSyncTimerRunning = false;
	@Getter
	@Setter
	private Boolean pollingTimerRunning = false;
	@Getter
	@Setter
	private Boolean updateTimerRunning = false;

	@Inject
	private DebugService debug;
	@Inject
	private UpdateWallet updateWallet;

	ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	@Inject
	private CosmosService cosmosService;
	@Inject
	private AccountsData accountsData;
	// TODO: These methods are all doing the same thing - generalize and put into a common class!

	@PostConstruct
	private void init() {
		executorService = Executors.newSingleThreadScheduledExecutor();
	}

	public void onAccountSelected(@Observes AccountSelectedEvent event) {
		startPoll();
	}

	// Convert all polls to this one where you can pass in the task to be executed
	// pollingService.startPolling(new LongPollingTask(), 1000, "polling", new Timer());
	public void startPolling(TimerTask task, int interval, String timerName, Timer timer) {
		debug.print(timerName + " started", PollingService.class.getSimpleName());
		timer.scheduleAtFixedRate(task, 0, interval);
	}

	// pollingService.stopPolling(pollingTimer, "polling");
	public void stopPole(Timer timer, String timerName) {
		if (timer != null) {
			timer.cancel();
			timer.purge();
			debug.print(timerName + " stopped", PollingService.class.getSimpleName());
		}
	}

	public void poll(int interval) {
		debug.print("poll started", PollingService.class.getSimpleName());
		pollingTimer = new Timer(true);
		pollingTimer.scheduleAtFixedRate(new LongPollingTask(), 0, interval);
		setPollingTimerRunning(true);
	}

	public void stopPolling() {
		if (pollingTimer != null) {
			pollingTimer.cancel();
			pollingTimer.purge();
			setPollingTimerRunning(false);
		}
	}

	public void pollForUpdate(int interval) {
		debug.print("starting the update timer", PollingService.class.getSimpleName());

		// TODO: Apparently, Java timers don't like proxy objects - can we clean this up ?
		updateTimer = new Timer(true);
		updateTimer.scheduleAtFixedRate(CDIUtil.unproxy(updateWallet), 0, interval);
		setUpdateTimerRunning(true);
	}

	public void stopPollingForUpdate() {
		if (updateTimer != null) {
			updateTimer.cancel();
			updateTimer.purge();
			setUpdateTimerRunning(false);
		}
	}

	public void pollForSync(int interval) {
		debug.print("starting sync poll", PollingService.class.getSimpleName());
		syncTimer = new Timer(true);
		syncTimer.scheduleAtFixedRate(new SyncPollingTask(), 0, interval);
		setSyncTimerRunning(true);
	}

	public void stopSyncPoll() {
		if (syncTimer != null) {
			syncTimer.cancel();
			syncTimer.purge();
			setSyncTimerRunning(false);
		}
	}

	public void longPollForSync(int interval) {
		debug.print("starting long sync poll", PollingService.class.getSimpleName());
		longSyncTimer = new Timer(true);
		longSyncTimer.scheduleAtFixedRate(new SyncPollingTask(), 0, interval);
		setLongSyncTimerRunning(true);
	}

	public void stopLongSyncPoll() {
		if (longSyncTimer != null) {
			longSyncTimer.cancel();
			longSyncTimer.purge();
			setLongSyncTimerRunning(false);
		}
	}

	public void startPoll() {
		stopPoll(); // Stop any existing poll
		if (accountsData.getSelectedAccount() == null) {
			System.out.println("AccountsData is empty!");
			return;
		}
		String address = accountsData.getSelectedAccount().getAddress();
		Runnable task = () -> {
			try {
				cosmosService.loadAccountData(address);
			} catch (Exception ex) {
				// Handle exception
			}
		};

		// Schedule the new polling task
		cosmosPoll = executorService.scheduleWithFixedDelay(
			task, 5, 5, TimeUnit.SECONDS);
	}

	public void stopPoll() {
		if (cosmosPoll != null && !cosmosPoll.isCancelled()) {
			cosmosPoll.cancel(true);
		}
	}

}
