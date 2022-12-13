package org.unigrid.janus.model.filesystem.memoryfs;

import com.github.jnrwinfspteam.jnrwinfsp.api.FileAttributes;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class WinFspInfo {
	private byte[] securityToken;
	@Builder.Default private Set<FileAttributes> fileAttributes = new HashSet<>();
}
