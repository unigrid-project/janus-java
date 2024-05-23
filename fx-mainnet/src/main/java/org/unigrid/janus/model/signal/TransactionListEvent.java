package org.unigrid.janus.model.signal;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionListEvent {
    private final List<String> transactionsSent;
    private final List<String> transactionsReceived;
}
