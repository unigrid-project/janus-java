package org.unigrid.janus.model.service;

import org.unigrid.janus.model.transaction.GridnodeTransaction;

public class GridnodeHandler {

	private CosmosService cosmosClient;

	public GridnodeHandler(String apiUrl) {
		//this.cosmosClient = new CosmosRestClient(apiUrl);
	}

	public String delegateToGridnode(GridnodeTransaction transaction) throws Exception {
		return "test"; //cosmosClient.sendDelegation(transaction);
	}

}
