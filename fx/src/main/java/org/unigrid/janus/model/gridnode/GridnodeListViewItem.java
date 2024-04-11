package org.unigrid.janus.model.gridnode;

import lombok.Getter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

@Getter
public class GridnodeListViewItem {
    private final SimpleStringProperty status;
    private final SimpleStringProperty key;
    private final SimpleStringProperty address;
    private final BooleanProperty showStartButton;

    public GridnodeListViewItem(String status, String key, String address) {
        this.status = new SimpleStringProperty(status);
        this.key = new SimpleStringProperty(key);
        this.address = new SimpleStringProperty(address);
        this.showStartButton = new SimpleBooleanProperty("INACTIVE".equals(status));
    }

    // Getters for JavaFX properties
    public SimpleStringProperty statusProperty() {
        return status;
    }

    public SimpleStringProperty keyProperty() {
        return key;
    }

    public SimpleStringProperty addressProperty() {
        return address;
    }

    public BooleanProperty showStartButtonProperty() {
        return showStartButton;
    }

    // Standard getters for the actual values
    public String getStatus() {
        return status.get();
    }

    public String getKey() {
        return key.get();
    }

    public String getAddress() {
        return address.get();
    }

    public boolean isShowStartButton() {
        return showStartButton.get();
    }

    // You might also want to provide setters that update the properties
    public void setStatus(String status) {
        this.status.set(status);
    }

    public void setKey(String key) {
        this.key.set(key);
    }

    public void setAddress(String address) {
        this.address.set(address);
    }

    public void setShowStartButton(boolean showStartButton) {
        this.showStartButton.set(showStartButton);
    }

    @Override
    public String toString() {
        return "Status: " + getStatus() + ", Key: " + getKey() + ", Address: " + getAddress();
    }
}

