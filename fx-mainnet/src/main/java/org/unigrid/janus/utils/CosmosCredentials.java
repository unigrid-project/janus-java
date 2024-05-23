package org.unigrid.janus.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CosmosCredentials {

    private ECKey ecKey;
    private String address;

    public static CosmosCredentials create(ECKey ecKey, String addressPrefix) {
        CosmosCredentials credentials = new CosmosCredentials();
        credentials.ecKey = ecKey;
        credentials.address = AddressUtil.ecKeyToAddress(ecKey, addressPrefix);
        return credentials;
    }

    public static CosmosCredentials create(byte[] privateKey, String addressPrefix) {
        ECKey ecKey = ECKey.fromPrivate(privateKey);
        return create(ecKey, addressPrefix);
    }

    public byte[] sign(byte[] data) {
        if (this.ecKey == null) {
            throw new IllegalStateException("ECKey not initialized in CosmosCredentials");
        }
        return this.ecKey.sign(Sha256Hash.wrap(data)).encodeToDER();
    }

    public ECKey getEcKey() {
        return ecKey;
    }

    public String getAddress() {
        return address;
    }
}

