package org.unigrid.janus.model.service;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

@Stateless
public class RPCService {
	@PostConstruct
	private void init() {
		//ClientBuilder.newBuilder()
		//	.register()
		//	.build().target("http://localhost:8080/api/v1");
	}
}
