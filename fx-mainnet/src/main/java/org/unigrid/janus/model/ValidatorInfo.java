package org.unigrid.janus.model;

public class ValidatorInfo {
    private String moniker;
    private String operatorAddress;
    private String commission;

    public ValidatorInfo(String moniker, String operatorAddress, String commission) {
        this.moniker = moniker;
        this.operatorAddress = operatorAddress;
	this.commission = commission;
    }

    public String getMoniker() {
        return moniker;
    }

    public String getOperatorAddress() {
        return operatorAddress;
    }
    
    public String getCommission() {
        return commission;
    }

    @Override
    public String toString() {
        // This is what will be displayed in the ComboBox
        return moniker + " " + commission;
    }
}