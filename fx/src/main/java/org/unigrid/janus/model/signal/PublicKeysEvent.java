package org.unigrid.janus.model.signal;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PublicKeysEvent {
    private final List<String> publicKeys;
}

