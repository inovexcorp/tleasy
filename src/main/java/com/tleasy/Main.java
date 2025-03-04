package com.tleasy;

import com.tleasy.rest.SimpleTleClient;
import com.tleasy.tle.SimpleTleFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Set;

public class Main {

    public static void main(String... args) throws Exception {
        TleClient client = SimpleTleClient.builder()
                .tleDataEndpoint(args[0])
                .keystoreFile(new File(args[1]))
                .keystorePassword(args[2].toCharArray())
                .truststoreFile(new File(args[3]))
                .truststorePassword(args[4].toCharArray())
                .build();
        TleFilter filter = SimpleTleFilter.builder()
                .targetNoradIds(Set.of(args[5].split("\\s")))
                .build();

        try (InputStream tleData = client.fetchTle();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            filter.filter(tleData, output);
            System.out.println(output.toString(Charset.defaultCharset()));
        }


    }


}
