package org.unigrid.janus.model.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.unigrid.janus.model.qualifer.MyCustomQualifier;
import org.unigrid.pax.sdk.cosmos.service.GrpcService;


// The producer class
@ApplicationScoped
public class GrpcServiceProducer {

    @Produces
    @MyCustomQualifier // Custom qualifier to resolve ambiguity
    public GrpcService createMyCustomGrpcService() {
        // Instantiate your GrpcService here
        GrpcService service = new GrpcService();
        // Configuration or other setup can be done here as needed
        return service;
    }
}