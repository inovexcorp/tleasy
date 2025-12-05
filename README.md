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

# Opening TLEasy searches in STK
In addition to downloading, you can also open the TLE file that TLEasy makes for you directly in STK. By default, TLEasy will look for STK's exe in its default location:
  
`C:\\Program Files\\AGI\\STK 12\\bin\\AgUiApplication.exe`

Then, in the configuration window:
- If your STK exe is not in the default location for whatever reason, you can set its path in the configuration window.
- If not using a Data Endpoint, you can tell TLEasy where your satellite .tle file is located on your local machine.
- You can tell TLEasy where your scenario save file is located for it to use that scenario. If not provided, it will create a new blank scenario and load in the satellites you entered (however, the resulting report TLEasy makes will be blank since no facilities have been provided yet).
- You can filter out results with an Access duration less than the entered time (default is 7 minutes).
- You can filter out results with an Access time greater than 24 hours in the future if your scenario length is longer than that.
- You can optionally label satellites in your results file with a julian date greater than 24 hours in the past.
  
When opening your TLEasy search directly in STK, TLEasy will automatically generate a .csv access report and allow you to download. TLEasy will also automatically instruct STK to color satellite paths with adjacent numerical IDs for easier visual grouping.