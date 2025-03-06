package com.realmone.tleasy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This simple interface describes a way of filtering a stream of TLE data.  The incoming {@link InputStream} of TLE
 * data can be filtered for output.
 */
public interface TleFilter {

    /**
     * This function will filter a stream of TLE data flowing through the process.
     *
     * @param tleStream    The incoming TLE data
     * @param outputStream The TLE data to include in the output of the filter
     * @throws IOException If there is an issue working with the streams of data
     */
    long filter(InputStream tleStream, OutputStream outputStream) throws IOException;
}
