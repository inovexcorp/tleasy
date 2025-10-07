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
- **Keystore File Path:** `/path/to/tleasy/src/test/resources/keystore.p12`
- **Keystore Password:** `changeit` -- bespoke one-off password for the test, self-signed keystore
- **Truststore File Path:** `/path/to/tleasy/src/test/resources/truststore.p12`
- **Truststore Password:** `changeit` -- bespoke one-off password for the test self-signed truststore
- **Skip Cert Validation:** `true`

# Opening TLE Files in STK
In addition to downloading, you can also open the TLE file that TLEasy makes for you directly in STK. For this functionality to work, STK must be installed in its default location:
  
`C:\\Program Files\\AGI\\STK 12\\bin\\AgUiApplication.exe`

Then, in the configuration window:
- If not using a Data Endpoint, you can tell TLEasy where your satellite .tle file is located on your local machine.
- You can tell TLEasy where your scenario save file is located for it to use that scenario. If not provided, it will create a new blank scenario and load in the satellites you entered (however, the resulting report TLEasy makes will be blank since no facilities have been provided yet).
- You can also tell it to create the Access report output in .csv or.txt format (default is .txt).
- You can filter out results with an Access duration less than the entered time (default is 7 minutes).