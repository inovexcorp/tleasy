package com.realmone.tleasy.rest;

import com.realmone.tleasy.TleClient;
import lombok.Builder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class SimpleTleClient implements TleClient {

    private static final String PKCS12 = "PKCS12";
    private final String tleDataEndpoint;
    private final SSLContext sslContext;

    @Builder
    private SimpleTleClient(String tleDataEndpoint, File keystoreFile, char[] keystorePassword,
                            File truststoreFile, char[] truststorePassword) throws IOException {
        this.tleDataEndpoint = tleDataEndpoint;
        this.sslContext = createSecureSslContext(keystoreFile, keystorePassword, truststoreFile, truststorePassword);
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
        }

        connection.setRequestMethod("GET");
        connection.setDoInput(true);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Remote server did not respond with success: " + responseCode);
        }

        return connection.getInputStream();
    }

    /**
     * Creates an SSLContext for secure communication using Java 8-compatible tooling.
     *
     * @return Configured SSLContext
     * @throws IOException If any security-related issues occur
     */
    private static SSLContext createSecureSslContext(File keystoreFile, char[] keystorePassword,
                                                     File truststoreFile, char[] truststorePassword) throws IOException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // Load client keystore
            KeyStore keyStore = KeyStore.getInstance(PKCS12);
            try (InputStream keyStoreStream = new FileInputStream(keystoreFile)) {
                keyStore.load(keyStoreStream, keystorePassword);
            }

            // Load truststore
            KeyStore trustStore = KeyStore.getInstance(PKCS12);
            try (InputStream trustStoreStream = new FileInputStream(truststoreFile)) {
                trustStore.load(trustStoreStream, truststorePassword);
            }

            // Create KeyManager (for client authentication)
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keystorePassword);

            // Create TrustManager (to trust the remote server)
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Initialize the SSLContext with the configured key and trust managers
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return sslContext;
        } catch (GeneralSecurityException e) {
            throw new IOException("Issue managing TLS certificates to make TLE file request", e);
        }
    }
}