package com.realmone.tleasy;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@UtilityClass
public class Configuration {

    private static final File CONFIG_FILE = new File(System.getProperty("user.home"), ".tleasy-config.properties");

    public static final String PROP_TLE_ENDPOINT = "tle_data_endpoint";
    public static final String PROP_KEYSTORE = "keystore";
    public static final String PROP_KEYSTORE_PASS = PROP_KEYSTORE + "_password";
    public static final String PROP_TRUSTSTORE = "truststore";
    public static final String PROP_TRUSTSTORE_PASS = PROP_TRUSTSTORE + "_password";
    public static final String PROP_SKIP_CERT_VALIDATE = "skip_cert_validation";

    private static Properties properties = new Properties();

    static {
        load();
    }

    public static boolean isConfigured() {
        return CONFIG_FILE.exists();
    }

    public static void configure(Properties newProperties) throws IOException {
        properties = newProperties;
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "TLEasy Configuration");
        }
    }

    public static String getTleDataEndpoint() {
        return properties.getProperty(PROP_TLE_ENDPOINT, "");
    }

    public static File getKeyStoreFile() {
        String path = properties.getProperty(PROP_KEYSTORE, "");
        return path.isEmpty() ? null : new File(path);
    }

    public static char[] getKeystorePassword() {
        return properties.getProperty(PROP_KEYSTORE_PASS, "").toCharArray();
    }

    public static File getTruststoreFile() {
        String path = properties.getProperty(PROP_TRUSTSTORE, "");
        return path.isEmpty() ? null : new File(path);
    }

    public static char[] getTruststorePassword() {
        return properties.getProperty(PROP_TRUSTSTORE_PASS, "").toCharArray();
    }

    public static boolean isSkipCertificateValidation() {
        return Boolean.parseBoolean(properties.getProperty(PROP_SKIP_CERT_VALIDATE, "false"));
    }

    private static void load() {
        if (isConfigured()) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                properties.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}