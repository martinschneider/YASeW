package io.cucumber.junit;

import cucumber.api.StepDefinitionReporter;
import cucumber.api.event.TestRunFinished;
import cucumber.api.event.TestRunStarted;
import cucumber.runner.EventBus;
import cucumber.runner.ThreadLocalRunnerSupplier;
import cucumber.runner.TimeService;
import cucumber.runner.TimeServiceEventBus;
import cucumber.runtime.BackendModuleBackendSupplier;
import cucumber.runtime.BackendSupplier;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.Env;
import cucumber.runtime.FeaturePathFeatureSupplier;
import cucumber.runtime.filter.Filters;
import cucumber.runtime.formatter.PluginFactory;
import cucumber.runtime.formatter.Plugins;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.FeatureLoader;
import io.cucumber.core.options.CucumberOptionsAnnotationParser;
import io.cucumber.core.options.EnvironmentOptionsParser;
import io.cucumber.core.options.RuntimeOptions;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import qa.justtestlah.configuration.Platform;
import qa.justtestlah.configuration.PropertiesHolder;

/** Custom JUnit runner to dynamically set cucumber.̰options. Based on {@link Cucumber}. */
public class JustTestLahRunner extends ParentRunner<FeatureRunner> {

  private static final String CLOUDPROVIDER_AWS = "aws";
  private static final String CLOUDPROVIDER_LOCAL = "local";
  public static final String AWS_JUNIT_GROUP_DESCRIPTION = "Test results";
  public static final String AWS_JUNIT_SUITE_DESCRIPTION = "AWS Devicefarm execution";

  private static final Logger LOG = LoggerFactory.getLogger(JustTestLahRunner.class);

  private final List<FeatureRunner> children = new ArrayList<>();
  private List<CucumberFeature> features = new ArrayList<>();
  private ThreadLocalRunnerSupplier runnerSupplier = null;
  private Plugins plugins = null;
  private EventBus bus = null;
  private PropertiesHolder properties = new PropertiesHolder();
  private boolean multiThreadingAssumed = false;

  private static final String STEPS_PACKAGE_KEY = "steps.package";
  private static final String CLOUD_PROVIDER = "cloudprovider";
  private static final String PLATFORM_KEY = "platform";
  private static final String TAGS_KEY = "tags";
  private static final String CUCUMBER_OPTIONS_KEY = "cucumber.options";
  private static final String FEATURES_DIRECTORY_KEY = "features.directory";
  private static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
  private static final String CUCUMBER_REPORT_DIRECTORY_KEY = "cucumber.report.directory";
  private static final String JUSTTESTLAH_SPRING_CONTEXT_KEY = "justtestlah.use.springcontext";
  private static final String DEFAULT_CUCUMBER_REPORT_DIRECTORY = "target/report/cucumber";
  private static final String DEFAULT_PLATFORM = "web";
  private static final String DELIMITER = ",";

  private Runner awsRunner;

  /**
   * Constructs a new {@link JustTestLahRunner}.
   *
   * @param clazz test class
   * @throws InitializationError {@link InitializationError}
   * @throws IOException {@link IOException}
   */
  public JustTestLahRunner(Class<?> clazz) throws InitializationError, IOException {
    super(clazz);

    // Initialize Spring profiles and settings
    init();

    // Bridge logging to SLF4J
    bridgeLogging();

    if (properties.getProperty(CLOUD_PROVIDER, CLOUDPROVIDER_LOCAL).equals(CLOUDPROVIDER_AWS)) {
      LOG.info("Using qa.justtestlah.awsdevicefarm.AWSTestRunner");
      awsRunner = getAWSRunner(clazz);
    } else {
      String cucumberOptions = buildCucumberOptions();
      LOG.info("Setting cucumber options ({}) to {}", CUCUMBER_OPTIONS_KEY, cucumberOptions);
      System.setProperty(CUCUMBER_OPTIONS_KEY, cucumberOptions);
      initCucumber(clazz);
    }
  }

