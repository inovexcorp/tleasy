package com.realmone.tleasy.rest;

import com.realmone.tleasy.TleClient;
import lombok.Builder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class SimpleTleClient implements TleClient {

    private static final String PKCS12 = "PKCS12";
    private final String tleDataEndpoint;
    private final SSLContext sslContext;
    private final boolean skipCertValidation;

    @Builder
    private SimpleTleClient(String tleDataEndpoint, File keystoreFile, char[] keystorePassword,
                            File truststoreFile, char[] truststorePassword, boolean skipCertValidation)
            throws IOException {
        this.tleDataEndpoint = tleDataEndpoint;
        this.sslContext = createSecureSslContext(keystoreFile, keystorePassword, truststoreFile,
                truststorePassword, skipCertValidation);
        this.skipCertValidation = skipCertValidation;
    }

    /**
     * Fetches the TLE data using Java 8's HttpURLConnection.
     *
     * @return The {@link InputStream} of TLE data
     * @throws IOException If there is an issue connecting to the remote server
     */
    @Override
    public InputStream fetchTle() throws IOException {
        URL url = URI.create(tleDataEndpoint).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

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
        // TODO - consider redirect following
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Remote server did not respond with success: " + responseCode);
        }

        return connection.getInputStream();
    }

    /**
     * Creates an SSLContext for secure communication, with an option to skip certificate validation.
     *
     * @param keystoreFile       The client keystore file
     * @param keystorePassword   The password for the keystore
     * @param truststoreFile     The truststore file (optional, can be null if skipping validation)
     * @param truststorePassword The password for the truststore
     * @param skipCertValidation Whether to disable SSL certificate validation
     * @return Configured SSLContext
     * @throws IOException If any security-related issues occur
     */
    private static SSLContext createSecureSslContext(File keystoreFile, char[] keystorePassword,
                                                     File truststoreFile, char[] truststorePassword,
                                                     boolean skipCertValidation) throws IOException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // Load client keystore
            KeyManagerFactory kmf = null;
            if (keystoreFile != null && keystorePassword != null) {
                KeyStore keyStore = KeyStore.getInstance(PKCS12);
                try (InputStream keyStoreStream = Files.newInputStream(keystoreFile.toPath())) {
                    keyStore.load(keyStoreStream, keystorePassword);
                }
                kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, keystorePassword);
            }

            TrustManagerFactory tmf = null;
            if (!skipCertValidation) {
                // Load truststore
                if (truststoreFile != null && truststorePassword != null) {
                    KeyStore trustStore = KeyStore.getInstance(PKCS12);
                    try (InputStream trustStoreStream = Files.newInputStream(truststoreFile.toPath())) {
                        trustStore.load(trustStoreStream, truststorePassword);
                    }

                    // Create TrustManager (to trust the remote server)
                    tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(trustStore);
                }
            }

            // If skipping certificate validation, use a permissive TrustManager
            javax.net.ssl.TrustManager[] trustManagers;
            if (skipCertValidation) {
                trustManagers = new javax.net.ssl.TrustManager[]{
                        new javax.net.ssl.X509TrustManager() {
                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                            }
                        }
                };
            } else {
                trustManagers = tmf != null ? tmf.getTrustManagers() : null;
            }

            // Initialize SSLContext
            sslContext.init(kmf != null ? kmf.getKeyManagers() : null, trustManagers, null);

            return sslContext;
        } catch (GeneralSecurityException e) {
            throw new IOException("Issue managing TLS certificates to make TLE file request", e);
        }
    }
}