package com.realmone.tleasy;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Properties;

@UtilityClass
public class Configuration {


    private static final String PROP_TLE_ENDPOINT = "tle_data_endpoint";
    private static final String PROP_KEYSTORE = "keystore";
    private static final String PROP_KEYSTORE_PASS = PROP_KEYSTORE + "_password";
    private static final String PROP_TRUSTSTORE = "truststore";
    private static final String PROP_TRUSTSTORE_PASS = PROP_TRUSTSTORE + "_password";

    public static final File CONFIG_FILE = new File(System.getProperty("user.home") + File.separator
            + ".tleasy.properties");

    private static final Properties configuration = new Properties();

    static {
        if (CONFIG_FILE.isFile()) {
            try (InputStream fis = new FileInputStream(CONFIG_FILE)) {
                configuration.load(fis);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load configuration file: " + CONFIG_FILE.getAbsolutePath(), e);
            }
        }
    }

    public static void configure() throws IOException {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            writer.write("\n");
        }
    }

    public static boolean isConfigured() {
        return !configuration.isEmpty();
    }

    public static String getTleDataEndpoint() {
        return getProperty(PROP_TLE_ENDPOINT);
    }

    public static File getKeyStoreFile() {
        return new File(getProperty(PROP_KEYSTORE));
    }

    public static char[] getKeystorePassword() {
        return getProperty(PROP_KEYSTORE_PASS).toCharArray();
    }

    public static File getTruststoreFile() {
        return new File(getProperty(PROP_TRUSTSTORE));
    }

    public static char[] getTruststorePassword() {
        return getProperty(PROP_TRUSTSTORE_PASS).toCharArray();
    }

    private static String getProperty(String property) {
        String data = configuration.getProperty(property);
        if (data == null) {
            throw new IllegalArgumentException("Required configuration property not found: " + property);
        } else {
            return data;
        }
    }
}
