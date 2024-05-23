package org.unigrid.janus.model;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@ApplicationScoped
public class PublicKeysModel {
    private ObservableList<String> publicKeys = FXCollections.observableArrayList();

    public ObservableList<String> getPublicKeys() {
        return publicKeys;
    }

    public void setPublicKeys(List<String> newKeys) {
        publicKeys.setAll(newKeys);
    }
}
