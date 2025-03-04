package com.tleasy;

import java.io.IOException;
import java.io.InputStream;

public interface TleClient {

    InputStream fetchTle() throws IOException, InterruptedException;
}
