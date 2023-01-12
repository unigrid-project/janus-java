package org.unigrid.janus.model.service.api;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MountFailureException extends Exception {
	public MountFailureException(Exception ex) {
		super(ex);
	}
}