  /**
   * This is the code taken from {@link Cucumber}
   *
   * @param clazz {@link Class}
   * @throws InitializationError {@link InitializationError}
   */
  private void initCucumber(Class<?> clazz) throws InitializationError {
    Assertions.assertNoCucumberAnnotatedMethods(clazz);

    ClassLoader classLoader = clazz.getClassLoader();
    ResourceLoader resourceLoader = new MultiLoader(classLoader);

    // Parse the options early to provide fast feedback about invalid options
    RuntimeOptions annotationOptions =
        new CucumberOptionsAnnotationParser(resourceLoader)
            .withOptionsProvider(new JUnitCucumberOptionsProvider())
            .parse(clazz)
            .build();

    RuntimeOptions runtimeOptions =
        new EnvironmentOptionsParser(resourceLoader).parse(Env.INSTANCE).build(annotationOptions);

    JUnitOptions junitAnnotationOptions = new JUnitOptionsParser().parse(clazz).build();

    JUnitOptions junitOptions =
        new JUnitOptionsParser()
            .parse(runtimeOptions.getJunitOptions())
            .setStrict(runtimeOptions.isStrict())
            .build(junitAnnotationOptions);

    ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);

    // Parse the features early. Don't proceed when there are lexer errors
    FeatureLoader featureLoader = new FeatureLoader(resourceLoader);
    FeaturePathFeatureSupplier featureSupplier =
        new FeaturePathFeatureSupplier(featureLoader, runtimeOptions);
    this.features = featureSupplier.get();

    // Create plugins after feature parsing to avoid the creation of empty files on lexer errors.
    this.plugins = new Plugins(classLoader, new PluginFactory(), runtimeOptions);
    this.bus = new TimeServiceEventBus(TimeService.SYSTEM);

