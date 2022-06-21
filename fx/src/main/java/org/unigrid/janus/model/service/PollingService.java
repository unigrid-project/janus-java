package org.unigrid.janus.model.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Timer;

@ApplicationScoped
public class PollingService {
	private static Timer pollingTimer;
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
}
