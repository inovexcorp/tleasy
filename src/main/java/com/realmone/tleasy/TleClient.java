package com.realmone.tleasy;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is a simple interface that describes a client that will make a request to a remote system that serves
 * data over HTTP using simple TLS client auth.
 */
public interface TleClient {

    /**
     * Fetch the TLE data stream from the remote system.
     *
     * @return The InputStream of the TLE data
     * @throws IOException          If there is an issue making the request to the remote server
     * @throws InterruptedException If there is an issue on the client side making the request
     */
    InputStream fetchTle() throws IOException, InterruptedException;

    /**
     * Runs through the client's endpoint's server certificates and adds all to the underlying truststore.
     *
     * @throws IOException If an issue occurs making the connection to the remote server
     */
    void trustCerts() throws IOException;
}
