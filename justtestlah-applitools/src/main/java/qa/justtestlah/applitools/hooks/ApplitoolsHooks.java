package qa.justtestlah.applitools.hooks;

import com.applitools.eyes.selenium.Eyes;
import com.codeborne.selenide.WebDriverRunner;
import io.cucumber.java.Scenario;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import qa.justtestlah.configuration.JustTestLahConfiguration;
import qa.justtestlah.hooks.AbstractCucumberHook;
import qa.justtestlah.hooks.HooksRegister;

/** Applitools hooks. */
@Component
public class ApplitoolsHooks extends AbstractCucumberHook {

  private static final Logger LOG = LoggerFactory.getLogger(ApplitoolsHooks.class);

  @Autowired private JustTestLahConfiguration configuration;

  @Autowired private Eyes eyes;

  @Autowired private HooksRegister hooksRegister;

  @PostConstruct
  void register() {
    hooksRegister.addHooks(this);
  }

  /**
   * Initialise Applitools.
   *
   * @param scenario Cucumber scenario
   */
  @Override
  public void before(Scenario scenario) {
    if (configuration.isEyesEnabled()) {
      LOG.info("Initializing Eyes");
      eyes.open(
          WebDriverRunner.getAndCheckWebDriver(),
          configuration.getApplicationName(),
          configuration.getPlatform().name());
    }
  }

  /**
   * Close the web driver and Applitools.
   *
   * @param scenario Cucumber scenario
   */
  @Override
  public void after(Scenario scenario) {
    if (configuration.isEyesEnabled() && eyes.getIsOpen()) {
      LOG.info("Closing Eyes");
      eyes.close();
    }
  }
}
