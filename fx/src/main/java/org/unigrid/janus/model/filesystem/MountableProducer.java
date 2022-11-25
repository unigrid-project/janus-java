package org.unigrid.janus.model.filesystem;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.unigrid.janus.model.filesystem.memoryfs.linux.MemoryFS;
import org.unigrid.janus.model.service.api.MountFailureException;
import org.unigrid.janus.model.service.api.Mountable;
import org.unigrid.janus.model.signal.UsedSpace;
import org.update4j.OS;

@ApplicationScoped
public class MountableProducer {
	@Inject
	private Event<UsedSpace> usedSpaceEvent;

	@Produces
	public Mountable getOS() throws MountFailureException {
		return switch (OS.CURRENT) {
			case LINUX -> new MemoryFS(usedSpaceEvent);
			case MAC -> null;
			case WINDOWS -> null;
			case OTHER -> { throw new IllegalStateException("Unsupported operating system"); }
		};
	}
}
