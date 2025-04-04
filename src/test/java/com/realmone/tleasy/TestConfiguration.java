package com.realmone.tleasy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class TestConfiguration {

    private File configFile;
    private File backupConfigFile;

    @Before
    public void setUp() throws Exception {
        // Determine the configuration file used by the Configuration class
        configFile = new File(System.getProperty("user.home"), ".tleasy-config.properties");
        // If a configuration file already exists, back it up
        if (configFile.exists()) {
            backupConfigFile = new File(configFile.getAbsolutePath() + ".backup");
            if (backupConfigFile.exists()) {
                backupConfigFile.delete();
            }
            if (!configFile.renameTo(backupConfigFile)) {
                throw new IOException("Failed to backup existing configuration file.");
            }
        }

        // Ensure the configuration file does not exist for tests that rely on defaults
        if (configFile.exists()) {
            configFile.delete();
        }

        // Reset the internal properties field in Configuration using reflection
        Field propertiesField = Configuration.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        propertiesField.set(null, new Properties());
    }

    @After
    public void tearDown() {
        // Clean up the test configuration file if it was created
        if (configFile.exists()) {
            configFile.delete();
        }
        // Restore the backup configuration file if it existed
        if (backupConfigFile != null && backupConfigFile.exists()) {
            backupConfigFile.renameTo(configFile);
        }
    }

    @Test
    public void testLoadDefaults() throws Exception {
        // Invoke the private load() method to simulate initial loading with no config file
        Method loadMethod = Configuration.class.getDeclaredMethod("load");
        loadMethod.setAccessible(true);
        loadMethod.invoke(null);

        // Compute expected default keystore path
        String expectedDefaultKeystore = System.getProperty("user.home") + File.separator + "tleasy"
                + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "keystore.p12";

        File keystoreFile = Configuration.getKeyStoreFile();
        assertNotNull("Default keystore file should not be null", keystoreFile);
        assertEquals("Default keystore path mismatch", expectedDefaultKeystore, keystoreFile.getPath());
    }

    @Test
    public void testConfigure() throws IOException {
        Properties testProps = new Properties();
        String testEndpoint = "http://example.com/tle";
        String testKeystore = "/tmp/test-keystore.p12";
        String testKeystorePassword = "secret";
        String testSkipCert = "true";
        String testDarkTheme = "true";

        testProps.setProperty(Configuration.PROP_TLE_ENDPOINT, testEndpoint);
        testProps.setProperty(Configuration.PROP_KEYSTORE, testKeystore);
        testProps.setProperty(Configuration.PROP_KEYSTORE_PASS, testKeystorePassword);
        testProps.setProperty(Configuration.PROP_SKIP_CERT_VALIDATE, testSkipCert);
//        testProps.setProperty(Configuration.PROP_DARK_THEME, testDarkTheme);

        // Configure with test properties
        Configuration.configure(testProps);

        // Verify that the configuration file now exists
        assertTrue("Configuration file should exist after configuring", configFile.exists());

        // Verify that getters return the correct values
        assertEquals("TLE endpoint mismatch", testEndpoint, Configuration.getTleDataEndpoint());
        assertNotNull("Keystore file should not be null", Configuration.getKeyStoreFile());
        assertEquals("Keystore path mismatch", testKeystore, Configuration.getKeyStoreFile().getPath());
        assertArrayEquals("Keystore password mismatch", testKeystorePassword.toCharArray(), Configuration.getKeystorePassword());
        assertTrue("Skip certificate validation should be true", Configuration.isSkipCertificateValidation());
//        assertTrue("Dark theme should be true", Configuration.isDarkTheme());

        // Additionally, verify that the properties are correctly stored in the file
        Properties loadedProps = new Properties();
        try (FileInputStream in = new FileInputStream(configFile)) {
            loadedProps.load(in);
        }
        assertEquals("Stored TLE endpoint mismatch", testEndpoint, loadedProps.getProperty(Configuration.PROP_TLE_ENDPOINT));
    }
}