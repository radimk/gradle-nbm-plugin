package org.gradle.plugins.nbm.integtest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

public final class ManifestUtils {
    public static Map<String, String> readManifest(Path path) throws IOException {
        try (InputStream fileInput = Files.newInputStream(path);
                InputStream input = new BufferedInputStream(fileInput)) {
            Manifest manifest = new Manifest(input);
            Map<?, ?> entries = manifest.getMainAttributes();

            Map<String, String> result = new HashMap<>(2 * entries.size());
            for (Map.Entry<?, ?> entry: entries.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue().toString());
            }

            return result;
        }
    }

    private ManifestUtils() {
        throw new AssertionError();
    }
}
