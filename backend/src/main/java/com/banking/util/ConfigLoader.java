package com.banking.util;

import java.io.InputStream;
import java.util.Properties;

// =============================================
// SYLLABUS: Unit III - Packages & Utility Classes
//           Unit IV  - File I/O Operations
// =============================================
public class ConfigLoader {
    
    private static Properties properties = new Properties();
    
    static {
        try {
            // Load config.properties from resources folder
            InputStream input = ConfigLoader.class
                .getClassLoader()
                .getResourceAsStream("config.properties");
            
            if (input == null) {
                System.err.println("[ConfigLoader] Unable to find config.properties");
            } else {
                properties.load(input);
                System.out.println("[ConfigLoader] Configuration loaded successfully");
                input.close();
            }
        } catch (Exception e) {
            System.err.println("[ConfigLoader] Error loading config: " + e.getMessage());
        }
    }
    
    /**
     * Returns config value. Priority order:
     * 1. Environment variable (e.g. db.password → DB_PASSWORD)
     * 2. config.properties file
     * 3. null
     */
    public static String get(String key) {
        // Convert "db.password" → "DB_PASSWORD" for env var lookup
        String envKey = key.replace(".", "_").replace("-", "_").toUpperCase();
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        String val = get(key);
        return (val != null) ? val : defaultValue;
    }
}
