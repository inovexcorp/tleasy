package com.tleasy.tle;

import com.tleasy.TleFilter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@RunWith(BlockJUnit4ClassRunner.class)
public class TestSimpleTleFilter {

    @Test(expected = IOException.class)
    public void testBad() throws Exception {
        // Invalid TLE data (missing required format)
        String invalidTleData = """
                INVALID DATA LINE
                NOT A TLE FORMAT
                1 99999U 12345A   12345.67890123  .00001234  00000-0  12345-6 0  9991
                """;

        // Convert to InputStream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidTleData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Create TleFilter
        TleFilter filter = SimpleTleFilter.builder().targetNoradIds(Set.of("99999")).build();

        // Expect an IOException due to invalid format
        filter.filter(inputStream, outputStream);
    }

    @Test
    public void test() throws Exception {
        SimpleTleFilter filter = SimpleTleFilter.builder()
                .targetNoradIds(Set.of("62903"))
                .build();
        try (InputStream is = new FileInputStream("src/test/resources/data.tle");
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            filter.filter(is, baos);
            String result = baos.toString(Charset.defaultCharset());
            Assert.assertEquals("""
                    COSMOS 2582
                    1 62903U 25026B   25054.91904369  .00003073  00000+0  29444-3 0  9990
                    2 62903  81.9966 201.7787 0014692 190.6063 169.4848 14.92808070  2797
                    """, result);
        }
    }
}
