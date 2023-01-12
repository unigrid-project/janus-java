package org.unigrid.janus.model.filesystem.memoryfs;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class VirtualTimeInfo {
	@Builder.Default private Instant accessTime = Instant.now();
	@Builder.Default private Instant creationTime = Instant.now();
	@Builder.Default private Instant writeTime = Instant.now();
}
