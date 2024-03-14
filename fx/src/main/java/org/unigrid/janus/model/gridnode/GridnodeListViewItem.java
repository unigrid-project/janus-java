package org.unigrid.janus.model.gridnode;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
public class GridnodeListViewItem {
    private String status;
    private String key;
    private String address;
    private boolean showStartButton;

    public GridnodeListViewItem(String status, String key, String address) {
        this.status = status;
        this.key = key;
        this.address = address;
        this.showStartButton = "INACTIVE".equals(status);
    }

    public void startGridnode() {
        // Logic to start the grid node by its key
        System.out.println("Starting grid node with key: " + key);
    }

    @Override
    public String toString() {
        return "Status: " + status + ", Key: " + key + ", Address: " + address;
    }
}
