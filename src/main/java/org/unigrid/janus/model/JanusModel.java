package org.unigrid.janus.model;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;

@ApplicationScoped
public class JanusModel {

	public enum AppState {
		STARTING,
		LOADED,
		RESTARTING
	}

	@Getter
	private AppState appState;

	public void setAppState(AppState state) {
		appState = state;
	}
}
