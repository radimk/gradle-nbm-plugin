package org.gradle.plugins.nbm;

import java.util.HashMap;
import java.util.Map;

public final class NbmManifest {
    private final Map<String, String> entries;

    public NbmManifest() {
        this.entries = new HashMap<>();
    }

    public void put(String key, String value) {
        entries.put(key, value);
    }
}
