package org.unigrid.janus.model.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.TimerTask;

@ApplicationScoped
public class LoadAccountDataTask extends TimerTask {
    private CosmosService cosmosService;
    private String accountAddress;

    public LoadAccountDataTask(CosmosService cosmosService, String accountAddress) {
        this.cosmosService = cosmosService;
        this.accountAddress = accountAddress;
    }

    @Override
    public void run() {
        try {
            cosmosService.loadAccountData(accountAddress);
            // You might also want to handle the rest of your logic here
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            // Handle exceptions appropriately
        }
    }
}

