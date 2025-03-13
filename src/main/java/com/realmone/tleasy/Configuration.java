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
    // TODO: Update this to the correct path from Dan
    private static final String DEFAULT_CERT = System.getProperty("user.home") + File.pathSeparator + "tleasy"
            + File.pathSeparator + "src" + File.pathSeparator + "test" + File.pathSeparator + "resources"
            + File.pathSeparator + "localhost-cert.pem";

    public static final String PROP_TLE_ENDPOINT = "tle_data_endpoint";
    public static final String PROP_CERT = "certificate";
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

    public static File getCertFile() {
        String path = properties.getProperty(PROP_CERT, "");
        return path.isEmpty() ? null : new File(path);
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
        } else {
            properties.setProperty(PROP_CERT, DEFAULT_CERT);
        }
    }
}