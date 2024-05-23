package org.unigrid.janus.model.gridnode;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ActivateGridnode {
    private String gridnodeId;
    private String publicKey;

}

