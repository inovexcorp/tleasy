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
    private static final String DEFAULT_KEYSTORE = System.getProperty("user.home") + File.separator + "tleasy"
            + File.separator + "src" + File.separator + "test" + File.separator + "resources"
            + File.separator + "keystore.p12";

    public static final String PROP_TLE_ENDPOINT = "tle_data_endpoint";
    public static final String PROP_TLE_FILE = "tle_data_file";
    public static final String PROP_KEYSTORE = "keystore";
    public static final String PROP_KEYSTORE_PASS = PROP_KEYSTORE + "_password";
    public static final String PROP_SKIP_CERT_VALIDATE = "skip_cert_validation";
    public static final String PROP_DARK_THEME = "dark_theme";
    public static final String PROP_EXE_LOCATION = "exe_location";
    public static final String PROP_SCENARIO_SAVE_FILE = "scenario_save_file";
    public static final String PROP_STK_ACCESS_REPORT_FORMAT = "stk_access_report_format";
    public static final String PROP_TIME_FILTER_MINUTES = "time_filter_minutes";
    public static final String PROP_TIME_FILTER_SECONDS = "time_filter_seconds";

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

    public static File getTleFile() {
        String path = properties.getProperty(PROP_TLE_FILE, "");
        return path.isEmpty() ? null : new File(path);
    }

    public static File getExeFile() {
        String path = properties.getProperty(PROP_EXE_LOCATION, "C:\\Program Files\\AGI\\STK 12\\bin\\AgUiApplication.exe");
        return path.isEmpty() ? null : new File(path);
    }

    public static File getScenarioSaveFile() {
        String path = properties.getProperty(PROP_SCENARIO_SAVE_FILE, "");
        return path.isEmpty() ? null : new File(path);
    }

    public static File getKeyStoreFile() {
        String path = properties.getProperty(PROP_KEYSTORE, "");
        return path.isEmpty() ? null : new File(path);
    }

    public static char[] getKeystorePassword() {
        return properties.getProperty(PROP_KEYSTORE_PASS, "").toCharArray();
    }

    public static boolean isSkipCertificateValidation() {
        return Boolean.parseBoolean(properties.getProperty(PROP_SKIP_CERT_VALIDATE, "false"));
    }

    public static boolean isDarkTheme() {
        return Boolean.parseBoolean(properties.getProperty(PROP_DARK_THEME, "false"));
    }

    public static boolean isCsv() {
        return Boolean.parseBoolean(properties.getProperty(PROP_STK_ACCESS_REPORT_FORMAT));
    }

    public static int getTimeFilterMinutes() {
        try {
            return Integer.parseInt(properties.getProperty(PROP_TIME_FILTER_MINUTES, "7"));
        } catch (NumberFormatException e) {
            return 7; // Default value if parsing fails
        }
    }

    public static int getTimeFilterSeconds() {
        try {
            return Integer.parseInt(properties.getProperty(PROP_TIME_FILTER_SECONDS, "0"));
        } catch (NumberFormatException e) {
            return 0; // Default value if parsing fails
        }
    }

    private static void load() {
        if (isConfigured()) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                properties.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            properties.setProperty(PROP_KEYSTORE, DEFAULT_KEYSTORE);
        }
    }
}