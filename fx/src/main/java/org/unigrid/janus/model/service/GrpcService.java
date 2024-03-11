package org.unigrid.janus.model.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.unigrid.janus.model.ApiConfig;
import org.unigrid.janus.model.cdi.Eager;

@Eager
@ApplicationScoped
public class GrpcService {

	private ManagedChannel channel;

	@PostConstruct
	private void init() {
		channel = ManagedChannelBuilder.forAddress(ApiConfig.getGRPC_IP(), 9090).usePlaintext().build();
	}

	public ManagedChannel getChannel() {
		return channel;
	}

	@PreDestroy
	private void destroy() {
		channel.shutdown();
	}
}
