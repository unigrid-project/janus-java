package org.unigrid.janus.model;

import org.apache.commons.configuration2.SystemConfiguration;

public class Preferences {
	private static final String ROOT_NODE = "janus";
	public static final SystemConfiguration PROPS = new SystemConfiguration();

	public static <T> void changePropertyDefault(Class<T> type, String key, T defaultValue) {
		PROPS.setProperty(key, PROPS.get(type, key, defaultValue));
	}

	public static java.util.prefs.Preferences get() {
		return java.util.prefs.Preferences.userRoot().node(ROOT_NODE);
	}
}
