package org.unigrid.janus.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FXMLInjectableImpl<T> implements FXMLInjectable<T> {
	final private T injectable;

	@Override
	public T get() {
		return injectable;
	}

	public static <T> FXMLInjectable<T> of(T injectable) {
		return new FXMLInjectableImpl<T>(injectable);
	}
}
