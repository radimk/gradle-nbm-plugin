package org.gradle.plugins.nbm.integtest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ManifestUtils {
    public static Map<String, String> readManifest(Path path) throws IOException {
        try (InputStream fileInput = Files.newInputStream(path);
                InputStream input = new BufferedInputStream(fileInput)) {
            Properties properties = new Properties();
            properties.load(input);

            Map<String, String> result = new HashMap<>(2 * properties.size());
            for (Map.Entry<?, ?> entry: properties.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue().toString());
            }

            return result;
        }
    }

    private ManifestUtils() {
        throw new AssertionError();
    }
}
