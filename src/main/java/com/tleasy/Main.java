package com.tleasy;

import com.tleasy.rest.SimpleTleClient;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {

    public static void main(String... args) throws Exception {
        TleClient client = SimpleTleClient.builder()
                .tleDataEndpoint(args[0])
                .keystoreFile(new File(args[1]))
                .keystorePassword(args[2].toCharArray())
                .truststoreFile(new File(args[3]))
                .truststorePassword(args[4].toCharArray())
                .build();

        try (InputStream data = client.fetchTle()) {
            // Convert InputStream to String
            String response = new Scanner(data, StandardCharsets.UTF_8).useDelimiter("\\A").next();

            // Print to stdout
            System.out.println(response);
        }
    }
}
