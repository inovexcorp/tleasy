# TLEasy
This project builds the executable for TLEasy, enabling you to download two-line-element set data from a configurable URL (protected with a cert) and parse out the specific lines of interest.

## To Build
The project depends on Java 8, to be compatible with the customer environment so ensure you are building and running the application with that version.

Build the project with Maven to start:
```bash
mvn clean install
```

Then to run the program, run the `tleasy-jar-with-dependencies.jar`
```bash
java -jar tleasy-jar-with-dependenceies
```

This will open the Java Swing application.

# To Test
The separate tl-easy-server project will create a mock endpoint protected with SSL authentication that serves up a file of TLE data to parse.

When the TLEasy configuration window appears, you can enter the following values to connect to the mock endpoint for testing.

- **TLE Data Endpoint:** `https://localhost:8443/secure/download`
- **Keystore File Path:** `/path/to/tleasy/src/test/resources/keywstore.p12`
- **Keystore Password:** `realm1p@ss`
- **Truststore File Path:** `/path/to/tleasy/src/test/resources/truststore.p12`
- **Truststore Password:** `realm1p@ss`
- **Skip Cert Validation:** `true`
