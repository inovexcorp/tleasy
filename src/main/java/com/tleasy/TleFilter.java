package com.tleasy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface TleFilter {

    void filter(InputStream tleStream, OutputStream outputStream) throws IOException;
}
