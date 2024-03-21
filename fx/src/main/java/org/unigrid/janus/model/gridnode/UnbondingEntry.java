package org.unigrid.janus.model.gridnode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnbondingEntry {
    private String account;
    private long amount;
    private long completionTime;
   
    public String getFormattedAmount() {
        double amountInDouble = this.amount / 100000000.0;
        return String.format("%.2f UGD", amountInDouble);
    }

    public String getFormattedCompletionTime() {
        Instant instant = Instant.ofEpochSecond(this.completionTime);
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                .withZone(ZoneId.systemDefault())
                                .format(instant);
    }
}
