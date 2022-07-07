package org.unigrid.janus.model.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import java.util.Timer;
import org.unigrid.janus.model.UpdateWallet;

@ApplicationScoped
public class PollingService {

	@Inject
	private UpdateWallet updateWallet;
	
	private static Timer pollingTimer;
	private static Timer updateTimer;
	@Inject
	private DebugService debug;

	public void poll(int interval) {
		debug.print("poll", PollingService.class.getSimpleName());
		pollingTimer = new Timer(true);
		pollingTimer.scheduleAtFixedRate(new LongPollingTask(), 0, interval);
	}

	public void stopPolling() {
		if (pollingTimer != null) {
			pollingTimer.cancel();
			pollingTimer.purge();
		}
	}

	public void pollForUpdate(int interval) {
		System.out.println("starting the update timer");
		updateTimer = new Timer(true);
		updateTimer.scheduleAtFixedRate(updateWallet, 0, interval);

	}

	public void stopPollingForUpdate() {
		if (updateTimer != null) {
			updateTimer.cancel();
			updateTimer.purge();
		}

	}
}
