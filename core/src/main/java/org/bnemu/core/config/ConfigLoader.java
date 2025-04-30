package org.bnemu.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ConfigLoader {

    public static CoreConfig load(String file) throws ConfigLoadException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        CoreConfig config = null;

        // Attempt to load from classpath (resource in JAR)
        try (InputStream stream = ConfigLoader.class.getClassLoader().getResourceAsStream(file)) {
            if (stream != null) {
                config = mapper.readValue(stream, CoreConfig.class);
                return config;
            }
        } catch (Exception e) {
            throw new ConfigLoadException("Failed to load config from classpath: " + file, e);
        }

        // Fallback to filesystem
        File configFile = new File(file);
        if (!configFile.exists()) {
            throw new ConfigLoadException("Config file not found in classpath or filesystem: " + file);
        }
        try (InputStream stream = new FileInputStream(configFile)) {
            config = mapper.readValue(stream, CoreConfig.class);
            return config;
        } catch (Exception e) {
            throw new ConfigLoadException("Failed to load config from file system: " + configFile.getAbsolutePath(), e);
        }
    }
}
