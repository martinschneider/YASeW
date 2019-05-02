package io.github.martinschneider.justtestlah.awsdevicefarm;

import io.github.martinschneider.justtestlah.configuration.PropertiesHolder;
import io.github.martinschneider.justtestlah.user.UserService;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;
import org.glassfish.jersey.internal.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a custom test spec to run Appium JUnit tests on AWS Device Farm.
 *
 * <p>The configuration (`justtestlah.properties`) is base64-encoded into the template!
 */
public class TestSpecFactory {

  private Logger LOG = LoggerFactory.getLogger(TestSpecFactory.class);

  private PropertiesHolder properties;

  public TestSpecFactory(PropertiesHolder properties) {
    this.properties = properties;
  }

  public String createTestSpec() throws IOException {
    Scanner scanner =
        new Scanner(
            AWSTestRunner.class
                .getClassLoader()
                .getResourceAsStream("aws-devicefarm-testspec-template.yml"));
    String testSpec = scanner.useDelimiter("\\A").next();
    scanner.close();
    StringWriter justTestLahProperties = new StringWriter();
    Properties props = new Properties();
    props.putAll(properties.getProperties());

    // encode the test users into the testSpec file
    String testUsersFile = props.getProperty("testusers.file");
    Properties testUsers = new Properties();
    try {
      if (testUsersFile != null && !testUsersFile.isEmpty()) {
        testUsers.load(new FileInputStream(testUsersFile));
      } else {
        LOG.info(
            "Loading JustTestLah properties from classpath ({})",
            UserService.TESTUSERS_DEFAULT_FILE);
        testUsers.load(
            UserService.class
                .getClassLoader()
                .getResourceAsStream(UserService.TESTUSERS_DEFAULT_FILE));
      }
    } catch (IOException exception) {
      LOG.warn("Error loading test user definitions from {}", testUsersFile);
    }
    StringWriter testUsersProperties = new StringWriter();
    testUsers.store(testUsersProperties, "test users");
    testSpec =
        testSpec.replaceAll(
            "__TESTUSERS_PROPERTIES_BASE64__",
            Base64.encodeAsString(testUsersProperties.toString()));

    // these settings will be overridden by the test spec execution
    props.remove("android.appPath");
    props.remove("ios.appPath");
    props.remove("cloudprovider");
    props.remove("testusers.file");
    props.store(justTestLahProperties, "justtestlah properties");

    // encode the `justtestlah.properties` into the testSpec file
    testSpec =
        testSpec.replaceAll(
            "__JUSTTESTLAH_PROPERTIES_BASE64__",
            Base64.encodeAsString(justTestLahProperties.toString()));

    LOG.info("Test spec file: \n{}", testSpec);
    String path = System.getProperty("java.io.tmpdir") + "aws-devicefarm-testspec.yml";
    Files.write(Paths.get(path), testSpec.getBytes());
    return path;
  }
}
