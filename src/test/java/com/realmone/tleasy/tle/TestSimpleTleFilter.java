package com.realmone.tleasy.tle;

import com.realmone.tleasy.TleFilter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

@RunWith(BlockJUnit4ClassRunner.class)
public class TestSimpleTleFilter {

    @Test(expected = IOException.class)
    public void testBad() throws Exception {
        // Invalid TLE data (missing required format)
        String invalidTleData = "INVALID DATA LINE\nNOT A TLE FORMAT\n1 99999U 12345A   12345.67890123  .00001234  00000-0  12345-6 0  9991";

        // Convert to InputStream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidTleData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Create TleFilter
        TleFilter filter = com.realmone.tleasy.tle.SimpleTleFilter.builder().targetNoradIds(Collections.singleton("99999")).build();

        // Expect an IOException due to invalid format
        filter.filter(inputStream, outputStream);
    }

    @Test
    public void test() throws Exception {
        com.realmone.tleasy.tle.SimpleTleFilter filter = com.realmone.tleasy.tle.SimpleTleFilter.builder()
                .targetNoradIds(Collections.singleton("62903"))
                .build();
        try (InputStream is = Files.newInputStream(Paths.get("src/test/resources/data.tle"));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            filter.filter(is, baos);
            String result = baos.toString(Charset.defaultCharset().name());
            Assert.assertEquals("COSMOS 2582\n1 62903U 25026B   25054.91904369  .00003073  00000+0  29444-3 0  9990\n2 62903  81.9966 201.7787 0014692 190.6063 169.4848 14.92808070  2797\n", result);
        }
    }
}
