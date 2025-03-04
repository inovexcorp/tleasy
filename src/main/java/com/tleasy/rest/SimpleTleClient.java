package com.tleasy.rest;

import com.tleasy.TleClient;
import lombok.Builder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class SimpleTleClient implements TleClient {

    private static final String PKCS12 = "PKCS12";

    private final String tleDataEndpoint;
    private final HttpClient httpClient;

    @Builder
    private SimpleTleClient(String tleDataEndpoint, File keystoreFile, char[] keystorePassword,
                            File truststoreFile, char[] truststorePassword) throws IOException {
        // Target this endpoint
        this.tleDataEndpoint = tleDataEndpoint;
        // Initialize the httpClient
        this.httpClient = createSecureHttpClient(keystoreFile, keystorePassword, truststoreFile, truststorePassword);
    }

    /**
     * Simple implementation that leverages the {@link HttpClient} API and {@link SSLContext} configurations to hook
     * into a remote server and fetch an {@link InputStream} of TLE data.
     *
     * @return The {@link InputStream} of TLE data
     * @throws IOException          If there is an issue connecting to the remote server and fetching the data
     * @throws InterruptedException If there is an issue locally while making the connection to the remote server
     */
    @Override
    public InputStream fetchTle() throws IOException, InterruptedException {
        // Build the request we'll make to the TLE data endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tleDataEndpoint))
                .GET()
                .build();
        // Make the request using our secure http client
        HttpResponse<InputStream> response = httpClient.send(request,
                // Process the response body as an InputStream to stream through processing
                HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            //TODO get body string message?
            throw new IOException("Remote server did not respond with success: ");
        } else {
            return response.body();
        }
    }

    private static HttpClient createSecureHttpClient(File keystoreFile, char[] keystorePassword,
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
            try (var trustStoreStream = new FileInputStream(truststoreFile)) {
                trustStore.load(trustStoreStream, truststorePassword);
            }
            // Create KeyManager (for client authentication)
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keystorePassword);
            // Create TrustManager (to trust the remote server)
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            // Initialize the SSLContext given the configured key and trust managers
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            // Construct our HttpClient implementation
            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();
        } catch (GeneralSecurityException e) {
            throw new IOException("Issue managing TLS certificates to make TLE file request", e);
        }
    }
}
