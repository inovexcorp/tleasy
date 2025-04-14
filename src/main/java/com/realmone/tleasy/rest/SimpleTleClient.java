package com.realmone.tleasy.rest;

import com.realmone.tleasy.TleClient;
import lombok.Builder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SimpleTleClient implements TleClient {

    private static final String PKCS12 = "PKCS12";
    private static final String TRUSTSTORE = "truststore.p12";
    private static final String TRUSTSTORE_PW = "realm1p@ss";
    private final String tleDataEndpoint;
    private final File tleFile;
    private final File keystoreFile;
    private final char[] keystorePassword;
    private final boolean skipCertValidation;
    private SSLContext sslContext;

    @Builder
    private SimpleTleClient(String tleDataEndpoint, File tleFile, File keystoreFile, char[] keystorePassword,
                            boolean skipCertValidation)
            throws IOException {
        this.tleDataEndpoint = tleDataEndpoint;
        this.tleFile = tleFile;
        this.keystoreFile = keystoreFile;
        this.keystorePassword = keystorePassword;
        this.skipCertValidation = skipCertValidation;
        this.sslContext = createSecureSslContext(keystoreFile, keystorePassword, skipCertValidation);
    }

    /**
     * Fetches the TLE data using Java 8's HttpURLConnection.
     *
     * @return The {@link InputStream} of TLE data
     * @throws IOException If there is an issue connecting to the remote server
     */
    @Override
    public InputStream fetchTle() throws IOException {
        // If an endpoint was provided instead of a file
        if (tleDataEndpoint != null && !tleDataEndpoint.isEmpty()) {
            URL url = URI.create(tleDataEndpoint).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            HttpURLConnection.setFollowRedirects(true);

            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                if (skipCertValidation) {
                    // Bypass hostname verification
                    httpsConnection.setHostnameVerifier((hostname, session) -> true);
                }
            }

            connection.setRequestMethod("GET");
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            // Redirects are now handled automatically
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Remote server did not respond with success: " + responseCode);
            }

            return connection.getInputStream();
        } else { // If a file was provided instead of an endpoint
            return Files.newInputStream(tleFile.toPath());
        }
    }

    @Override
    public void trustCerts() throws IOException {
        // Only performed if an endpoint was provided instead of a file
        if (tleDataEndpoint != null && !tleDataEndpoint.isEmpty()) {
            URL url = URI.create(tleDataEndpoint).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

                // Trust everything so we can grab the certs without it failing
                try {
                    SSLContext trustAllSSLContext = getTrustAllSSLContext(keystoreFile, keystorePassword);
                    httpsConnection.setSSLSocketFactory(trustAllSSLContext.getSocketFactory());
                    httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    connection.connect();
                    this.sslContext = createSecureSslContext(keystoreFile, keystorePassword, skipCertValidation,
                            httpsConnection.getServerCertificates());
                } catch (GeneralSecurityException ex) {
                    throw new RuntimeException(ex);
                }
            }
            connection.disconnect();
        }
    }

    /**
     * Creates an {@link SSLContext} for secure communication, with an option to skip certificate validation. Uses the
     * provided keystore {@link File} with the provided password.
     *
     * @param keystoreFile       The client keystore file
     * @param keystorePassword   The password for the keystore
     * @param skipCertValidation Whether to disable SSL certificate validation
     * @return Configured SSLContext
     * @throws IOException If any security-related issues occur
     */
    private static SSLContext createSecureSslContext(File keystoreFile, char[] keystorePassword,
                                                     boolean skipCertValidation) throws IOException {
        return createSecureSslContext(keystoreFile, keystorePassword, skipCertValidation, new Certificate[]{});
    }

    /**
     * Creates an {@link SSLContext} for secure communication, with an option to skip certificate validation. Uses the
     * provided keystore {@link File} with the provided password. Also loads the provided server certificates into the
     * embedded truststore.
     *
     * @param keystoreFile       The client keystore file
     * @param keystorePassword   The password for the keystore
     * @param skipCertValidation Whether to disable SSL certificate validation
     * @param serverCerts        The list of certificates to load into the truststore
     * @return Configured SSLContext
     * @throws IOException If any security-related issues occur
     */
    private static SSLContext createSecureSslContext(File keystoreFile, char[] keystorePassword, boolean skipCertValidation,
                                                     Certificate[] serverCerts) throws IOException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // Load client keystore
            KeyManagerFactory kmf = getKeyManagerFactory(keystoreFile, keystorePassword);

            TrustManagerFactory tmf = null;
            if (!skipCertValidation) {
                // Load truststore
                KeyStore trustStore = KeyStore.getInstance(PKCS12);
                try (InputStream trustStoreStream = SimpleTleClient.class.getResourceAsStream(TRUSTSTORE)) {
                    trustStore.load(trustStoreStream, TRUSTSTORE_PW.toCharArray());
                }
                // Load in server certs to trust
                for (Certificate serverCert : serverCerts) {
                    loadCertIfMissing(serverCert, trustStore);
                }
                // Create TrustManager (to trust the remote server)
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
            }

            // If skipping certificate validation, use a permissive TrustManager
            TrustManager[] trustManagers;
            if (skipCertValidation) {
                trustManagers = new TrustManager[]{getTrustAllManager()};
            } else {
                trustManagers = tmf.getTrustManagers();
            }

            // Initialize SSLContext
            sslContext.init(kmf.getKeyManagers(), trustManagers, null);

            return sslContext;
        } catch (GeneralSecurityException e) {
            throw new IOException("Issue managing TLS certificates to make TLE file request", e);
        }
    }

    /**
     * Checks whether the provided {@link Certificate} is present in the provided {@link KeyStore} by iterating through
     * the loaded certs and checking for equality.
     *
     * @param cert The {@link Certificate} to search for
     * @param keyStore The target {@link KeyStore}
     * @return True if the certificate is within the keystore; false otherwise
     * @throws KeyStoreException If an error occurs pulling information from the keystore
     */
    private static boolean certInKeystore(Certificate cert, KeyStore keyStore) throws KeyStoreException {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String currentAlias = aliases.nextElement();
            Certificate existingCert = keyStore.getCertificate(currentAlias);
            if (existingCert != null && existingCert.equals(cert)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads the provided {@link Certificate} into the provided {@link KeyStore} if not already present. The alias
     * is autogenerated with "tleasy" as a prefix and a UUID ad the end.
     *
     * @param cert A {@link Certificate} to load
     * @param keyStore The target {@link KeyStore}
     * @throws KeyStoreException If an issue occurs loading the certificate into the keystore
     */
    private static void loadCertIfMissing(Certificate cert, KeyStore keyStore) throws KeyStoreException {
        if (!certInKeystore(cert, keyStore)) {
            String alias = "tleasy" + UUID.randomUUID();
            keyStore.setCertificateEntry(alias, cert);
        }
    }

    /**
     * Creates a {@link KeyManagerFactory} with the provided {@link KeyStore} with the provided password for use in
     * creating an {@link SSLContext}.
     *
     * @param keystoreFile       The client keystore file
     * @param keystorePassword   The password for the keystore
     * @return A {@link KeyManagerFactory} to use to create an {@link SSLContext}
     * @throws GeneralSecurityException If an issue occurs creating and loading the keystore
     * @throws IOException If an issue occurs reading the keystore file
     */
    private static KeyManagerFactory getKeyManagerFactory(File keystoreFile, char[] keystorePassword)
            throws GeneralSecurityException, IOException {
        // Load client keystore
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = KeyStore.getInstance(PKCS12);
        try (InputStream keyStoreStream = Files.newInputStream(keystoreFile.toPath())) {
            keyStore.load(keyStoreStream, keystorePassword);
        }
        kmf.init(keyStore, keystorePassword);
        return kmf;
    }

    /**
     * Creates an {@link SSLContext} that accepts all certificates (instead of the embedded truststore) and uses the
     * provided keystore and password.
     *
     * @param keystoreFile       The client keystore file
     * @param keystorePassword   The password for the keystore
     * @return An {@link SSLContext} to use to make HTTPS connections
     * @throws GeneralSecurityException If an issue occurs creating and loading the keystore and truststore
     * @throws IOException If an issue occurs reading the keystore file
     */
    private static SSLContext getTrustAllSSLContext(File keystoreFile, char[] keystorePassword)
            throws GeneralSecurityException, IOException {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Load client keystore
        KeyManagerFactory kmf = getKeyManagerFactory(keystoreFile, keystorePassword);

        // Get Trust All Manager
        TrustManager[] trustManagers = new TrustManager[]{getTrustAllManager()};

        // Initialize SSLContext
        sslContext.init(kmf.getKeyManagers(), trustManagers, null);

        return sslContext;
    }

    /**
     * Creates a {@link TrustManager} that accepts all certificates.
     *
     * @return A {@link X509TrustManager} all certificates.
     */
    private static TrustManager getTrustAllManager() {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        };
    }
}