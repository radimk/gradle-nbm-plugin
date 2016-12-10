package org.gradle.plugins.nbm;

import java.util.HashMap;
import java.util.Map;

public final class CustomManifestEntries {
    private final Map<String, Object> entries;

    public CustomManifestEntries() {
        this.entries = new HashMap<>();
    }

    public void entry(String key, Object value) {
        entries.put(key, value);
    }

    public void entries(Map<String, ?> newEntries) {
        entries.putAll(newEntries);
    }

    public Map<String, Object> getEntries() {
        return entries;
    }
}
