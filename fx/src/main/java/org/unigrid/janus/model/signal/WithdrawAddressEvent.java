package org.unigrid.janus.model.signal;

public class WithdrawAddressEvent {
    private final String withdrawAddress;

    public WithdrawAddressEvent(String withdrawAddress) {
        this.withdrawAddress = withdrawAddress;
    }

    public String getWithdrawAddress() {
        return withdrawAddress;
    }
}