    BackendSupplier backendSupplier =
        new BackendModuleBackendSupplier(resourceLoader, classFinder, runtimeOptions);
    this.runnerSupplier = new ThreadLocalRunnerSupplier(runtimeOptions, bus, backendSupplier);
    Filters filters = new Filters(runtimeOptions);
    for (CucumberFeature cucumberFeature : features) {
      FeatureRunner featureRunner =
          new FeatureRunner(cucumberFeature, filters, runnerSupplier, junitOptions);
      if (!featureRunner.isEmpty()) {
        children.add(featureRunner);
      }
    }
    LOG.debug(
        "Found {} feature(s) in {}: {}",
        features.size(),
        properties.getProperty(FEATURES_DIRECTORY_KEY),
        features);
  }

  @Override
  public List<FeatureRunner> getChildren() {
    return children;
  }

  @Override
  protected Description describeChild(FeatureRunner child) {
    return child.getDescription();
  }

  @Override
  protected void runChild(FeatureRunner child, RunNotifier notifier) {
    child.run(notifier);
  }

  @Override
  protected Statement childrenInvoker(RunNotifier notifier) {
    Statement runFeatures = super.childrenInvoker(notifier);
    return new RunCucumber(runFeatures);
  }

  class RunCucumber extends Statement {
    private final Statement runFeatures;

    RunCucumber(Statement runFeatures) {
      this.runFeatures = runFeatures;
    }

    @Override
    public void evaluate() throws Throwable {
      if (multiThreadingAssumed) {
        plugins.setSerialEventBusOnEventListenerPlugins(bus);
      } else {
        plugins.setEventBusOnEventListenerPlugins(bus);
      }

      bus.send(new TestRunStarted(bus.getTime(), bus.getTimeMillis()));
      for (CucumberFeature feature : features) {
        feature.sendTestSourceRead(bus);
      }
      StepDefinitionReporter stepDefinitionReporter = plugins.stepDefinitionReporter();
      runnerSupplier.get().reportStepDefinitions(stepDefinitionReporter);
      runFeatures.evaluate();
      bus.send(new TestRunFinished(bus.getTime(), bus.getTimeMillis()));
    }
  }

  @Override
  public void setScheduler(RunnerScheduler scheduler) {
    super.setScheduler(scheduler);
    multiThreadingAssumed = true;
  }

  String buildCucumberOptions() {
    StringBuilder cucumberOptions = new StringBuilder();
    cucumberOptions.append("--tags '@" + properties.getProperty(PLATFORM_KEY, DEFAULT_PLATFORM));
    String tags = properties.getProperty(TAGS_KEY, null);
    if (tags != null) {
      // Prevent injection attacks
      if (tags.contains("'")) {
        throw new RuntimeException(
            String.format("Invalid character ' in tag expression: %s", tags));
      }
      // support legacy format (i.e. comma-separated list of tags without @)
      if (!tags.contains("@")) {
        for (String tag : tags.split(DELIMITER)) {
          cucumberOptions.append(" and @");
          cucumberOptions.append(tag);
        }
        cucumberOptions.append("'");
      } else // no format (tag expressions)
      {
        cucumberOptions.append(" and (");
        cucumberOptions.append(tags);
        cucumberOptions.append(")'");
      }
    }
    if (Boolean.parseBoolean(
        properties.getProperty(JUSTTESTLAH_SPRING_CONTEXT_KEY, Boolean.toString(true)))) {
      cucumberOptions.append(" --glue qa.justtestlah.steps ");
    }
    cucumberOptions.append(" --glue ");
    cucumberOptions.append(properties.getProperty(STEPS_PACKAGE_KEY));
    cucumberOptions.append(" --plugin pretty --plugin html:report --plugin json:");
    cucumberOptions.append(
        properties.getProperty(CUCUMBER_REPORT_DIRECTORY_KEY, DEFAULT_CUCUMBER_REPORT_DIRECTORY));
    cucumberOptions.append("/cucumber.json ");
    cucumberOptions.append(properties.getProperty(FEATURES_DIRECTORY_KEY));
    cucumberOptions.append(" --strict");
    return cucumberOptions.toString();
  }

  private void bridgeLogging() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private void init() {
    // set the active Spring profile to the current platform
    String platform = properties.getProperty(PLATFORM_KEY);
    if (platform == null || platform.isEmpty()) {
      LOG.info("No platform specified. Using default ({})", Platform.DEFAULT);
      platform = Platform.DEFAULT.getPlatformName();
      System.setProperty(PLATFORM_KEY, platform);
    }
    String springProfiles = System.getProperty(SPRING_PROFILES_ACTIVE);
    if (springProfiles != null && !springProfiles.isEmpty()) {
      springProfiles += "," + platform;
    } else {
      springProfiles = platform;
    }
    LOG.info("Setting platform to {}", platform);
    System.setProperty(SPRING_PROFILES_ACTIVE, springProfiles);
  }

  @Override
  public Description getDescription() {
    if (properties.getProperty(CLOUD_PROVIDER, CLOUDPROVIDER_LOCAL).equals(CLOUDPROVIDER_AWS)) {
      Description suiteDescription =
          Description.createSuiteDescription(AWS_JUNIT_SUITE_DESCRIPTION);
      suiteDescription.addChild(
          Description.createTestDescription("groupName", AWS_JUNIT_GROUP_DESCRIPTION));
      return suiteDescription;
    } else {
      return super.getDescription();
    }
  }

  /** this method uses reflection to avoid a compile-time dependency on justtestlah-awsdevicefarm */
  private Runner getAWSRunner(Class<?> clazz) {
    try {
      return (Runner)
          Class.forName("qa.justtestlah.awsdevicefarm.AWSTestRunner")
              .getConstructor(Class.class)
              .newInstance(clazz);
    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException exception) {
      LOG.error(
          "Unable to create an instance of qa.justtestlah.awsdevicefarm.AWSTestRunner. Ensure justtestlah-aws is on your classpath (check your Maven pom.xml).",
          exception);
    }
    return null;
  }

  @Override
  public void run(RunNotifier notifier) {
    if (properties.getProperty(CLOUD_PROVIDER, CLOUDPROVIDER_LOCAL).equals(CLOUDPROVIDER_AWS)) {
      awsRunner.run(notifier);
    } else {
      super.run(notifier);
    }
  }
}
